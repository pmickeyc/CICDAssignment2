package com.microservicesassignment.paymentapi.service;

import com.microservicesassignment.paymentapi.dto.CreatePaymentRequest;
import com.microservicesassignment.paymentapi.dto.PaymentResponse;
import com.microservicesassignment.paymentapi.entity.Payment;
import com.microservicesassignment.paymentapi.entity.PaymentMethod;
import com.microservicesassignment.paymentapi.entity.PaymentStatus;
import com.microservicesassignment.paymentapi.exception.ConflictException;
import com.microservicesassignment.paymentapi.exception.InvalidRequestException;
import com.microservicesassignment.paymentapi.exception.ResourceNotFoundException;
import com.microservicesassignment.paymentapi.mapper.PaymentMapper;
import com.microservicesassignment.paymentapi.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, new PaymentMapper());
    }

    @Test
    void createPayment_setsPendingStatusAndGeneratedReference() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setSalesOrderId(99L);
        request.setCustomerId(7L);
        request.setAmount(BigDecimal.valueOf(199.99));
        request.setPaymentMethod(PaymentMethod.CARD);

        when(paymentRepository.existsBySalesOrderId(99L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(55L);
            payment.setCreatedAt(LocalDateTime.of(2026, 3, 31, 12, 0));
            payment.setUpdatedAt(LocalDateTime.of(2026, 3, 31, 12, 0));
            return payment;
        });

        PaymentResponse response = paymentService.createPayment(request);

        assertEquals(55L, response.getId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
        assertEquals(BigDecimal.valueOf(199.99), response.getAmount());
        assertTrue(response.getTransactionReference().startsWith("TXN-99-"));

        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(99L, savedPayment.getSalesOrderId());
        assertEquals(7L, savedPayment.getCustomerId());
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
    }

    @Test
    void createPayment_throwsConflictWhenSalesOrderAlreadyExists() {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setSalesOrderId(2L);
        request.setCustomerId(1L);
        request.setAmount(BigDecimal.valueOf(25));
        request.setPaymentMethod(PaymentMethod.CASH);

        when(paymentRepository.existsBySalesOrderId(2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> paymentService.createPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void deletePayment_rejectsPaidPayment() {
        Payment payment = buildPayment(12L, 7L, PaymentStatus.PAID);

        when(paymentRepository.findById(12L)).thenReturn(Optional.of(payment));

        assertThrows(InvalidRequestException.class, () -> paymentService.deletePayment(12L));
        verify(paymentRepository, never()).delete(any(Payment.class));
    }

    @Test
    void getPayments_mapsRepositoryPageToResponsePage() {
        PageRequest pageable = PageRequest.of(0, 20);
        Payment payment = buildPayment(3L, 1L, PaymentStatus.FAILED);
        when(paymentRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Payment>>any(), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));

        Page<PaymentResponse> result = paymentService.getPayments(1L, PaymentStatus.FAILED, PaymentMethod.CARD, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(3L, result.getContent().getFirst().getId());
        assertEquals(PaymentStatus.FAILED, result.getContent().getFirst().getStatus());
    }

    @Test
    void getPaymentBySalesOrderId_throwsWhenMissing() {
        when(paymentRepository.findBySalesOrderId(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> paymentService.getPaymentBySalesOrderId(404L)
        );

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("salesOrderId=404"));
    }

    private Payment buildPayment(Long id, Long salesOrderId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setSalesOrderId(salesOrderId);
        payment.setCustomerId(1L);
        payment.setAmount(BigDecimal.valueOf(80.50));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setStatus(status);
        payment.setTransactionReference("TXN-" + salesOrderId + "-TEST000001");
        payment.setCreatedAt(LocalDateTime.of(2026, 3, 31, 10, 0));
        payment.setUpdatedAt(LocalDateTime.of(2026, 3, 31, 10, 5));
        return payment;
    }
}
