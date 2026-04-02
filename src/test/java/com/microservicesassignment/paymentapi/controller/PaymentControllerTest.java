package com.microservicesassignment.paymentapi.controller;
import com.microservicesassignment.paymentapi.dto.CreatePaymentRequest;
import com.microservicesassignment.paymentapi.dto.PaymentResponse;
import com.microservicesassignment.paymentapi.entity.PaymentMethod;
import com.microservicesassignment.paymentapi.entity.PaymentStatus;
import com.microservicesassignment.paymentapi.exception.GlobalExceptionHandler;
import com.microservicesassignment.paymentapi.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    private PaymentService paymentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        PaymentController controller = new PaymentController(paymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void createPayment_returns201WithLocationAndBody() throws Exception {
        PaymentResponse response = buildResponse(25L, 99L, PaymentStatus.PENDING);
        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(response);

        String requestBody = """
            {
              "salesOrderId": 99,
              "customerId": 7,
              "amount": 149.95,
              "paymentMethod": "CARD"
            }
            """;

        mockMvc.perform(post("/api/payments/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/payments/v1/payments/25"))
            .andExpect(jsonPath("$.id").value(25))
            .andExpect(jsonPath("$.salesOrderId").value(99))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // @Test
    // void createPayment_demoFailure_expectsWrongStatus() throws Exception {
    //     PaymentResponse response = buildResponse(25L, 99L, PaymentStatus.PENDING);
    //     when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(response);

    //     String requestBody = """
    //         {
    //           "salesOrderId": 99,
    //           "customerId": 7,
    //           "amount": 149.95,
    //           "paymentMethod": "CARD"
    //         }
    //         """;

    //     mockMvc.perform(post("/api/payments/v1/payments")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(requestBody))
    //         .andExpect(status().isCreated())
    //         .andExpect(jsonPath("$.status").value("PAID"));
    // }

    @Test
    void getPayments_returnsPagedEnvelope() throws Exception {
        PaymentResponse response = buildResponse(4L, 4L, PaymentStatus.PAID);
        when(paymentService.getPayments(eq(2L), eq(PaymentStatus.PAID), eq(PaymentMethod.CARD), any()))
            .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/payments/v1/payments")
                .param("customerId", "2")
                .param("status", "PAID")
                .param("paymentMethod", "CARD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(4))
            .andExpect(jsonPath("$.content[0].status").value("PAID"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void createPayment_validationFailure_returns400() throws Exception {
        String invalidBody = """
            {
              "salesOrderId": 0,
              "customerId": 0,
              "amount": -1.00,
              "paymentMethod": null
            }
            """;

        mockMvc.perform(post("/api/payments/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("validation failed"))
            .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void deletePayment_returns204() throws Exception {
        mockMvc.perform(delete("/api/payments/v1/payments/8"))
            .andExpect(status().isNoContent());

        verify(paymentService).deletePayment(8L);
    }

    private PaymentResponse buildResponse(Long id, Long salesOrderId, PaymentStatus status) {
        PaymentResponse response = new PaymentResponse();
        response.setId(id);
        response.setSalesOrderId(salesOrderId);
        response.setCustomerId(2L);
        response.setAmount(BigDecimal.valueOf(149.95));
        response.setPaymentMethod(PaymentMethod.CARD);
        response.setStatus(status);
        response.setTransactionReference("TXN-" + salesOrderId + "-TEST000001");
        response.setCreatedAt(LocalDateTime.of(2026, 3, 31, 12, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 3, 31, 12, 1));
        return response;
    }
}
