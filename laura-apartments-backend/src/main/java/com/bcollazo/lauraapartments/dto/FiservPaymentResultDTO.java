package com.bcollazo.lauraapartments.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FiservPaymentResultDTO {

    private FiservResponseDTO.FiservResponseHeader responseHeader;
    private String token;
    private String reference;
    private PaymentDetails payment;
    private java.util.List<Map<String, Object>> ecommerceAdditionalData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentDetails {
        private String paymentToken;
        private String accessToken;
        private String dateTime;
        private String state;
        private boolean authorized;
        private Boolean voided;
        private Boolean refunded;
        private String paymentmode;
        private String paymentEntity;
        private String currency;
        private long amount;
        private long taxedAmount;
        private Long taxAmount;
        private Long ivaDiscountAmount;
        private String installments;
        private Map<String, Object> card;
        private FiservRequestDTO.Client client;
        private FiservRequestDTO.Invoice invoice;
        private Map<String, Object> authorizer;
    }
}
