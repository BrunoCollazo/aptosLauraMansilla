package com.bcollazo.lauraapartments.integration;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class FiservClient {

    /**
     * Placeholder for Fiserv Carat Gateway API integration.
     * In a real scenario, this would call the Fiserv API and return a session token or redirect URL.
     */
    public String createPaymentSession(BigDecimal amount, String clientEmail) {
        // Mocking the generation of a redirect URL from Fiserv
        return "https://gateway.fiserv.example.com/pay/" + UUID.randomUUID().toString();
    }

    public String generateToken() {
        return UUID.randomUUID().toString();
    }
}
