package com.bcollazo.lauraapartments.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private LocalDate checkIn; // fecha de entrada de la estadía (define la temporada / IVA)

    private Integer nights;

    private BigDecimal totalAmount;

    private String clientEmail;

    private String fiservToken; // Stores accessToken from initiation

    private String paymentToken; // Stores paymentToken from webhook/callback

    @Column(unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String cardBrand; // e.g. "Mastercard", from payment.card.cardBrandName

    private String cardMask; // e.g. "515845******0949", from payment.card.cardMask

    private String authorizationCode; // from payment.authorizer.authorizationCode

    private Long confirmedAmount; // amount charged per Fiserv, in cents

    private String currency; // Fiserv currency code, e.g. "858"

    private LocalDateTime confirmedAt; // payment.dateTime from Fiserv (when authorized/confirmed)

    @CreationTimestamp
    private LocalDateTime createdAt;
}