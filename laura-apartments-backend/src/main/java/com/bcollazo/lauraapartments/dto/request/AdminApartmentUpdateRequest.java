package com.bcollazo.lauraapartments.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Lo que manda el admin al apretar Guardar en una tarjeta de apartamento: el estado completo
// (disponibilidad, precio y los dos descuentos). Los descuentos pueden venir null = sin descuento.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminApartmentUpdateRequest {
    private Boolean available;
    private BigDecimal pricePerNight;

    private BigDecimal percentDiscount;
    private Integer percentDiscountMinNights;
    private BigDecimal amountDiscount;
    private Integer amountDiscountMinNights;
}
