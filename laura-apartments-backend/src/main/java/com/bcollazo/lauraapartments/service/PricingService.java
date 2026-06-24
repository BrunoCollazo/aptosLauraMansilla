package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.response.QuoteResponseDTO;
import com.bcollazo.lauraapartments.entity.Apartment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // El precio de los apartamentos se guarda SIN IVA; le sumamos este 10% encima.
    // OJO: falta que el contador confirme la tasa antes de salir a producción.
    public static final BigDecimal IVA_RATE = new BigDecimal("0.10");

    private static final String DISCOUNT_NONE = "NONE";
    private static final String DISCOUNT_PERCENT = "PERCENT";
    private static final String DISCOUNT_AMOUNT = "AMOUNT";

    // Desglose completo de la estadía. Calcula el total EXACTAMENTE como lo cobra PaymentService,
    // así lo que muestra el front no puede divergir: base con descuento + 10% IVA.
    public QuoteResponseDTO calculateQuote(Apartment apartment, int nights) {
        BigDecimal baseAmount = apartment.getPricePerNight().multiply(new BigDecimal(nights));

        // Calculamos cuánto descuenta cada tipo (0 si no aplica) y nos quedamos con el mayor:
        // el que más le conviene al cliente. Nunca se suman.
        BigDecimal percentOff = percentDiscountOff(apartment, nights, baseAmount);
        BigDecimal amountOff = amountDiscountOff(apartment, nights, baseAmount);

        String discountType = DISCOUNT_NONE;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (percentOff.compareTo(amountOff) >= 0 && percentOff.signum() > 0) {
            discountType = DISCOUNT_PERCENT;
            discountAmount = percentOff;
        } else if (amountOff.signum() > 0) {
            discountType = DISCOUNT_AMOUNT;
            discountAmount = amountOff;
        }

        BigDecimal subtotal = baseAmount.subtract(discountAmount);
        BigDecimal total = subtotal.add(subtotal.multiply(IVA_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        // El IVA mostrado lo sacamos del total ya redondeado para que el desglose cuadre.
        BigDecimal ivaAmount = total.subtract(subtotal);

        return QuoteResponseDTO.builder()
                .apartmentId(apartment.getId())
                .pricePerNight(apartment.getPricePerNight())
                .nights(nights)
                .baseAmount(baseAmount)
                .discountType(discountType)
                .discountAmount(discountAmount)
                .subtotal(subtotal)
                .ivaRate(IVA_RATE)
                .ivaAmount(ivaAmount)
                .total(total)
                .build();
    }

    // Lo que descuenta el % (0 si no está configurado o la estadía no llega al umbral).
    private BigDecimal percentDiscountOff(Apartment apartment, int nights, BigDecimal baseAmount) {
        BigDecimal percent = apartment.getPercentDiscount();
        Integer minNights = apartment.getPercentDiscountMinNights();
        if (percent == null || percent.signum() <= 0 || minNights == null || nights < minNights) {
            return BigDecimal.ZERO;
        }
        return baseAmount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    // Lo que descuenta el monto fijo, topeado al base para no dejar el total negativo.
    private BigDecimal amountDiscountOff(Apartment apartment, int nights, BigDecimal baseAmount) {
        BigDecimal amount = apartment.getAmountDiscount();
        Integer minNights = apartment.getAmountDiscountMinNights();
        if (amount == null || amount.signum() <= 0 || minNights == null || nights < minNights) {
            return BigDecimal.ZERO;
        }
        return amount.min(baseAmount);
    }
}
