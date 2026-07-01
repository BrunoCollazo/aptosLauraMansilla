package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.response.QuoteResponseDTO;
import com.bcollazo.lauraapartments.entity.Apartment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // El precio de los apartamentos se guarda SIN IVA. El alquiler temporario paga IVA 10% solo
    // en TEMPORADA (15/nov -> Domingo de Pascua); fuera de temporada está EXONERADO (0%).
    // El IVA se aplica por noche: cada noche cuya fecha cae en temporada lleva el 10%.
    public static final BigDecimal IVA_RATE = new BigDecimal("0.10");

    private static final String DISCOUNT_NONE = "NONE";
    private static final String DISCOUNT_PERCENT = "PERCENT";
    private static final String DISCOUNT_AMOUNT = "AMOUNT";

    // Desglose completo de la estadía. Calcula el total EXACTAMENTE como lo cobra PaymentService,
    // así lo que muestra el front no puede divergir: base con descuento + IVA por temporada.
    public QuoteResponseDTO calculateQuote(Apartment apartment, LocalDate checkIn, int nights) {
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

        // IVA por temporada: el 10% aplica solo a la porción de la estadía que cae en temporada.
        // Contamos las noches en temporada y prorrateamos el subtotal (estadía toda en temporada
        // -> 10% pleno; toda fuera -> 0; a caballo del límite -> proporcional).
        int inSeasonNights = countInSeasonNights(checkIn, nights);
        BigDecimal taxableSubtotal = subtotal
                .multiply(BigDecimal.valueOf(inSeasonNights))
                .divide(new BigDecimal(nights), 2, RoundingMode.HALF_UP);
        BigDecimal ivaAmount = taxableSubtotal.multiply(IVA_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(ivaAmount).setScale(2, RoundingMode.HALF_UP);

        return QuoteResponseDTO.builder()
                .apartmentId(apartment.getId())
                .pricePerNight(apartment.getPricePerNight())
                .checkIn(checkIn)
                .nights(nights)
                .inSeasonNights(inSeasonNights)
                .baseAmount(baseAmount)
                .discountType(discountType)
                .discountAmount(discountAmount)
                .subtotal(subtotal)
                .ivaRate(IVA_RATE)
                .ivaAmount(ivaAmount)
                .total(total)
                .build();
    }

    // Cuántas de las 'nights' noches (a partir de checkIn) caen en temporada alta.
    private int countInSeasonNights(LocalDate checkIn, int nights) {
        int count = 0;
        for (int i = 0; i < nights; i++) {
            if (isHighSeason(checkIn.plusDays(i))) {
                count++;
            }
        }
        return count;
    }

    // Temporada alta = del 15 de noviembre al Domingo de Pascua (inclusive), envolviendo el año
    // nuevo. Una fecha está en temporada si es >= 15/nov de su año O <= Pascua de su año.
    static boolean isHighSeason(LocalDate date) {
        LocalDate nov15 = LocalDate.of(date.getYear(), 11, 15);
        LocalDate easter = easterSunday(date.getYear());
        return !date.isBefore(nov15) || !date.isAfter(easter);
    }

    // Domingo de Pascua (fin de Semana Santa) por el algoritmo de Computus gregoriano
    // (Meeus/Jones/Butcher). Es fecha móvil, por eso se calcula en vez de hardcodear.
    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
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
