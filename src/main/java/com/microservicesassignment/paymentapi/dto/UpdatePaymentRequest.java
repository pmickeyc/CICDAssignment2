package com.microservicesassignment.paymentapi.dto;

import com.microservicesassignment.paymentapi.entity.PaymentMethod;
import com.microservicesassignment.paymentapi.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdatePaymentRequest {

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "amount must be zero or positive")
    private BigDecimal amount;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "status is required")
    private PaymentStatus status;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
