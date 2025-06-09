package com.example.paymentservice.kafka.producer;

import com.example.commonlib.event.PaymentProcessedEvent;
import com.example.commonlib.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentProcessedEvent(PaymentProcessedEvent event) {
        try {
            log.info("Sending payment processed event: {}", event);
            kafkaTemplate.send(KafkaTopics.PAYMENT_PROCESSED, event.getOrderId().toString(), event)
                    .addCallback(
                            success -> log.info("Payment event sent successfully for order ID: {}", event.getOrderId()),
                            failure -> log.error("Failed to send payment event for order ID: {}", event.getOrderId(), failure)
                    );
        } catch (Exception e) {
            log.error("Error while sending payment processed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send payment processed event", e);
        }
    }
} 