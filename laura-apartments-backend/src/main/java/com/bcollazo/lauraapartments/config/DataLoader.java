package com.bcollazo.lauraapartments.config;

import com.bcollazo.lauraapartments.entity.Apartment;
import com.bcollazo.lauraapartments.entity.Discount;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import com.bcollazo.lauraapartments.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ApartmentRepository apartmentRepository;
    private final DiscountRepository discountRepository;

    @Override
    public void run(String... args) {

        if (apartmentRepository.count() == 0) {

            for (int i = 1; i <= 4; i++) {

                Apartment apartment = new Apartment();
                apartment.setName("Apartamento " + i);
                apartment.setAvailable(true);
                apartment.setPricePerNight(new BigDecimal("2500"));

                apartmentRepository.save(apartment);
            }
        }

        if (discountRepository.count() == 0) {
            discountRepository.save(Discount.builder().minNights(7).percentage(new BigDecimal("10")).build());
            discountRepository.save(Discount.builder().minNights(14).percentage(new BigDecimal("15")).build());
        }
    }
}