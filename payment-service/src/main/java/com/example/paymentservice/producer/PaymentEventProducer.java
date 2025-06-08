package com.example.paymentservice.producer;

import com.example.commonlib.event.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;
    private static final String PAYMENT_PROCESSED_TOPIC = "payment-processed";

    public void sendPaymentProcessedEvent(Long paymentId, Long orderId, BigDecimal amount, String status, String failureReason) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
            orderId,
            paymentId,
            amount,
            status,
            failureReason
        );
        
        kafkaTemplate.send(PAYMENT_PROCESSED_TOPIC, orderId.toString(), event);
        log.info("Payment processed event sent for orderId: {}, status: {}", orderId, status);
    }
} 