package com.bcollazo.lauraapartments.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
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

    private Integer nights;

    private BigDecimal totalAmount;

    private String fiservToken; // Stores accessToken from initiation

    private String paymentToken; // Stores paymentToken from webhook/callback

    @Column(unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
