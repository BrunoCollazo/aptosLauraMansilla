package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.request.AdminApartmentUpdateRequest;
import com.bcollazo.lauraapartments.dto.response.AdminApartmentDTO;
import com.bcollazo.lauraapartments.entity.Apartment;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminApartmentService {

    private final ApartmentRepository apartmentRepository;

    public List<AdminApartmentDTO> getAllApartments() {
        return apartmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    // Guarda el estado completo del apartamento. Disponibilidad y precio se actualizan si vienen;
    // los descuentos se setean tal cual (null = sin ese descuento).
    @Transactional
    public AdminApartmentDTO updateApartment(Long id, AdminApartmentUpdateRequest request) {
        Apartment apartment = apartmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        if (request.getAvailable() != null) {
            apartment.setAvailable(request.getAvailable());
        }
        if (request.getPricePerNight() != null) {
            apartment.setPricePerNight(request.getPricePerNight());
        }

        apartment.setPercentDiscount(request.getPercentDiscount());
        apartment.setPercentDiscountMinNights(request.getPercentDiscountMinNights());
        apartment.setAmountDiscount(request.getAmountDiscount());
        apartment.setAmountDiscountMinNights(request.getAmountDiscountMinNights());

        apartmentRepository.save(apartment);
        return convertToDTO(apartment);
    }

    private AdminApartmentDTO convertToDTO(Apartment apartment) {
        return AdminApartmentDTO.builder()
                .id(apartment.getId())
                .name(apartment.getName())
                .available(apartment.isAvailable())
                .pricePerNight(apartment.getPricePerNight())
                .percentDiscount(apartment.getPercentDiscount())
                .percentDiscountMinNights(apartment.getPercentDiscountMinNights())
                .amountDiscount(apartment.getAmountDiscount())
                .amountDiscountMinNights(apartment.getAmountDiscountMinNights())
                .build();
    }
}
