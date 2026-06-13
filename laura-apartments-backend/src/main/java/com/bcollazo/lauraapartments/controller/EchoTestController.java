package com.bcollazo.lauraapartments.controller;

import com.bcollazo.lauraapartments.service.FiservSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fiserv")
@RequiredArgsConstructor
public class EchoTestController {

    private final FiservSignatureService signatureService;

    @PostMapping("/echoTest")
    public ResponseEntity<Map<String, Object>> echoTest(@RequestBody Map<String, Object> request) {
        log.info("Received Fiserv EchoTest request: {}", request);

        Map<String, Object> requestHeader = (Map<String, Object>) request.get("requestHeader");
        if (requestHeader != null) {
            String digitalSign = (String) requestHeader.get("digitalSign");
            try {
                if (!signatureService.verifySign(request, digitalSign)) {
                    log.warn("Invalid signature in Fiserv EchoTest request");
                }
            } catch (Exception e) {
                log.warn("Error verifying signature in Fiserv EchoTest request: {}", e.getMessage());
            }
        } else {
            log.warn("Received Fiserv EchoTest request without requestHeader");
        }

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> responseHeader = new HashMap<>();
        responseHeader.put("responseCode", "00");
        responseHeader.put("responseDescription", "OK");

        String signature;
        try {
            response.put("responseHeader", responseHeader);
            signature = signatureService.generateSign(response);
        } catch (Exception e) {
            log.warn("Could not sign echoTest response: {}", e.getMessage());
            signature = "";
        }
        responseHeader.put("digitalSign", signature);
        response.put("responseHeader", responseHeader);

        return ResponseEntity.ok(response);
    }
}