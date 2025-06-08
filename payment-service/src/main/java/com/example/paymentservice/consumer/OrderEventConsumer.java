package com.example.paymentservice.consumer;

import com.example.commonlib.event.OrderCreatedEvent;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.producer.PaymentEventProducer;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {
    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    @KafkaListener(topics = "order-created", groupId = "${spring.application.name}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received order created event: {}", event);
        try {
            // 결제 처리
            Payment payment = paymentService.processPayment(event.getOrderId(), event.getTotalAmount());
            
            // 결제 결과 이벤트 발행
            paymentEventProducer.sendPaymentProcessedEvent(
                payment.getId(),
                event.getOrderId(),
                payment.getAmount(),
                payment.getStatus().name(),
                null
            );
        } catch (Exception e) {
            log.error("Failed to process payment for order {}: {}", event.getOrderId(), e.getMessage());
            // 실패 이벤트 발행
            paymentEventProducer.sendPaymentProcessedEvent(
                null,
                event.getOrderId(),
                event.getTotalAmount(),
                "FAILED",
                e.getMessage()
            );
        }
    }
} 