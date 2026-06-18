package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.dto.request.HomologationPaymentRequestDTO;
import com.bcollazo.lauraapartments.dto.response.PaymentResponseDTO;
import com.bcollazo.lauraapartments.entity.PaymentStatus;
import com.bcollazo.lauraapartments.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Endpoints de pago para administración/pruebas: correr la homologación de Fiserv
// (pagos con monto/moneda/impuesto a mano) y anular pagos. No los usa la web pública.
@Slf4j
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/homologation")
    public ResponseEntity<PaymentResponseDTO> createHomologationPayment(
            @Valid @RequestBody HomologationPaymentRequestDTO requestDTO) {
        return ResponseEntity.ok(paymentService.createHomologationPayment(requestDTO));
    }

    @PostMapping("/{reference}/void")
    public ResponseEntity<PaymentStatus> voidPayment(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.voidPayment(reference));
    }
}
