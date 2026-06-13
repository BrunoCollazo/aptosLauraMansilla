package com.bcollazo.lauraapartments.service;

import com.bcollazo.lauraapartments.config.FiservConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FiservSignatureService {

    private final FiservConfig fiservConfig;
    private PrivateKey privateKey;
    private PublicKey fiservPublicKey;

    public boolean isReady() {
        return privateKey != null && fiservPublicKey != null;
    }

    @PostConstruct
    public void init() {
        try {
            loadMerchantPrivateKey();
            loadFiservPublicKey();
        } catch (Exception e) {
            log.warn("Could not load Fiserv keys during startup: {}. This is expected in test environments without actual keys.", e.getMessage());
        }
    }

    private void loadMerchantPrivateKey() throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(fiservConfig.getKeystorePath())) {
            keystore.load(fis, fiservConfig.getP12Pass().toCharArray());
        }
        privateKey = (PrivateKey) keystore.getKey(fiservConfig.getKeyAlias(), fiservConfig.getKeyPass().toCharArray());
    }

    private void loadFiservPublicKey() throws Exception {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        try (FileInputStream is = new FileInputStream(fiservConfig.getPublicCertPath())) {
            X509Certificate cer = (X509Certificate) fact.generateCertificate(is);
            fiservPublicKey = cer.getPublicKey();
        }
    }

    public String buildSignatureText(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);

        for (String key : sortedKeys) {
            if ("digitalSign".equals(key)) continue;
            Object value = map.get(key);
            if (value == null) continue;

            if (value instanceof Map) {
                sb.append(buildSignatureText((Map<String, Object>) value));
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        sb.append(buildSignatureText((Map<String, Object>) item));
                    } else {
                        sb.append(key).append(sanitize(String.valueOf(item)));
                    }
                }
            } else {
                sb.append(key).append(sanitize(String.valueOf(value)));
            }
        }
        return sb.toString();
    }

    public String generateSign(Map<String, Object> payload) {
        try {
            String textToSign = buildSignatureText(payload).toUpperCase();
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(textToSign.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new RuntimeException("Error generating Fiserv signature", e);
        }
    }

    public boolean verifySign(Map<String, Object> payload, String receivedSign) {
        try {
            String textToVerify = buildSignatureText(payload).toUpperCase();
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(fiservPublicKey);
            signature.update(textToVerify.getBytes(StandardCharsets.UTF_8));
            byte[] decodedSign = Base64.getDecoder().decode(receivedSign);
            return signature.verify(decodedSign);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying Fiserv signature", e);
        }
    }

    private String sanitize(String input) {
        if (input == null) return "";
        // Remove all non-printable characters: keep only ASCII 0x21 to 0x7E
        return input.replaceAll("[^\\x21-\\x7E]", "");
    }
}