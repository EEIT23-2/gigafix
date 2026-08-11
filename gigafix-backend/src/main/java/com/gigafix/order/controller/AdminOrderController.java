package com.gigafix.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import com.gigafix.order.dto.OrderResponse;
import com.gigafix.order.dto.ShipOrderRequest;
import com.gigafix.order.dto.UpdateOrderRequest;
import com.gigafix.order.service.OrderService;
import com.gigafix.order.dto.AdminCreateOrderRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    // 管理員指定會員與商品建立訂單
    @PostMapping
    public ResponseEntity<OrderResponse> createOrderByAdmin(
            @Valid @RequestBody AdminCreateOrderRequest request) {

        OrderResponse response = orderService.createOrderByAdmin(request);

        return ResponseEntity.ok(response);

    }

    // 查詢所有會員訂單
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {

        List<OrderResponse> responses = orderService.getAllOrders();

        return ResponseEntity.ok(responses);
    }

    // 依會員 ID 查詢該會員所有訂單
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByMemberId(
            @PathVariable Long memberId) {

        List<OrderResponse> responses = orderService.getOrdersByMemberId(memberId);

        return ResponseEntity.ok(responses);
    }

    // 管理員查詢指定訂單
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getAdminOrder(
            @PathVariable Long orderId) {

        OrderResponse response = orderService.getAdminOrder(orderId);

        return ResponseEntity.ok(response);
    }

    // 修改未出貨訂單資訊
    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> updateOrderInfo(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderRequest request) {

        OrderResponse response = orderService.updateOrderInfo(orderId, request);

        return ResponseEntity.ok(response);
    }

    // 訂單出貨
    @PostMapping("/{orderId}/ship")
    public ResponseEntity<OrderResponse> shipOrder(

            @PathVariable Long orderId,
            @Valid @RequestBody ShipOrderRequest request) {

        OrderResponse response = orderService.shipOrder(orderId, request);

        return ResponseEntity.ok(response);
    }

    // 訂單送達
    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable Long orderId) {

        OrderResponse response = orderService.deliverOrder(orderId);

        return ResponseEntity.ok(response);
    }

    // 刪除符合條件的訂單
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long orderId) {

        orderService.deleteOrder(orderId);

        return ResponseEntity.noContent().build();
    }
}
