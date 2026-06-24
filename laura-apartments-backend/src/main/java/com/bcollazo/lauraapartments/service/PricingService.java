package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.response.QuoteResponseDTO;
import com.bcollazo.lauraapartments.entity.Discount;
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

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // El precio de los apartamentos se guarda SIN IVA; le sumamos este 10% encima.
    // OJO: falta que el contador confirme la tasa antes de salir a producción.
    public static final BigDecimal IVA_RATE = new BigDecimal("0.10");

    // Subtotal con descuento por estadía (sin IVA). Lo usa el quote como base del cálculo.
    public BigDecimal calculateTotalAmount(BigDecimal pricePerNight, int nights) {
        BigDecimal baseTotal = pricePerNight.multiply(new BigDecimal(nights));
        BigDecimal percentageDiscount = findDiscountPercentage(nights);

        if (percentageDiscount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = baseTotal.multiply(percentageDiscount)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            return baseTotal.subtract(discountAmount);
        }

        return baseTotal;
    }

    // Desglose completo de la estadía. Calcula el total EXACTAMENTE como lo hace PaymentService
    // al cobrar (subtotal con descuento + 10% IVA), así lo que muestra el front no puede divergir.
    public QuoteResponseDTO calculateQuote(BigDecimal pricePerNight, int nights) {
        BigDecimal baseAmount = pricePerNight.multiply(new BigDecimal(nights));
        BigDecimal discountPercentage = findDiscountPercentage(nights);
        BigDecimal subtotal = calculateTotalAmount(pricePerNight, nights);
        BigDecimal discountAmount = baseAmount.subtract(subtotal);

        BigDecimal total = subtotal.add(subtotal.multiply(IVA_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        // El IVA mostrado lo sacamos del total ya redondeado para que el desglose cuadre.
        BigDecimal ivaAmount = total.subtract(subtotal);

        return QuoteResponseDTO.builder()
                .pricePerNight(pricePerNight)
                .nights(nights)
                .baseAmount(baseAmount)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .subtotal(subtotal)
                .ivaRate(IVA_RATE)
                .ivaAmount(ivaAmount)
                .total(total)
                .build();
    }

    // Busca el mejor descuento que califique: gana el de mayor minNights que la estadía alcance.
    private BigDecimal findDiscountPercentage(int nights) {
        List<Discount> applicableDiscounts = discountRepository.findAllByOrderByMinNightsDesc();

        for (Discount discount : applicableDiscounts) {
            if (nights >= discount.getMinNights()) {
                return discount.getPercentage();
            }
        }

        return BigDecimal.ZERO;
    }
}
