package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "주문 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @Operation(summary = "주문 조회", description = "특정 주문의 상세 정보를 조회합니다.")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(
            @Parameter(description = "주문 ID") @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @Operation(summary = "사용자 주문 목록 조회", description = "현재 사용자의 전체 주문 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<OrderDto>> getCurrentUserOrders(
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        return ResponseEntity.ok(orderService.getCurrentUserOrders(pageable));
    }

    @Operation(summary = "상태별 주문 목록 조회", description = "현재 사용자의 주문 목록을 상태별로 조회합니다.")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<OrderDto>> getCurrentUserOrdersByStatus(
            @Parameter(description = "주문 상태") @PathVariable OrderStatus status,
            @Parameter(description = "페이지네이션 정보") Pageable pageable) {
        return ResponseEntity.ok(orderService.getCurrentUserOrdersByStatus(status, pageable));
    }
} 