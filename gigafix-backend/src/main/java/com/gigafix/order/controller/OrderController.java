package com.gigafix.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.order.dto.CreateOrderRequest;
import com.gigafix.order.dto.OrderResponse;
import com.gigafix.order.dto.PaymentSuccessRequest;
import com.gigafix.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 訂單 Controller
 * 提供訂單相關 REST API
 */
@RestController
@RequestMapping("/api/gigafix/members/me/orders")
@RequiredArgsConstructor
public class OrderController {

    // 訂單 Service
    private final OrderService orderService;

    // 建立訂單
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestAttribute("memberId") Long memberId,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(memberId, request);

        return ResponseEntity.ok(response);
    }

    // 查詢會員所有訂單
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestAttribute("memberId") Long memberId) {

        List<OrderResponse> responses = orderService.getOrders(memberId);

        return ResponseEntity.ok(responses);
    }

    // 查詢會員指定訂單
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long orderId) {

        OrderResponse response = orderService.getOrder(memberId, orderId);

        return ResponseEntity.ok(response);
    }

    // 訂單付款成功
    @PostMapping("/{orderId}/payment")
    public ResponseEntity<OrderResponse> payOrder(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentSuccessRequest request) {

        OrderResponse response = orderService.payOrder(memberId, orderId, request);

        return ResponseEntity.ok(response);
    }

    // 取消訂單
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @RequestAttribute("memberId") Long memberId,
            @PathVariable Long orderId) {

        OrderResponse response = orderService.cancelOrder(memberId, orderId);

        return ResponseEntity.ok(response);
    }

}