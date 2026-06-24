package com.bcollazo.lauraapartments.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "apartments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Apartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private boolean available;

    private BigDecimal pricePerNight;

    // Descuentos por apartamento. Cada uno aplica solo si su umbral de noches y su valor están
    // seteados (> 0). Si una estadía califica para los dos, se aplica el que más conviene al
    // cliente (mayor descuento), nunca se suman. Ver PricingService.
    private BigDecimal percentDiscount;          // % off (ej: 10 = 10%)
    private Integer percentDiscountMinNights;    // noches mínimas para el descuento %

    private BigDecimal amountDiscount;           // monto fijo off del total de la reserva
    private Integer amountDiscountMinNights;     // noches mínimas para el descuento de monto fijo
}