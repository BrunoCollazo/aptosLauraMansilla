package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.PaymentRequestDTO;
import com.bcollazo.lauraapartments.dto.PaymentResponseDTO;
import com.bcollazo.lauraapartments.integration.FiservClient;
import com.bcollazo.lauraapartments.model.Apartment;
import com.bcollazo.lauraapartments.model.Payment;
import com.bcollazo.lauraapartments.model.PaymentStatus;
import com.bcollazo.lauraapartments.repository.ApartmentRepository;
import com.bcollazo.lauraapartments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ApartmentRepository apartmentRepository;
    private final PaymentRepository paymentRepository;
    private final PricingService pricingService;
    private final FiservClient fiservClient;

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO request) {

        // 1. Validate apartment existence
        Apartment apartment = apartmentRepository
                .findById(request.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        // 2. Validate availability
        if (!apartment.isAvailable()) {
            throw new RuntimeException("Apartment is not available for rental");
        }

        // 3. Calculate price
        BigDecimal totalAmount = pricingService.calculateTotalAmount(
                apartment.getPricePerNight(),
                request.getNights()
        );

        // 4. Generate Fiserv token (placeholder for now)
        String fiservToken = fiservClient.generateToken();

        // 5. Create payment record
        Payment payment = Payment.builder()
                .apartment(apartment)   // <-- relación JPA
                .nights(request.getNights())
                .totalAmount(totalAmount)
                .fiservToken(fiservToken)
                .status(PaymentStatus.PENDING)  // <-- enum
                .build();

        paymentRepository.save(payment);

        // 6. Create payment session (redirect to gateway)
        String redirectUrl = fiservClient.createPaymentSession(
                totalAmount,
                request.getClientEmail()
        );

        return PaymentResponseDTO.builder()
                .redirectUrl(redirectUrl)
                .build();
    }
}