package com.bcollazo.lauraapartments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiservResponseDTO {
    private FiservResponseHeader responseHeader;
    private String token;
    private java.util.List<FiservPaymentResultDTO.PaymentDetails> payments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FiservResponseHeader {
        private String responseCode;
        private String responseDescription;
        private String rrn;
        private String digitalSign;
    }
}
