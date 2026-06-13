package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.dto.response.FiservPaymentResultDTO;
import com.bcollazo.lauraapartments.dto.request.PaymentRequestDTO;
import com.bcollazo.lauraapartments.dto.response.PaymentResponseDTO;
import com.bcollazo.lauraapartments.entity.PaymentStatus;
import com.bcollazo.lauraapartments.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> createPayment(@Valid @RequestBody PaymentRequestDTO requestDTO) {
        PaymentResponseDTO response = paymentService.createPayment(requestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> paymentCallback(@RequestParam("data") String dataJson) {
        try {
            log.info("Received Fiserv callback: {}", dataJson);
            FiservPaymentResultDTO result = objectMapper.readValue(dataJson, FiservPaymentResultDTO.class);
            PaymentStatus status = paymentService.processCallbackResult(result);

            String redirectUrl = (status == PaymentStatus.PROCESSED)
                    ? "https://www.lauramansilla.com/payment-success"
                    : "https://www.lauramansilla.com/payment-failure";

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(redirectUrl))
                    .build();
        } catch (Exception e) {
            log.error("Error processing Fiserv callback", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("https://www.lauramansilla.com/payment-failure"))
                    .build();
        }
    }

    @GetMapping("/{reference}/sync")
    public ResponseEntity<PaymentStatus> syncPaymentStatus(@PathVariable String reference) {
        PaymentStatus status = paymentService.syncPaymentStatus(reference);
        return ResponseEntity.ok(status);
    }
}