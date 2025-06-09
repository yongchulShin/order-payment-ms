package com.example.paymentservice.model;

public enum PaymentStatus {
    PENDING,        // 결제 대기
    PROCESSING,     // 결제 처리 중
    SUCCESS,        // 결제 성공
    FAILED,         // 결제 실패
    CANCELLED,      // 결제 취소됨
    REFUND_PENDING, // 환불 대기
    REFUNDED        // 환불 완료
} 