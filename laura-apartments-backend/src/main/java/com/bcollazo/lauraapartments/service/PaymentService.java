package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.dto.PaymentRequestDTO;
import com.bcollazo.lauraapartments.dto.PaymentResponseDTO;
import com.bcollazo.lauraapartments.integration.FiservClient;
import com.bcollazo.lauraapartments.model.Apartment;
import com.bcollazo.lauraapartments.model.Payment;
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
        // 1. Validate apartment existence and availability
        Apartment apartment = apartmentRepository.findById(request.getApartmentId())
                .orElseThrow(() -> new RuntimeException("Apartment not found"));

        if (!apartment.isAvailable()) {
            throw new RuntimeException("Apartment is not available for rental");
        }

        // 2. Calculate final price using PricingService
        BigDecimal totalAmount = pricingService.calculateTotalAmount(
                apartment.getPricePerNight(), 
                request.getNights()
        );

        // 3. Interact with Fiserv integration placeholder
        String redirectUrl = fiservClient.createPaymentSession(totalAmount, request.getClientEmail());
        String fiservToken = fiservClient.generateToken();

        // 4. Create and save payment record
        Payment payment = Payment.builder()
                .apartmentId(apartment.getId())
                .nights(request.getNights())
                .totalAmount(totalAmount)
                .fiservToken(fiservToken)
                .status("PENDING")
                .build();

        paymentRepository.save(payment);

        return PaymentResponseDTO.builder()
                .redirectUrl(redirectUrl)
                .build();
    }
}
