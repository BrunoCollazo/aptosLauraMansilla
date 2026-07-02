package com.bcollazo.lauraapartments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApartmentDTO {
    private Long id;
    private String name;
    private boolean available;
    private BigDecimal pricePerNight;

    private BigDecimal percentDiscount;
    private Integer percentDiscountMinNights;
    private BigDecimal amountDiscount;
    private Integer amountDiscountMinNights;
}