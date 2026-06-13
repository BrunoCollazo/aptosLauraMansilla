package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.response.ApartmentResponseDTO;
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

    public List<ApartmentResponseDTO> getAllApartments() {
        return apartmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
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