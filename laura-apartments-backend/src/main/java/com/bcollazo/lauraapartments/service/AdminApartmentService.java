package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.ApartmentDTO;
import com.bcollazo.lauraapartments.model.Apartment;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminApartmentService {

    private final ApartmentRepository apartmentRepository;

    public List<ApartmentDTO> getAllApartments() {
        return apartmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateAvailability(Long id, boolean available) {
        Apartment apartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        apartment.setAvailable(available);
        apartmentRepository.save(apartment);
    }

    @Transactional
    public void updatePrice(Long id, BigDecimal price) {
        Apartment apartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));
        apartment.setPricePerNight(price);
        apartmentRepository.save(apartment);
    }

    private ApartmentDTO convertToDTO(Apartment apartment) {
        return ApartmentDTO.builder()
                .id(apartment.getId())
                .name(apartment.getName())
                .available(apartment.isAvailable())
                .pricePerNight(apartment.getPricePerNight())
                .build();
    }
}
