package com.bcollazo.lauraapartments.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class FiservConfig {
    @Value("${FISERV_NET_ID}")           private String netId;
    @Value("${FISERV_BASE_URL}")         private String baseUrl;      // e.g. https://host:port/service
    @Value("${FISERV_PAGE_URL}")         private String pageUrl;      // URL to POST token to open payment page
    /**
     * Pre-registered code at Fiserv (e.g. "99") which is mapped on their side
     * to the actual callback URL of our backend.
     */
    @Value("${FISERV_CALLBACK_URL_CODE}") private String callbackUrlCode;
    @Value("${FISERV_KEYSTORE_PATH}")    private String keystorePath;
    @Value("${FISERV_KEYSTORE_PASS}")    private String p12Pass;
    @Value("${FISERV_KEY_ALIAS}")        private String keyAlias;
    @Value("${FISERV_KEY_PASS}")         private String keyPass;
    @Value("${FISERV_PUBLIC_CERT_PATH}") private String publicCertPath; // Fiserv's .cer for verifying responses
    @Value("${FISERV_VERSION}")          private String version;        // e.g. "3.02"

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }
}