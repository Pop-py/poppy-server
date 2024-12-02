package com.poppy.domain.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
public class TossPaymentClient {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payment.base-url}")
    private String baseUrl;

    @Value("${payment.secret-key}")
    private String secretKey;

    public boolean confirmPayment(String paymentKey, String orderId, Long amount) {
        // 결제 확인 요청
        String url = baseUrl + "/confirm";
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        // 요청 데이터 설정
        Map<String, Object> requestData = Map.of(
                "orderId", orderId,
                "amount", amount,
                "paymentKey", paymentKey
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            return true; // 성공 시
        }
        catch (Exception e) {
            return false; // 실패 시
        }
    }

    public boolean cancelPayment(String paymentKey, String cancelReason) {
        String url = baseUrl + "/" + paymentKey + "/cancel";
        String authHeader = "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        Map<String, String> requestData = Map.of("cancelReason", cancelReason);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestData, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
