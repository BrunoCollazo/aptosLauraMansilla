package com.bcollazo.lauraapartments.integration;

import com.bcollazo.lauraapartments.config.FiservConfig;
import com.bcollazo.lauraapartments.dto.FiservPaymentResultDTO;
import com.bcollazo.lauraapartments.dto.FiservRequestDTO;
import com.bcollazo.lauraapartments.dto.FiservResponseDTO;
import com.bcollazo.lauraapartments.service.FiservSignatureService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiservClient {

    private final FiservConfig config;
    private final RestTemplate restTemplate;
    private final FiservSignatureService signatureService;
    private final ObjectMapper objectMapper;
    private final AtomicLong auditCounter = new AtomicLong(System.currentTimeMillis() % 1000000);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId URUGUAY_ZONE = ZoneId.of("America/Montevideo");

    public String initiatePayment(FiservRequestDTO requestDTO) {
        String auditNumber = generateAuditNumber();
        String dateTime = ZonedDateTime.now(URUGUAY_ZONE).format(DATE_TIME_FORMATTER);

        requestDTO.setRequestHeader(FiservRequestDTO.RequestHeader.builder()
                .netId(config.getNetId())
                .version(config.getVersion() != null ? config.getVersion() : "3.02")
                .auditNumber(auditNumber)
                .dateTime(dateTime)
                .build());

        Map<String, Object> payload = objectMapper.convertValue(requestDTO, new TypeReference<Map<String, Object>>() {});
        String signature = signatureService.generateSign(payload);
        requestDTO.getRequestHeader().setDigitalSign(signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<FiservRequestDTO> request = new HttpEntity<>(requestDTO, headers);

        try {
            FiservResponseDTO response = restTemplate.postForObject(config.getBaseUrl() + "/payment", request, FiservResponseDTO.class);
            if (response == null || response.getResponseHeader() == null) {
                throw new RuntimeException("Empty response from Fiserv");
            }

            // Verify response signature
            Map<String, Object> responseMap = objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {});
            if (!signatureService.verifySign(responseMap, response.getResponseHeader().getDigitalSign())) {
                throw new RuntimeException("Invalid response signature from Fiserv");
            }

            if ("00".equals(response.getResponseHeader().getResponseCode())) {
                return response.getToken();
            } else {
                log.error("Fiserv payment initiation failed: {} - {}", 
                        response.getResponseHeader().getResponseCode(), 
                        response.getResponseHeader().getResponseDescription());
                throw new RuntimeException("Fiserv initiation failed: " + response.getResponseHeader().getResponseDescription());
            }
        } catch (Exception e) {
            log.error("Error calling Fiserv /payment", e);
            throw new RuntimeException("Failed to initiate payment with Fiserv", e);
        }
    }

    public Optional<FiservPaymentResultDTO.PaymentDetails> queryPayment(String accessToken) {
        String auditNumber = generateAuditNumber();
        String dateTime = ZonedDateTime.now(URUGUAY_ZONE).format(DATE_TIME_FORMATTER);

        Map<String, Object> header = new HashMap<>();
        header.put("netId", config.getNetId());
        header.put("version", config.getVersion() != null ? config.getVersion() : "3.02");
        header.put("auditNumber", auditNumber);
        header.put("dateTime", dateTime);

        Map<String, Object> bodyForSign = new HashMap<>();
        bodyForSign.put("requestHeader", header);
        bodyForSign.put("accessToken", accessToken);

        String signature = signatureService.generateSign(bodyForSign);
        
        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl() + "/paymentQuery")
                .queryParam("requestHeader.netId", config.getNetId())
                .queryParam("requestHeader.version", config.getVersion() != null ? config.getVersion() : "3.02")
                .queryParam("requestHeader.auditNumber", auditNumber)
                .queryParam("requestHeader.dateTime", dateTime)
                .queryParam("requestHeader.digitalSign", signature)
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        try {
            ResponseEntity<FiservResponseDTO> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, FiservResponseDTO.class);
            FiservResponseDTO response = responseEntity.getBody();
            
            if (response != null && response.getResponseHeader() != null) {
                // Verify response signature
                Map<String, Object> responseMap = objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {});
                if (!signatureService.verifySign(responseMap, response.getResponseHeader().getDigitalSign())) {
                    log.warn("Invalid signature in Fiserv paymentQuery response");
                }

                if (!"00".equals(response.getResponseHeader().getResponseCode())) {
                    log.warn("Fiserv paymentQuery returned non-zero code: {} - {}", 
                            response.getResponseHeader().getResponseCode(),
                            response.getResponseHeader().getResponseDescription());
                } else if (response.getPayments() != null && !response.getPayments().isEmpty()) {
                    return Optional.of(response.getPayments().get(0));
                }
            }
        } catch (Exception e) {
            log.error("Error calling Fiserv /paymentQuery", e);
        }
        return Optional.empty();
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void echoTest() {
        Map<String, Object> requestBody = new HashMap<>();
        String dateTime = ZonedDateTime.now(URUGUAY_ZONE).format(DATE_TIME_FORMATTER);
        String auditNumber = generateAuditNumber();

        Map<String, Object> header = new HashMap<>();
        header.put("netId", config.getNetId());
        header.put("version", config.getVersion() != null ? config.getVersion() : "3.02");
        header.put("auditNumber", auditNumber);
        header.put("dateTime", dateTime);

        requestBody.put("requestHeader", header);
        String signature = signatureService.generateSign(requestBody);
        header.put("digitalSign", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            FiservResponseDTO response = restTemplate.postForObject(config.getBaseUrl() + "/echoTest", request, FiservResponseDTO.class);
            if (response != null && response.getResponseHeader() != null) {
                // Verify response signature
                Map<String, Object> responseMap = objectMapper.convertValue(response, new TypeReference<Map<String, Object>>() {});
                if (!signatureService.verifySign(responseMap, response.getResponseHeader().getDigitalSign())) {
                    log.warn("Invalid signature in Fiserv echoTest response");
                }

                if (!"00".equals(response.getResponseHeader().getResponseCode())) {
                    log.warn("Fiserv echoTest returned non-zero code: {}", response.getResponseHeader().getResponseCode());
                }
            }
        } catch (Exception e) {
            log.error("Error during Fiserv echoTest", e);
        }
    }

    private String generateAuditNumber() {
        long count = auditCounter.incrementAndGet() % 10000000000000000L;
        return String.format("%016d", count);
    }
}
