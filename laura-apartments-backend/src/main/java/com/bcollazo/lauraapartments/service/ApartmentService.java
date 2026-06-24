package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.response.ApartmentResponseDTO;
import com.bcollazo.lauraapartments.dto.response.QuoteResponseDTO;
import com.bcollazo.lauraapartments.entity.Apartment;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final PricingService pricingService;

    public List<ApartmentResponseDTO> getAllApartments() {
        return apartmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Cotiza una estadía para que el front muestre el total real (con descuento e IVA).
    public QuoteResponseDTO getQuote(Long apartmentId, int nights) {
        if (nights < 1) {
            throw new IllegalArgumentException("nights must be at least 1");
        }
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        QuoteResponseDTO quote = pricingService.calculateQuote(apartment.getPricePerNight(), nights);
        quote.setApartmentId(apartment.getId());
        return quote;
    }

    private ApartmentResponseDTO mapToDTO(Apartment apartment) {
        return ApartmentResponseDTO.builder()
                .id(apartment.getId())
                .name(apartment.getName())
                .pricePerNight(apartment.getPricePerNight())
                .available(apartment.isAvailable())
                .build();
    }
}