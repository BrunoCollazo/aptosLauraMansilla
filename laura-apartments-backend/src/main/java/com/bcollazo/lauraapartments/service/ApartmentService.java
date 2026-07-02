package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.response.PublicApartmentDTO;
import com.bcollazo.lauraapartments.dto.response.QuoteResponseDTO;
import com.bcollazo.lauraapartments.entity.Apartment;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final PricingService pricingService;

    public List<PublicApartmentDTO> getAllApartments() {
        return apartmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    // Cotiza una estadía para que el front muestre el total real (con descuento e IVA por temporada).
    public QuoteResponseDTO getQuote(Long apartmentId, LocalDate checkIn, int nights) {
        if (nights < 1) {
            throw new IllegalArgumentException("nights must be at least 1");
        }
        if (checkIn == null) {
            throw new IllegalArgumentException("checkIn is required");
        }
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        return pricingService.calculateQuote(apartment, checkIn, nights);
    }

    private PublicApartmentDTO mapToDTO(Apartment apartment) {
        return PublicApartmentDTO.builder()
                .id(apartment.getId())
                .name(apartment.getName())
                .pricePerNight(apartment.getPricePerNight())
                .available(apartment.isAvailable())
                .build();
    }
}