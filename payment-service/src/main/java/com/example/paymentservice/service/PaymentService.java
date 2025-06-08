package com.example.paymentservice.service;

import com.example.commonlib.event.OrderCreatedEvent;
import com.example.commonlib.event.PaymentProcessedEvent;
import com.example.paymentservice.dto.CreatePaymentRequest;
import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;
    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event);
        
        try {
            // 자동으로 결제 생성
            CreatePaymentRequest request = CreatePaymentRequest.builder()
                    .orderId(event.getOrderId())
                    .userId(event.getUserId())
                    .amount(new BigDecimal(event.getAmount()))
                    .paymentMethod("CREDIT_CARD") // 기본값
                    .build();

            PaymentDto payment = createPayment(request);
            
            // 결제 처리 결과 이벤트 발행
            PaymentProcessedEvent processedEvent = new PaymentProcessedEvent(
                payment.getOrderId(),
                payment.getId(),
                payment.getTransactionId(),
                payment.getStatus().toString(),
                payment.getAmount().doubleValue()
            );
            kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, processedEvent);
            
            log.info("Payment created and processed event published for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process payment for order: {}", event.getOrderId(), e);
            // 실패 이벤트 발행
            PaymentProcessedEvent failedEvent = new PaymentProcessedEvent(
                event.getOrderId(),
                null,
                null,
                PaymentStatus.FAILED.toString(),
                event.getAmount()
            );
            kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, failedEvent);
        }
    }

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .transactionId(UUID.randomUUID().toString())
                .build();

        payment = paymentRepository.save(payment);
        
        return PaymentDto.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
} 