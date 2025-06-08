package com.example.orderservice.service;

import com.example.commonlib.event.OrderCreatedEvent;
import com.example.commonlib.event.PaymentProcessedEvent;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.dto.OrderItemDto;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderItem;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private static final String ORDER_CREATED_TOPIC = "order-created";

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .status(OrderStatus.CREATED)
                .shippingAddress(request.getShippingAddress())
                .orderItems(request.getItems().stream()
                        .map(item -> OrderItem.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        order = orderRepository.save(order);
        
        // Kafka로 이벤트 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount()
        );
        kafkaTemplate.send(ORDER_CREATED_TOPIC, event);
        
        return mapToDto(order);
    }

    @KafkaListener(topics = "payment-processed", groupId = "order-service")
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("Received PaymentProcessedEvent: {}", event);
        
        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + event.getOrderId()));

            // 결제 상태에 따라 주문 상태 업데이트
            if ("COMPLETED".equals(event.getStatus())) {
                order.updateStatus(OrderStatus.PAID);
                log.info("Order {} status updated to PAID", order.getId());
            } else if ("FAILED".equals(event.getStatus())) {
                order.updateStatus(OrderStatus.PAYMENT_FAILED);
                log.info("Order {} status updated to PAYMENT_FAILED", order.getId());
            }

            orderRepository.save(order);
        } catch (Exception e) {
            log.error("Failed to process payment event for order: {}", event.getOrderId(), e);
        }
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(String orderId) {
        Order order = orderRepository.findById(Long.parseLong(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        return mapToDto(order);
    }

    private OrderDto mapToDto(Order order) {
        return OrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .orderItems(order.getOrderItems().stream()
                        .map(item -> OrderItemDto.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
} 