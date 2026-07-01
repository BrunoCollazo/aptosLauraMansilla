package com.bcollazo.lauraapartments.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class PaymentRequestDTO {
    @NotNull(message = "Apartment ID is required")
    private Long apartmentId;

    @NotNull(message = "Check-in date is required")
    @FutureOrPresent(message = "Check-in date cannot be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "Number of nights is required")
    @Min(value = 1, message = "Nights must be at least 1")
    private Integer nights;

    @NotNull(message = "Client email is required")
    @Email(message = "Invalid email format")
    private String clientEmail;
}