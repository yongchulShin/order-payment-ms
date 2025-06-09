package com.example.paymentservice.kafka.consumer;

import com.example.commonlib.event.OrderCreatedEvent;
import com.example.commonlib.event.PaymentProcessedEvent;
import com.example.commonlib.kafka.KafkaTopics;
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

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "${spring.application.name}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received order created event: {}", event);
        try {
            paymentService.processPayment(event);
        } catch (Exception e) {
            log.error("Failed to process payment for order {}: {}", event.getOrderId(), e.getMessage(), e);
        }
    }
} 