package com.bcollazo.lauraapartments.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDTO {
    @NotNull(message = "Apartment ID is required")
    private Long apartmentId;

    @NotNull(message = "Number of nights is required")
    @Min(value = 1, message = "Nights must be at least 1")
    private Integer nights;

    @NotNull(message = "Client email is required")
    @Email(message = "Invalid email format")
    private String clientEmail;
}