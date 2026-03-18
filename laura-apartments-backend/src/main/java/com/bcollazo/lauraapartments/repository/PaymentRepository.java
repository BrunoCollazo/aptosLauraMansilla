package com.bcollazo.lauraapartments.repository;

import com.bcollazo.lauraapartments.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByFiservToken(String fiservToken);
    Optional<Payment> findByPaymentToken(String paymentToken);
    Optional<Payment> findByReference(String reference);
}
