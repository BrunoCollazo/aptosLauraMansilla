package com.bcollazo.lauraapartments.integration;

import com.bcollazo.lauraapartments.config.FiservConfig;
import com.bcollazo.lauraapartments.dto.response.FiservPaymentResultDTO;
import com.bcollazo.lauraapartments.dto.request.FiservRequestDTO;
import com.bcollazo.lauraapartments.dto.response.FiservResponseDTO;
import com.bcollazo.lauraapartments.service.FiservSignatureService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
                .version(config.getVersionOrDefault())
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
            Map<String, Object> responseMap = postForResponseMap(config.getBaseUrl() + "/payment", request);
            FiservResponseDTO response = objectMapper.convertValue(responseMap, FiservResponseDTO.class);
            if (response == null || response.getResponseHeader() == null) {
                throw new RuntimeException("Empty response from Fiserv");
            }

            // Verify response signature against the raw response map, not a DTO round-trip:
            // our DTOs don't model every field Fiserv may send, so signing/verifying off the
            // typed object silently drops fields and breaks the signature check.
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
        try {
            // Fiserv's own docs state all operations are POST; paymentQuery's "(GET)" label
            // in the spec doesn't work in practice — a GET with a body gets served a WAF
            // bot-challenge page instead of reaching the API.
            FiservResponseDTO response = postSignedTokenOp("/paymentQuery", accessToken);
            if (response != null && response.getResponseHeader() != null) {
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

    public boolean voidPayment(String accessToken) {
        try {
            FiservResponseDTO response = postSignedTokenOp("/voidPayment", accessToken);
            if (response == null || response.getResponseHeader() == null) {
                log.error("Empty response from Fiserv /voidPayment");
                return false;
            }
            if ("00".equals(response.getResponseHeader().getResponseCode())) {
                log.info("Fiserv void successful for token: {}", accessToken);
                return true;
            }
            log.error("Fiserv voidPayment failed: {} - {}",
                    response.getResponseHeader().getResponseCode(),
                    response.getResponseHeader().getResponseDescription());
            return false;
        } catch (Exception e) {
            log.error("Error calling Fiserv /voidPayment", e);
            return false;
        }
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void echoTest() {
        if (!signatureService.isReady()) {
            log.debug("Skipping echoTest: signature keys are not loaded");
            return;
        }

        Map<String, Object> header = newRequestHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("requestHeader", header);
        header.put("digitalSign", signatureService.generateSign(body));

        try {
            Map<String, Object> responseMap = postForResponseMap(config.getBaseUrl() + "/echoTest", jsonEntity(body));
            FiservResponseDTO response = objectMapper.convertValue(responseMap, FiservResponseDTO.class);
            if (response != null && response.getResponseHeader() != null) {
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

    // Header común de todo request a Fiserv (netId, versión con default, auditNumber, dateTime).
    private Map<String, Object> newRequestHeader() {
        Map<String, Object> header = new HashMap<>();
        header.put("netId", config.getNetId());
        header.put("version", config.getVersionOrDefault());
        header.put("auditNumber", generateAuditNumber());
        header.put("dateTime", ZonedDateTime.now(URUGUAY_ZONE).format(DATE_TIME_FORMATTER));
        return header;
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    // Operaciones sobre un pago ya existente (query, void): mismo header + {requestHeader,
    // accessToken} + firma + POST + verificación de la firma de la respuesta. Devuelve la
    // respuesta ya verificada (o null si vino vacía); cada caller interpreta código/datos.
    private FiservResponseDTO postSignedTokenOp(String path, String accessToken) throws Exception {
        Map<String, Object> header = newRequestHeader();
        Map<String, Object> body = new HashMap<>();
        body.put("requestHeader", header);
        body.put("accessToken", accessToken);
        header.put("digitalSign", signatureService.generateSign(body));

        Map<String, Object> responseMap = postForResponseMap(config.getBaseUrl() + path, jsonEntity(body));
        FiservResponseDTO response = objectMapper.convertValue(responseMap, FiservResponseDTO.class);
        if (response != null && response.getResponseHeader() != null
                && !signatureService.verifySign(responseMap, response.getResponseHeader().getDigitalSign())) {
            log.warn("Invalid signature in Fiserv {} response", path);
        }
        return response;
    }

    private String generateAuditNumber() {
        long count = auditCounter.incrementAndGet() % 10000000000000000L;
        return String.format("%016d", count);
    }

    private Map<String, Object> postForResponseMap(String url, HttpEntity<?> request) throws Exception {
        ResponseEntity<String> rawResponse = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        return objectMapper.readValue(rawResponse.getBody(), new TypeReference<Map<String, Object>>() {});
    }
}
