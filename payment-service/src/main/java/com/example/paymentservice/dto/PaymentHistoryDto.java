package com.example.paymentservice.dto;

import com.example.paymentservice.model.PaymentStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDto {
    private Long id;
    private Long paymentId;
    private PaymentStatus previousStatus;
    private PaymentStatus newStatus;
    private String description;
    private LocalDateTime createdAt;
} 