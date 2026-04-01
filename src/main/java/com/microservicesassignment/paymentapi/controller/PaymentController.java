package com.microservicesassignment.paymentapi.controller;

import com.microservicesassignment.paymentapi.dto.CreatePaymentRequest;
import com.microservicesassignment.paymentapi.dto.PagedResponse;
import com.microservicesassignment.paymentapi.dto.PaymentResponse;
import com.microservicesassignment.paymentapi.dto.UpdatePaymentRequest;
import com.microservicesassignment.paymentapi.entity.PaymentMethod;
import com.microservicesassignment.paymentapi.entity.PaymentStatus;
import com.microservicesassignment.paymentapi.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/payments/v1/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PaymentResponse>> getPayments(
        @RequestParam(name = "customerId", required = false) @Positive Long customerId,
        @RequestParam(name = "status", required = false) PaymentStatus status,
        @RequestParam(name = "paymentMethod", required = false) PaymentMethod paymentMethod,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PaymentResponse> page = paymentService.getPayments(customerId, status, paymentMethod, pageable);
        return ResponseEntity.ok(PagedResponse.fromPage(page));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable @Positive Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/order/{salesOrderId}")
    public ResponseEntity<PaymentResponse> getPaymentBySalesOrderId(@PathVariable @Positive Long salesOrderId) {
        return ResponseEntity.ok(paymentService.getPaymentBySalesOrderId(salesOrderId));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse created = paymentService.createPayment(request);
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{paymentId}")
            .buildAndExpand(created.getId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> updatePayment(@PathVariable @Positive Long paymentId,
                                                         @Valid @RequestBody UpdatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.updatePayment(paymentId, request));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> deletePayment(@PathVariable @Positive Long paymentId) {
        paymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
