package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.dto.response.ApartmentResponseDTO;
import com.bcollazo.lauraapartments.dto.response.QuoteResponseDTO;
import com.bcollazo.lauraapartments.service.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/apartments")
@RequiredArgsConstructor
public class ApartmentController {

    private final ApartmentService apartmentService;

    @GetMapping
    public ResponseEntity<List<ApartmentResponseDTO>> getAllApartments() {
        return ResponseEntity.ok(apartmentService.getAllApartments());
    }

    @GetMapping("/{id}/quote")
    public ResponseEntity<QuoteResponseDTO> getQuote(@PathVariable Long id,
                                                     @RequestParam int nights) {
        return ResponseEntity.ok(apartmentService.getQuote(id, nights));
    }
}