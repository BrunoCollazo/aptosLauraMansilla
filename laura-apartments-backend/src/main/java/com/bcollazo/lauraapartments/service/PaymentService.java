package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.config.FiservConfig;
import com.bcollazo.lauraapartments.dto.request.FiservRequestDTO;
import com.bcollazo.lauraapartments.dto.request.PaymentRequestDTO;
import com.bcollazo.lauraapartments.dto.response.FiservPaymentResultDTO;
import com.bcollazo.lauraapartments.dto.response.PaymentResponseDTO;
import com.bcollazo.lauraapartments.integration.FiservClient;
import com.bcollazo.lauraapartments.entity.Apartment;
import com.bcollazo.lauraapartments.entity.Payment;
import com.bcollazo.lauraapartments.entity.PaymentStatus;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import com.bcollazo.lauraapartments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ApartmentRepository apartmentRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final FiservClient fiservClient;
    private final FiservConfig fiservConfig;
    private final FiservSignatureService signatureService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO request) {

        Apartment apartment = apartmentRepository
                .findById(request.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        if (!apartment.isAvailable()) {
            throw new RuntimeException("Apartment is not available for rental");
        }

        BigDecimal totalAmount = pricingService.calculateTotalAmount(
                apartment.getPricePerNight(),
                request.getNights()
        );

        long caratAmount = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
        String reference = UUID.randomUUID().toString();
        String today = java.time.LocalDate.now().toString();
        String dueDate = java.time.LocalDate.now().plusDays(request.getNights()).toString();

        String invoiceNumber = String.valueOf(System.currentTimeMillis() % 1000000000L);
        FiservRequestDTO fiservRequest = FiservRequestDTO.builder()
                .currency("858") // UYU
                .amount(caratAmount)
                .taxedAmount(0L)
                .taxAmount(0L)
                .indi(0) // No devuelve impuesto
                .reference(reference)
                .invoice(FiservRequestDTO.Invoice.builder()
                        .number(invoiceNumber)
                        .totalAmount(caratAmount)
                        .currency("858")
                        .date(today)
                        .dueDate(dueDate)
                        .description("Apartment Rental: " + apartment.getName())
                        .finalConsumer(true)
                        .serial("A")
                        .address(FiservRequestDTO.Address.builder()
                                .country("UY")
                                .city("Montevideo")
                                .street("General Artigas")
                                .doorNumber("1234")
                                .build())
                        .build())
                .client(FiservRequestDTO.Client.builder()
                        .clientIdType("EMAIL")
                        .clientId(request.getClientEmail())
                        .email(request.getClientEmail())
                        .build())
                .config(FiservRequestDTO.Config.builder()
                        .callbackUrl(fiservConfig.getCallbackUrlCode())
                        .useRedirect(true)
                        .build())
                .build();

        String token = fiservClient.initiatePayment(fiservRequest);

        Payment payment = Payment.builder()
                .apartment(apartment)
                .nights(request.getNights())
                .totalAmount(totalAmount)
                .clientEmail(request.getClientEmail())
                .status(PaymentStatus.PENDING)
                .fiservToken(token)
                .reference(reference)
                .build();

        paymentRepository.save(payment);

        return PaymentResponseDTO.builder()
                .pageUrl(fiservConfig.getPageUrl())
                .token(token)
                .reference(reference)
                .build();
    }

    @Transactional
    public PaymentStatus processCallbackResult(String dataJson) throws com.fasterxml.jackson.core.JsonProcessingException {
        // Verify the signature against the raw JSON map, not a DTO round-trip: our DTOs
        // don't model every field Fiserv may send, so verifying off the typed object would
        // silently drop fields and break the signature check.
        Map<String, Object> resultMap = objectMapper.readValue(dataJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        FiservPaymentResultDTO result = objectMapper.convertValue(resultMap, FiservPaymentResultDTO.class);

        String reference = result.getReference();
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found for reference: " + reference));

        if (result.getResponseHeader() == null || result.getResponseHeader().getDigitalSign() == null) {
            log.error("Missing responseHeader or digitalSign in callback for reference: {}", reference);
            throw new RuntimeException("Missing callback signature");
        }

        if (!signatureService.verifySign(resultMap, result.getResponseHeader().getDigitalSign())) {
            log.error("Invalid signature in callback for reference: {}", reference);
            throw new RuntimeException("Invalid callback signature");
        }

        updatePaymentFromDetails(payment, result.getPayment());

        paymentRepository.save(payment);
        return payment.getStatus();
    }

    @Transactional
    public PaymentStatus syncPaymentStatus(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found for reference: " + reference));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment.getStatus();
        }

        fiservClient.queryPayment(payment.getFiservToken()).ifPresent(details -> {
            updatePaymentFromDetails(payment, details);
            paymentRepository.save(payment);
            log.info("Synced payment status for reference {}: {}", reference, payment.getStatus());
        });

        return payment.getStatus();
    }

    private static final java.time.format.DateTimeFormatter FISERV_DATE_TIME_FORMATTER =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Copies the Fiserv-reported payment details (card, authorization, confirmed amount/currency,
     * confirmation timestamp) onto our record. Shared by the callback, sync and webhook paths so
     * none of them silently drop data Fiserv actually sent.
     */
    public void applyPaymentDetails(Payment payment, FiservPaymentResultDTO.PaymentDetails details) {
        if (details == null) {
            return;
        }
        if (details.getPaymentToken() != null && payment.getPaymentToken() == null) {
            payment.setPaymentToken(details.getPaymentToken());
        }
        if (details.getCard() != null) {
            Object cardBrandName = details.getCard().get("cardBrandName");
            Object cardMask = details.getCard().get("cardMask");
            if (cardBrandName != null) payment.setCardBrand(cardBrandName.toString());
            if (cardMask != null) payment.setCardMask(cardMask.toString());
        }
        if (details.getAuthorizer() != null) {
            Object authorizationCode = details.getAuthorizer().get("authorizationCode");
            if (authorizationCode != null) payment.setAuthorizationCode(authorizationCode.toString());
        }
        if (details.getAmount() > 0) {
            payment.setConfirmedAmount(details.getAmount());
        }
        if (details.getCurrency() != null) {
            payment.setCurrency(details.getCurrency());
        }
        if (details.getDateTime() != null) {
            try {
                payment.setConfirmedAt(LocalDateTime.parse(details.getDateTime(), FISERV_DATE_TIME_FORMATTER));
            } catch (Exception e) {
                log.warn("Could not parse Fiserv payment dateTime '{}': {}", details.getDateTime(), e.getMessage());
            }
        }
    }

    private void updatePaymentFromDetails(Payment payment, FiservPaymentResultDTO.PaymentDetails details) {
        if (details != null) {
            applyPaymentDetails(payment, details);

            if (details.isAuthorized() && "PROCESSED".equals(details.getState())) {
                payment.setStatus(PaymentStatus.PROCESSED);
            } else if ("CANCELED".equals(details.getState())) {
                payment.setStatus(PaymentStatus.FAILED);
            } else if ("INPROCESS".equals(details.getState())) {
                // Expected transient state (per Fiserv docs): payment not yet confirmed by the
                // client, or still being processed by a collection network. Stays PENDING.
                log.debug("Payment still INPROCESS for reference {}", payment.getReference());
            } else {
                log.warn("Unhandled payment state '{}' for reference {}", details.getState(), payment.getReference());
            }
        }
    }
}