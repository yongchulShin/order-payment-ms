package com.example.paymentservice.repository;

import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Payment> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status, Pageable pageable);
    Optional<Payment> findByOrderId(Long orderId);
} 