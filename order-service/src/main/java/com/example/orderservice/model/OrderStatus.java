package com.example.orderservice.model;

public enum OrderStatus {
    CREATED,    // 주문이 처음 생성된 상태
    PENDING,    // 결제 대기 상태
    PAID,       // 결제 완료 상태
    COMPLETED,  // 주문 처리 완료 상태
    FAILED,     // 결제 실패 상태
    CANCELLED   // 주문 취소 상태
} 