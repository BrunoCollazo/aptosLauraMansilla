package com.bcollazo.lauraapartments.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

// Lo que mandamos para disparar un pago con monto, moneda e indi a mano. Es solo para la
// homologación de tarjetas de Fiserv: los montos exactos de la planilla, probar dólares y
// las leyes de devolución de impuestos. La web normal sigue cobrando en pesos por apartamento.
@Data
public class HomologationPaymentRequestDTO {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    // "UYU"/"858" o "USD"/"840". Si viene vacío, va en pesos.
    private String currency;

    // indi de Fiserv: 0 = sin devolución, 6 = Ley 19210 (IVA). Si es mayor a 0
    // calculamos solo el desglose del IVA.
    private Integer indi;

    @Email(message = "Invalid email format")
    private String clientEmail;
}
