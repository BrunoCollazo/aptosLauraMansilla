package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.dto.request.AdminApartmentUpdateRequest;
import com.bcollazo.lauraapartments.dto.response.ApartmentDTO;
import com.bcollazo.lauraapartments.service.AdminApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // Guarda todo el apartamento de una (disponibilidad, precio y descuentos) cuando el admin
    // aprieta Guardar. Reemplaza los PUT sueltos de /availability y /price.
    @PutMapping("/{id}")
    public ResponseEntity<ApartmentDTO> updateApartment(
            @PathVariable Long id,
            @RequestBody AdminApartmentUpdateRequest request) {
        return ResponseEntity.ok(adminApartmentService.updateApartment(id, request));
    }
}