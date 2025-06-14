package com.example.commonlib.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {
    private Long orderId;
    private Long paymentId;
    private BigDecimal amount;
    private String status;  // SUCCESS, FAILED
    private String failureReason;
} 