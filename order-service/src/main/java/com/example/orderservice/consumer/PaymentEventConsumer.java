package com.example.orderservice.consumer;

import com.example.orderservice.dto.event.PaymentProcessedEvent;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final OrderService orderService;

    @KafkaListener(topics = "payment-processed", groupId = "${spring.application.name}")
    public void handlePaymentProcessedEvent(PaymentProcessedEvent event) {
        log.info("Received payment processed event: {}", event);
        try {
            if ("SUCCESS".equals(event.getStatus())) {
                orderService.completeOrder(event.getOrderId(), event.getPaymentId());
            } else {
                orderService.failOrder(event.getOrderId(), event.getFailureReason());
            }
        } catch (Exception e) {
            log.error("Failed to process payment event for order {}: {}", event.getOrderId(), e.getMessage());
        }
    }
} 