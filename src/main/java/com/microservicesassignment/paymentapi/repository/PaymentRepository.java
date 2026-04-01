package com.microservicesassignment.paymentapi.repository;

import com.microservicesassignment.paymentapi.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findBySalesOrderId(Long salesOrderId);

    boolean existsBySalesOrderId(Long salesOrderId);
}
