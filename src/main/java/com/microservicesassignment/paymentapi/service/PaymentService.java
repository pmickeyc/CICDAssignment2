package com.microservicesassignment.paymentapi.service;

import com.microservicesassignment.paymentapi.dto.CreatePaymentRequest;
import com.microservicesassignment.paymentapi.dto.PaymentResponse;
import com.microservicesassignment.paymentapi.dto.UpdatePaymentRequest;
import com.microservicesassignment.paymentapi.entity.Payment;
import com.microservicesassignment.paymentapi.entity.PaymentMethod;
import com.microservicesassignment.paymentapi.entity.PaymentStatus;
import com.microservicesassignment.paymentapi.exception.ConflictException;
import com.microservicesassignment.paymentapi.exception.InvalidRequestException;
import com.microservicesassignment.paymentapi.exception.ResourceNotFoundException;
import com.microservicesassignment.paymentapi.mapper.PaymentMapper;
import com.microservicesassignment.paymentapi.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(PaymentRepository paymentRepository, PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(Long customerId,
                                             PaymentStatus status,
                                             PaymentMethod paymentMethod,
                                             Pageable pageable) {
        Specification<Payment> specification = buildPaymentSpecification(customerId, status, paymentMethod);
        Page<Payment> payments = paymentRepository.findAll(specification, pageable);
        return payments.map(paymentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        return paymentMapper.toResponse(findPayment(paymentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentBySalesOrderId(Long salesOrderId) {
        Payment payment = paymentRepository.findBySalesOrderId(salesOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("payment not found for salesOrderId=" + salesOrderId));
        return paymentMapper.toResponse(payment);
    }

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        validateAmount(request.getAmount());

        if (paymentRepository.existsBySalesOrderId(request.getSalesOrderId())) {
            throw new ConflictException("payment already exists for salesOrderId=" + request.getSalesOrderId());
        }

        Payment payment = new Payment();
        payment.setSalesOrderId(request.getSalesOrderId());
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionReference(generateTransactionReference(request.getSalesOrderId()));

        Payment saved = paymentRepository.save(payment);
        return paymentMapper.toResponse(saved);
    }

    public PaymentResponse updatePayment(Long paymentId, UpdatePaymentRequest request) {
        validateAmount(request.getAmount());

        Payment payment = findPayment(paymentId);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(request.getStatus());

        Payment updated = paymentRepository.save(payment);
        return paymentMapper.toResponse(updated);
    }

    public void deletePayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        validateDeleteAllowed(payment);
        paymentRepository.delete(payment);
    }

    @Transactional(readOnly = true)
    public Payment findPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("payment not found for paymentId=" + paymentId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount != null && amount.signum() < 0) {
            throw new InvalidRequestException("amount must be zero or positive");
        }
    }

    private void validateDeleteAllowed(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.FAILED) {
            throw new InvalidRequestException("only PENDING or FAILED payments can be deleted");
        }
    }

    private String generateTransactionReference(Long salesOrderId) {
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "TXN-" + salesOrderId + "-" + randomPart;
    }

    private Specification<Payment> buildPaymentSpecification(Long customerId,
                                                             PaymentStatus status,
                                                             PaymentMethod paymentMethod) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
