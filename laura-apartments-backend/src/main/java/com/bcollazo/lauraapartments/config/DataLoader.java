package com.bcollazo.lauraapartments.config;

import com.bcollazo.lauraapartments.model.Apartment;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ApartmentRepository apartmentRepository;

    @Override
    public void run(String... args) {

        if (apartmentRepository.count() == 0) {

            for (int i = 1; i <= 4; i++) {

                Apartment apartment = new Apartment();
                apartment.setName("Apartamento " + i);
                apartment.setAvailable(true);
                apartment.setPricePerNight(new BigDecimal("4000"));

                apartmentRepository.save(apartment);
            }
        }
    }
}