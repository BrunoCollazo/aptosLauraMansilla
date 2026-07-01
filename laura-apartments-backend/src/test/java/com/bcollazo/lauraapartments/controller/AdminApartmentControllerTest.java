package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.entity.Apartment;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminApartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApartmentRepository apartmentRepository;

    private Long apartmentId;

    @BeforeEach
    void setUp() {
        apartmentRepository.deleteAll();
        
        Apartment apt = Apartment.builder()
                .name("Luxury Suite")
                .pricePerNight(new BigDecimal("150.00"))
                .available(true)
                .build();
        
        apt = apartmentRepository.save(apt);
        apartmentId = apt.getId();
    }

    // El admin guarda todo el apartamento de una con PUT /{id} (reemplazó los PUT sueltos
    // de /availability y /price).
    @Test
    void shouldUpdateAvailability() throws Exception {
        mockMvc.perform(put("/api/admin/apartments/" + apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\":false,\"pricePerNight\":150.00}"))
                .andExpect(status().isOk());

        Apartment updated = apartmentRepository.findById(apartmentId).get();
        assertFalse(updated.isAvailable());
    }

    @Test
    void shouldUpdatePrice() throws Exception {
        mockMvc.perform(put("/api/admin/apartments/" + apartmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"available\":true,\"pricePerNight\":200.00}"))
                .andExpect(status().isOk());

        Apartment updated = apartmentRepository.findById(apartmentId).get();
        assertEquals(0, new BigDecimal("200.00").compareTo(updated.getPricePerNight()));
    }
}
