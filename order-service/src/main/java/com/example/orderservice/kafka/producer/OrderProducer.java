package com.example.orderservice.kafka.producer;

import com.example.commonlib.event.OrderCreatedEvent;
import com.example.commonlib.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            log.info("Sending order created event: {}", event);
            kafkaTemplate.send(KafkaTopics.ORDER_CREATED, event)
                    .addCallback(
                            success -> log.info("Order event sent successfully for order ID: {}", event.getOrderId()),
                            failure -> log.error("Failed to send order event for order ID: {}", event.getOrderId(), failure)
                    );
        } catch (Exception e) {
            log.error("Error while sending order created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send order created event", e);
        }
    }
} 