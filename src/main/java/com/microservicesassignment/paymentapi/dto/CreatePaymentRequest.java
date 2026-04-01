package com.microservicesassignment.paymentapi.dto;

import com.microservicesassignment.paymentapi.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class CreatePaymentRequest {

    @NotNull(message = "salesOrderId is required")
    @Positive(message = "salesOrderId must be positive")
    private Long salesOrderId;

    @NotNull(message = "customerId is required")
    @Positive(message = "customerId must be positive")
    private Long customerId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "amount must be zero or positive")
    private BigDecimal amount;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    public Long getSalesOrderId() {
        return salesOrderId;
    }

    public void setSalesOrderId(Long salesOrderId) {
        this.salesOrderId = salesOrderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

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
}
