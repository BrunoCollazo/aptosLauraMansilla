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

    // indi de Fiserv (indicador de devolución de impuestos), valores que confirmó Rodrigo:
    //   0 = no devuelve IVA
    //   1 = IVA restaurantes, Ley 17.934
    //   2 = IMESI
    //   4 = impuestos Ley 18.999
    //   6 = IVA Ley 19.210   ← el que usamos en producción (alquiler de apartamentos)
    // OJO: el desglose de PaymentService asume IVA 22%, sirve para 1 y 6 pero NO para 2/4.
    private Integer indi;

    @Email(message = "Invalid email format")
    private String clientEmail;
}
