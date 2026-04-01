package com.microservicesassignment.paymentapi.integration;

import com.microservicesassignment.paymentapi.dto.CreatePaymentRequest;
import com.microservicesassignment.paymentapi.entity.Payment;
import com.microservicesassignment.paymentapi.entity.PaymentMethod;
import com.microservicesassignment.paymentapi.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void startupLoadsSeedData() {
        assertEquals(8, paymentRepository.count());

        String response = restClient().get()
            .uri("/api/payments/v1/payments")
            .retrieve()
            .body(String.class);

        assertTrue(response.contains("\"totalElements\":8"));
    }

    @Test
    void createPaymentAndFetchBySalesOrderId_overHttp() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setSalesOrderId(999L);
        request.setCustomerId(42L);
        request.setAmount(BigDecimal.valueOf(560.75));
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        ResponseEntity<String> createResponse = restClient().post()
            .uri("/api/payments/v1/payments")
            .body(request)
            .retrieve()
            .toEntity(String.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertTrue(createResponse.getBody().contains("\"salesOrderId\":999"));
        assertTrue(createResponse.getBody().contains("\"status\":\"PENDING\""));
        String location = createResponse.getHeaders().getFirst("Location");
        assertNotNull(location);
        Long createdId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        String fetchResponse = restClient().get()
            .uri("/api/payments/v1/payments/order/999")
            .retrieve()
            .body(String.class);

        assertTrue(fetchResponse.contains("\"id\":" + createdId));
        assertTrue(fetchResponse.contains("\"paymentMethod\":\"BANK_TRANSFER\""));
    }

    @Test
    void deletePendingPayment_removesRow() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setSalesOrderId(1001L);
        request.setCustomerId(88L);
        request.setAmount(BigDecimal.valueOf(50.00));
        request.setPaymentMethod(PaymentMethod.CASH);

        ResponseEntity<String> createResponse = restClient().post()
            .uri("/api/payments/v1/payments")
            .body(request)
            .retrieve()
            .toEntity(String.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        String location = createResponse.getHeaders().getFirst("Location");
        assertNotNull(location);
        Long paymentId = Long.valueOf(location.substring(location.lastIndexOf('/') + 1));

        restClient().delete()
            .uri("/api/payments/v1/payments/{paymentId}", paymentId)
            .retrieve()
            .toBodilessEntity();

        Optional<Payment> deletedPayment = paymentRepository.findById(paymentId);
        assertFalse(deletedPayment.isPresent());
    }

    private RestClient restClient() {
        return RestClient.builder()
            .baseUrl("http://localhost:" + port)
            .build();
    }
}
