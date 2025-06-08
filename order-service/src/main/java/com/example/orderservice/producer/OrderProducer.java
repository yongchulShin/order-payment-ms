package com.example.orderservice.producer;

import com.example.orderservice.dto.event.OrderCreatedEvent;
import com.example.orderservice.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private static final String ORDER_CREATED_TOPIC = "order-created";

    public void sendOrderCreatedEvent(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount()
        );
        
        kafkaTemplate.send(ORDER_CREATED_TOPIC, event);
        log.info("Order created event sent for orderId: {}", order.getId());
    }
} 