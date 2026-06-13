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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @BeforeEach
    void setUp() {
        apartmentRepository.deleteAll();
        
        Apartment apt1 = Apartment.builder()
                .name("Luxury Suite")
                .pricePerNight(new BigDecimal("150.00"))
                .available(true)
                .build();
        
        Apartment apt2 = Apartment.builder()
                .name("Cozy Studio")
                .pricePerNight(new BigDecimal("80.00"))
                .available(false)
                .build();
        
        apartmentRepository.save(apt1);
        apartmentRepository.save(apt2);
    }

    @Test
    void shouldGetAllApartments() throws Exception {
        mockMvc.perform(get("/api/apartments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Luxury Suite")))
                .andExpect(jsonPath("$[0].pricePerNight", is(150.0)))
                .andExpect(jsonPath("$[0].available", is(true)))
                .andExpect(jsonPath("$[1].name", is("Cozy Studio")))
                .andExpect(jsonPath("$[1].pricePerNight", is(80.0)))
                .andExpect(jsonPath("$[1].available", is(false)));
    }
}
