package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.model.Discount;
import com.bcollazo.lauraapartments.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final DiscountRepository discountRepository;

    public BigDecimal calculateTotalAmount(BigDecimal pricePerNight, int nights) {
        BigDecimal baseTotal = pricePerNight.multiply(new BigDecimal(nights));
        
        // Find the best applicable discount based on number of nights
        List<Discount> applicableDiscounts = discountRepository.findAllByOrderByMinNightsDesc();
        
        BigDecimal percentageDiscount = BigDecimal.ZERO;
        
        for (Discount discount : applicableDiscounts) {
            if (nights >= discount.getMinNights()) {
                percentageDiscount = discount.getPercentage();
                break; // Take the highest minNights discount
            }
        }

        if (percentageDiscount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = baseTotal.multiply(percentageDiscount)
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            return baseTotal.subtract(discountAmount);
        }

        return baseTotal;
    }
}
