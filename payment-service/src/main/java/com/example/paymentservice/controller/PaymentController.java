package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.dto.RefundRequest;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "결제 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "결제 정보 조회", description = "특정 결제의 상세 정보를 조회합니다.")
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPayment(
            @Parameter(description = "결제 ID") @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @Operation(summary = "주문별 결제 정보 조회", description = "주문 ID로 결제 정보를 조회합니다.")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto> getPaymentByOrderId(
            @Parameter(description = "주문 ID") @PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @Operation(summary = "사용자 결제 목록 조회", description = "현재 사용자의 전체 결제 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<PaymentDto>> getCurrentUserPayments(
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getCurrentUserPayments(pageable));
    }

    @Operation(summary = "상태별 결제 목록 조회", description = "현재 사용자의 결제 목록을 상태별로 조회합니다.")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PaymentDto>> getCurrentUserPaymentsByStatus(
            @Parameter(description = "결제 상태") @PathVariable PaymentStatus status,
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        return ResponseEntity.ok(paymentService.getCurrentUserPaymentsByStatus(status, pageable));
    }

    @Operation(summary = "결제 취소", description = "결제를 취소합니다.")
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentDto> cancelPayment(
            @Parameter(description = "결제 ID") @PathVariable Long paymentId,
            @Parameter(description = "취소 사유") @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId, reason));
    }

    @Operation(summary = "환불 요청", description = "결제에 대한 환불을 요청합니다.")
    @PostMapping("/{paymentId}/refund/initiate")
    public ResponseEntity<PaymentDto> initiateRefund(
            @Parameter(description = "결제 ID") @PathVariable Long paymentId,
            @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.initiateRefund(paymentId, request));
    }

    @Operation(summary = "환불 처리", description = "요청된 환불을 처리합니다.")
    @PostMapping("/{paymentId}/refund/process")
    public ResponseEntity<PaymentDto> processRefund(
            @Parameter(description = "결제 ID") @PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.processRefund(paymentId));
    }
} 