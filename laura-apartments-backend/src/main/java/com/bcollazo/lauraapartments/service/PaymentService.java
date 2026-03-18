package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.config.FiservConfig;
import com.bcollazo.lauraapartments.dto.*;
import com.bcollazo.lauraapartments.integration.FiservClient;
import com.bcollazo.lauraapartments.model.Apartment;
import com.bcollazo.lauraapartments.model.Payment;
import com.bcollazo.lauraapartments.model.PaymentStatus;
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

        FiservRequestDTO fiservRequest = FiservRequestDTO.builder()
                .currency("858") // UYU
                .amount(caratAmount)
                .taxedAmount(caratAmount)
                .taxAmount(0L)
                .indi(6) // IVA Ley 19.210
                .reference(reference)
                .invoice(FiservRequestDTO.Invoice.builder()
                        .number("INV" + reference.substring(0, 6))
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
    public PaymentStatus processCallbackResult(FiservPaymentResultDTO result) {
        String reference = result.getReference();
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found for reference: " + reference));

        // Verify digitalSign
        Map<String, Object> resultMap = objectMapper.convertValue(result, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        if (!signatureService.verifySign(resultMap, result.getResponseHeader().getDigitalSign())) {
            log.error("Invalid signature in callback for reference: {}", reference);
            throw new RuntimeException("Invalid callback signature");
        }

        updatePaymentFromDetails(payment, result.getPayment());

        paymentRepository.save(payment);
        return payment.getStatus();
    }

    @Transactional
    public void syncPaymentStatus(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Payment not found for reference: " + reference));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        fiservClient.queryPayment(payment.getFiservToken()).ifPresent(details -> {
            updatePaymentFromDetails(payment, details);
            paymentRepository.save(payment);
            log.info("Synced payment status for reference {}: {}", reference, payment.getStatus());
        });
    }

    private void updatePaymentFromDetails(Payment payment, FiservPaymentResultDTO.PaymentDetails details) {
        if (details != null) {
            // Store paymentToken if received
            if (details.getPaymentToken() != null && payment.getPaymentToken() == null) {
                payment.setPaymentToken(details.getPaymentToken());
            }

            if (details.isAuthorized() && "PROCESSED".equals(details.getState())) {
                payment.setStatus(PaymentStatus.PROCESSED);
            } else if ("CANCELED".equals(details.getState())) {
                payment.setStatus(PaymentStatus.FAILED);
            }
        }
    }
}