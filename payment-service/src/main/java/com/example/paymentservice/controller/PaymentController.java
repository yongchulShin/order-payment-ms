package com.example.paymentservice.controller;

import com.example.paymentservice.dto.PaymentDto;
import com.example.paymentservice.dto.RefundRequest;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPayment(paymentId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentDto> getPaymentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentDto>> getCurrentUserPayments(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getCurrentUserPayments(pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PaymentDto>> getCurrentUserPaymentsByStatus(
            @PathVariable PaymentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(paymentService.getCurrentUserPaymentsByStatus(status, pageable));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentDto> cancelPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId, reason));
    }

    @PostMapping("/{paymentId}/refund/initiate")
    public ResponseEntity<PaymentDto> initiateRefund(
            @PathVariable Long paymentId,
            @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.initiateRefund(paymentId, request));
    }

    @PostMapping("/{paymentId}/refund/process")
    public ResponseEntity<PaymentDto> processRefund(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.processRefund(paymentId));
    }
} 