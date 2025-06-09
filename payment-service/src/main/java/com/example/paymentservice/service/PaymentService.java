package com.example.paymentservice.service;

import com.example.commonlib.event.OrderCreatedEvent;
import com.example.commonlib.event.PaymentProcessedEvent;
import com.example.paymentservice.dto.CreatePaymentRequest;
import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.dto.RefundRequest;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.kafka.producer.PaymentEventProducer;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;

    private static final Set<PaymentStatus> CANCELLABLE_STATUSES = Set.of(
            PaymentStatus.SUCCESS,
            PaymentStatus.PENDING,
            PaymentStatus.PROCESSING
    );

    private static final Set<PaymentStatus> REFUNDABLE_STATUSES = Set.of(
            PaymentStatus.CANCELLED
    );

    private Long getCurrentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Long.parseLong(jwt.getSubject());
    }

    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        try {
            Payment payment = Payment.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(event.getTotalAmount())
                    .status(PaymentStatus.PROCESSING)
                    .build();
            
            payment = paymentRepository.save(payment);
            log.info("Payment processing started for order ID: {}", event.getOrderId());

            // 결제 처리 로직
            boolean isPaymentSuccessful = processPaymentWithExternalSystem(payment);
            
            if (isPaymentSuccessful) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment = paymentRepository.save(payment);
                log.info("Payment processed successfully for order ID: {}", event.getOrderId());

                // 성공 이벤트 발행
                PaymentProcessedEvent successEvent = new PaymentProcessedEvent(
                        event.getOrderId(),
                        payment.getId(),
                        payment.getAmount(),
                        "SUCCESS",
                        null
                );
                paymentEventProducer.sendPaymentProcessedEvent(successEvent);
            } else {
                throw new PaymentException("Payment processing failed");
            }

        } catch (Exception e) {
            log.error("Payment processing failed for order ID: {}", event.getOrderId(), e);
            
            // 실패 이벤트 발행
            PaymentProcessedEvent failedEvent = new PaymentProcessedEvent(
                    event.getOrderId(),
                    null,
                    event.getTotalAmount(),
                    "FAILED",
                    e.getMessage()
            );
            paymentEventProducer.sendPaymentProcessedEvent(failedEvent);
            
            throw new PaymentException("Payment processing failed", e);
        }
    }

    @Transactional
    public PaymentDto cancelPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));

        // 권한 검증
        Long currentUserId = getCurrentUserId();
        if (!payment.getUserId().equals(currentUserId)) {
            throw new PaymentException("Access denied to payment: " + paymentId);
        }

        // 상태 검증
        if (!CANCELLABLE_STATUSES.contains(payment.getStatus())) {
            throw new PaymentException("Payment cannot be cancelled. Current status: " + payment.getStatus());
        }

        try {
            // 외부 결제 시스템에 취소 요청
            boolean isCancellationSuccessful = cancelPaymentWithExternalSystem(payment);
            
            if (isCancellationSuccessful) {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCancellationReason(reason);
                payment.setCancelledAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);
                
                // 취소 이벤트 발행
                PaymentProcessedEvent cancelEvent = new PaymentProcessedEvent(
                        payment.getOrderId(),
                        payment.getId(),
                        payment.getAmount(),
                        "CANCELLED",
                        reason
                );
                paymentEventProducer.sendPaymentProcessedEvent(cancelEvent);
                
                log.info("Payment cancelled successfully - paymentId: {}, reason: {}", paymentId, reason);
                return convertToDto(payment);
            } else {
                throw new PaymentException("Payment cancellation failed with external system");
            }
        } catch (Exception e) {
            log.error("Failed to cancel payment: {}", paymentId, e);
            throw new PaymentException("Payment cancellation failed", e);
        }
    }

    @Transactional
    public PaymentDto initiateRefund(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));

        // 권한 검증
        Long currentUserId = getCurrentUserId();
        if (!payment.getUserId().equals(currentUserId)) {
            throw new PaymentException("Access denied to payment: " + paymentId);
        }

        // 상태 검증
        if (!REFUNDABLE_STATUSES.contains(payment.getStatus())) {
            throw new PaymentException("Payment cannot be refunded. Current status: " + payment.getStatus());
        }

        try {
            // 환불 처리 시작
            payment.setStatus(PaymentStatus.REFUND_PENDING);
            payment.setRefundReason(request.getReason());
            payment.setRefundRequestedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            // 환불 이벤트 발행
            PaymentProcessedEvent refundEvent = new PaymentProcessedEvent(
                    payment.getOrderId(),
                    payment.getId(),
                    payment.getAmount(),
                    "REFUND_PENDING",
                    request.getReason()
            );
            paymentEventProducer.sendPaymentProcessedEvent(refundEvent);

            log.info("Refund initiated for payment: {}, reason: {}", paymentId, request.getReason());
            return convertToDto(payment);
        } catch (Exception e) {
            log.error("Failed to initiate refund for payment: {}", paymentId, e);
            throw new PaymentException("Failed to initiate refund", e);
        }
    }

    @Transactional
    public PaymentDto processRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new PaymentException("Payment is not in REFUND_PENDING status: " + paymentId);
        }

        try {
            // 외부 시스템에 환불 요청
            boolean isRefundSuccessful = processRefundWithExternalSystem(payment);

            if (isRefundSuccessful) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);

                // 환불 완료 이벤트 발행
                PaymentProcessedEvent refundedEvent = new PaymentProcessedEvent(
                        payment.getOrderId(),
                        payment.getId(),
                        payment.getAmount(),
                        "REFUNDED",
                        payment.getRefundReason()
                );
                paymentEventProducer.sendPaymentProcessedEvent(refundedEvent);

                log.info("Refund processed successfully for payment: {}", paymentId);
                return convertToDto(payment);
            } else {
                throw new PaymentException("Refund processing failed with external system");
            }
        } catch (Exception e) {
            log.error("Failed to process refund for payment: {}", paymentId, e);
            throw new PaymentException("Failed to process refund", e);
        }
    }

    // 외부 결제 시스템과의 통신을 시뮬레이션하는 메서드
    private boolean processPaymentWithExternalSystem(Payment payment) {
        // 실제 구현에서는 외부 결제 시스템과 통신
        try {
            Thread.sleep(1000); // 결제 처리 시뮬레이션
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // 외부 결제 시스템과의 취소 통신을 시뮬레이션하는 메서드
    private boolean cancelPaymentWithExternalSystem(Payment payment) {
        // 실제 구현에서는 외부 결제 시스템과 통신
        try {
            Thread.sleep(1000); // 취소 처리 시뮬레이션
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean processRefundWithExternalSystem(Payment payment) {
        // 실제 구현에서는 외부 결제 시스템과 통신하여 환불 처리
        try {
            Thread.sleep(1000); // 환불 처리 시뮬레이션
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));
        
        // Check if the current user has access to this payment
        Long currentUserId = getCurrentUserId();
        if (!payment.getUserId().equals(currentUserId)) {
            throw new PaymentException("Access denied to payment: " + paymentId);
        }
        
        return convertToDto(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getCurrentUserPayments(Pageable pageable) {
        Long userId = getCurrentUserId();
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<PaymentDto> getCurrentUserPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        Long userId = getCurrentUserId();
        return paymentRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Payment not found for order: " + orderId));
        
        // Check if the current user has access to this payment
        Long currentUserId = getCurrentUserId();
        if (!payment.getUserId().equals(currentUserId)) {
            throw new PaymentException("Access denied to payment for order: " + orderId);
        }
        
        return convertToDto(payment);
    }

    private PaymentDto convertToDto(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .cancelledAt(payment.getCancelledAt())
                .cancellationReason(payment.getCancellationReason())
                .refundRequestedAt(payment.getRefundRequestedAt())
                .refundedAt(payment.getRefundedAt())
                .refundReason(payment.getRefundReason())
                .build();
    }
} 