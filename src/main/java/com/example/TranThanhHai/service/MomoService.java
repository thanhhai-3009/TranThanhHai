package com.example.TranThanhHai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class MomoService {

    private static final String PARTNER_CODE = "MOMO";
    private static final String ACCESS_KEY = "F8BBA842ECF85";
    private static final String SECRET_KEY = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private static final String REQUEST_TYPE = "payWithMethod";

    private static final String CREATE_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/create";
    private static final String QUERY_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/query";

    @Value("${momo.redirect-url:http://localhost:8080/order/momo-return}")
    private String redirectUrl;

    @Value("${momo.ipn-url:http://localhost:8080/order/momo-return}")
    private String ipnUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String createPaymentRequest(String amount) {
        try {
            String requestId = PARTNER_CODE + System.currentTimeMillis();
            String orderId = requestId;
            String orderInfo = "Thanh toan don hang";
            String extraData = "";

            String rawSignature = String.format(
                    "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                    ACCESS_KEY, amount, extraData, ipnUrl, orderId, orderInfo, PARTNER_CODE, redirectUrl,
                    requestId, REQUEST_TYPE);

            String signature = signHmacSHA256(rawSignature, SECRET_KEY);

            String payload = "{"
                    + "\"partnerCode\":\"" + jsonEscape(PARTNER_CODE) + "\","
                    + "\"accessKey\":\"" + jsonEscape(ACCESS_KEY) + "\","
                    + "\"requestId\":\"" + jsonEscape(requestId) + "\","
                    + "\"amount\":\"" + jsonEscape(amount) + "\","
                    + "\"orderId\":\"" + jsonEscape(orderId) + "\","
                    + "\"orderInfo\":\"" + jsonEscape(orderInfo) + "\","
                    + "\"redirectUrl\":\"" + jsonEscape(redirectUrl) + "\","
                    + "\"ipnUrl\":\"" + jsonEscape(ipnUrl) + "\","
                    + "\"extraData\":\"" + jsonEscape(extraData) + "\","
                    + "\"requestType\":\"" + jsonEscape(REQUEST_TYPE) + "\","
                    + "\"signature\":\"" + jsonEscape(signature) + "\","
                    + "\"lang\":\"vi\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CREATE_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.body();
        } catch (Exception e) {
            return "{\"error\":\"Failed to create payment request: " + jsonEscape(e.getMessage()) + "\"}";
        }
    }

    public String checkPaymentStatus(String orderId) {
        try {
            String requestId = PARTNER_CODE + System.currentTimeMillis();

            String rawSignature = String.format(
                    "accessKey=%s&orderId=%s&partnerCode=%s&requestId=%s",
                    ACCESS_KEY, orderId, PARTNER_CODE, requestId);

            String signature = signHmacSHA256(rawSignature, SECRET_KEY);

            String payload = "{"
                    + "\"partnerCode\":\"" + jsonEscape(PARTNER_CODE) + "\","
                    + "\"accessKey\":\"" + jsonEscape(ACCESS_KEY) + "\","
                    + "\"requestId\":\"" + jsonEscape(requestId) + "\","
                    + "\"orderId\":\"" + jsonEscape(orderId) + "\","
                    + "\"signature\":\"" + jsonEscape(signature) + "\","
                    + "\"lang\":\"vi\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(QUERY_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.body();
        } catch (Exception e) {
            return "{\"error\":\"Failed to check payment status: " + jsonEscape(e.getMessage()) + "\"}";
        }
    }

    private static String signHmacSHA256(String data, String key) throws Exception {
        Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSHA256.init(secretKey);
        byte[] hash = hmacSHA256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

}