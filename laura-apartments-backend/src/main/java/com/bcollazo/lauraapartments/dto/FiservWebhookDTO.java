package com.bcollazo.lauraapartments.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FiservWebhookDTO {
    private FiservRequestDTO.RequestHeader requestHeader;
    private FiservPaymentResultDTO.PaymentDetails payment;
    private String event;
}
