package com.gigafix.order.controller;

import java.util.List;

import com.gigafix.order.dto.PaymentSuccessRequest;
import com.gigafix.order.dto.ShipOrderRequest;
import com.gigafix.order.dto.CreateOrderRequest;
import com.gigafix.order.dto.OrderResponse;
import com.gigafix.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 訂單 Controller
 * 提供訂單相關 REST API
 */
@RestController
@RequestMapping("/api/members/{memberId}/orders")
@RequiredArgsConstructor
public class OrderController {

    // 訂單 Service
    private final OrderService orderService;

    // 建立訂單
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable Long memberId,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(memberId, request);

        return ResponseEntity.ok(response);
    }

    // 查詢會員所有訂單
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @PathVariable Long memberId) {

        List<OrderResponse> responses = orderService.getOrders(memberId);

        return ResponseEntity.ok(responses);
    }

    // 查詢會員指定訂單
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long memberId,
            @PathVariable Long orderId) {

        OrderResponse response = orderService.getOrder(memberId, orderId);

        return ResponseEntity.ok(response);
    }

    // 訂單付款成功
    @PostMapping("/{orderId}/payment")
    public ResponseEntity<OrderResponse> payOrder(
            @PathVariable Long memberId,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentSuccessRequest request) {

        OrderResponse response = orderService.payOrder(memberId, orderId, request);

        return ResponseEntity.ok(response);
    }

    // 取消訂單
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long memberId,
            @PathVariable Long orderId) {

        OrderResponse response = orderService.cancelOrder(memberId, orderId);

        return ResponseEntity.ok(response);
    }

    // 訂單出貨
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<OrderResponse> shipOrder(
            @PathVariable Long memberId,
            @PathVariable Long orderId,
            @Valid @RequestBody ShipOrderRequest request) {

        OrderResponse response = orderService.shipOrder(memberId, orderId, request);

        return ResponseEntity.ok(response);
    }

    // 訂單送達
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<OrderResponse> deliverOrder(
            @PathVariable Long memberId,
            @PathVariable Long orderId) {

        OrderResponse response = orderService.deliverOrder(memberId, orderId);

        return ResponseEntity.ok(response);
    }
}