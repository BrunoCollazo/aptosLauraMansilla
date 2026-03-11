package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.dto.ApartmentDTO;
import com.bcollazo.lauraapartments.service.AdminApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/apartments")
@RequiredArgsConstructor
public class AdminApartmentController {

    private final AdminApartmentService adminApartmentService;

    @GetMapping
    public ResponseEntity<List<ApartmentDTO>> getAllApartments() {
        return ResponseEntity.ok(adminApartmentService.getAllApartments());
    }

    @PutMapping("/{id}/availability")
    public ResponseEntity<Void> updateAvailability(
            @PathVariable Long id, 
            @RequestParam boolean available) {
        adminApartmentService.updateAvailability(id, available);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/price")
    public ResponseEntity<Void> updatePrice(
            @PathVariable Long id, 
            @RequestParam BigDecimal price) {
        adminApartmentService.updatePrice(id, price);
        return ResponseEntity.ok().build();
    }
}
