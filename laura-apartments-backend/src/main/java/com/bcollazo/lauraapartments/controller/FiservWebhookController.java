package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.dto.FiservWebhookDTO;
import com.bcollazo.lauraapartments.model.Payment;
import com.bcollazo.lauraapartments.model.PaymentStatus;
import com.bcollazo.lauraapartments.repository.PaymentRepository;
import com.bcollazo.lauraapartments.service.FiservSignatureService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fiserv/webhook")
@RequiredArgsConstructor
public class FiservWebhookController {

    private final PaymentRepository paymentRepository;
    private final FiservSignatureService signatureService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody FiservWebhookDTO payload) {
        log.info("Received Fiserv webhook event: {}", payload.getEvent());

        // 1. Validate digitalSign
        Map<String, Object> payloadMap = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {});
        if (!signatureService.verifySign(payloadMap, payload.getRequestHeader().getDigitalSign())) {
            log.warn("Invalid signature in webhook for event: {}", payload.getEvent());
            return ResponseEntity.ok().build(); // Fiserv expects 200 even if invalid
        }

        // 2. Identify payment
        String reference = null;
        String paymentToken = null;
        String accessToken = null;
        
        if (payload.getPayment() != null) {
            paymentToken = payload.getPayment().getPaymentToken();
            accessToken = payload.getPayment().getAccessToken();
            if (payload.getPayment().getInvoice() != null) {
                reference = payload.getPayment().getInvoice().getNumber();
                // If invoice number is like "INV-ref", we might need to strip prefix or search by exact match
                // Looking at PaymentService, it sets invoice.number = "INV-" + reference.substring(0, 8)
                // This is problematic for direct lookup. Let's rely on reference field if available elsewhere.
            }
        }

        // 3. Find Payment
        Payment payment = null;
        if (paymentToken != null) {
            payment = paymentRepository.findByPaymentToken(paymentToken).orElse(null);
        }
        if (payment == null && accessToken != null) {
            payment = paymentRepository.findByFiservToken(accessToken).orElse(null);
        }
        
        if (payment == null) {
            log.warn("Payment not found for webhook: paymentToken={}, accessToken={}", paymentToken, accessToken);
            return ResponseEntity.ok().build();
        }

        // Update paymentToken if it was missing in our DB
        if (payment.getPaymentToken() == null && paymentToken != null) {
            payment.setPaymentToken(paymentToken);
        }

        // 4. Handle idempotency
        if (payment.getStatus() == PaymentStatus.PROCESSED) {
            log.info("Payment {} already PROCESSED, skipping update", payment.getId());
            return ResponseEntity.ok().build();
        }

        // 5. Check event
        String event = payload.getEvent();
        if ("PAYMENT".equals(event)) {
            if (payload.getPayment().isAuthorized() && "PROCESSED".equals(payload.getPayment().getState())) {
                payment.setStatus(PaymentStatus.PROCESSED);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
        } else if ("PAYMENT_VOID".equals(event)) {
            payment.setStatus(PaymentStatus.VOIDED);
        } else {
            log.info("Unhandled webhook event: {}", event);
            return ResponseEntity.ok().build();
        }

        paymentRepository.save(payment);
        log.info("Updated payment {} status to {} via webhook", payment.getId(), payment.getStatus());

        return ResponseEntity.ok().build();
    }
}
