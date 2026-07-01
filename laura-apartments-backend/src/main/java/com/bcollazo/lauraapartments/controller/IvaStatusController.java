package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.dto.response.IvaStatusDTO;
import com.bcollazo.lauraapartments.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Estado del IVA por temporada (para el cartel informativo del panel de admin).
@RestController
@RequestMapping("/api/iva-status")
@RequiredArgsConstructor
public class IvaStatusController {

    private final PricingService pricingService;

    @GetMapping
    public ResponseEntity<IvaStatusDTO> getIvaStatus() {
        return ResponseEntity.ok(pricingService.getIvaStatus(LocalDate.now()));
    }
}
