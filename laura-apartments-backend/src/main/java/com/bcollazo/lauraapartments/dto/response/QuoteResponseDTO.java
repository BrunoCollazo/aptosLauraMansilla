package com.bcollazo.lauraapartments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Cotización de una estadía: el desglose exacto de lo que se le va a cobrar al cliente.
// Lo usa el front para mostrar el total sin tener que duplicar descuentos ni IVA.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponseDTO {
    private Long apartmentId;
    private BigDecimal pricePerNight;
    private int nights;
    private BigDecimal baseAmount;          // precio x noches, sin descuento
    private String discountType;            // NONE | PERCENT | AMOUNT (cuál se aplicó)
    private BigDecimal discountAmount;      // cuánto se descontó (0 si ninguno)
    private BigDecimal subtotal;            // base - descuento (sin IVA)
    private BigDecimal ivaRate;             // tasa de IVA aplicada
    private BigDecimal ivaAmount;           // IVA sobre el subtotal
    private BigDecimal total;               // lo que paga el cliente (subtotal + IVA)
}
