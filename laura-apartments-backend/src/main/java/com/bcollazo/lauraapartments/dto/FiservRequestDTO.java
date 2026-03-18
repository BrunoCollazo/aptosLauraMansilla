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
public class FiservRequestDTO {

    private RequestHeader requestHeader;
    private String currency;
    private long amount;
    private long taxedAmount;
    private long taxAmount;
    private int indi;
    private String reference;
    private Invoice invoice;
    private Client client;
    private Config config;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestHeader {
        private String dateTime;
        private String netId;
        private String netDescription;
        private String auditNumber;
        private String version;
        private String digitalSign;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Invoice {
        private String number;
        private long totalAmount;
        private String currency;
        private String date;
        private String dueDate;
        private String description;
        private boolean finalConsumer;
        private String serial;
        private Address address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Address {
        private String country;
        private String city;
        private String street;
        private String doorNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Client {
        private String clientIdType;
        private String clientId;
        private String email;
        private String firstName;
        private String lastName;
        private String mobile;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Config {
        private String callbackUrl;
        private String callbackUrlSuffix;
        private Boolean useRedirect;
    }
}
