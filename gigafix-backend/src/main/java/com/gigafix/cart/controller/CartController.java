package com.gigafix.cart.controller;

import com.gigafix.cart.dto.AddCartItemRequest;
import com.gigafix.cart.dto.CartItemResponse;
import com.gigafix.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 購物車 Controller
 * 提供購物車相關 REST API
 */
@RestController
@RequestMapping("/api/members/{memberId}/cart")
@RequiredArgsConstructor
public class CartController {

    // 購物車 Service
    private final CartService cartService;

    // 加入商品到購物車
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
            @PathVariable Long memberId,
            @Valid @RequestBody AddCartItemRequest request) {

        CartItemResponse response = cartService.addItem(memberId, request);

        return ResponseEntity.ok(response);
    }

    // 查詢會員購物車
    @GetMapping("/items")
    public ResponseEntity<List<CartItemResponse>> getCartItems(
            @PathVariable Long memberId) {

        List<CartItemResponse> responses = cartService.getCartItems(memberId);

        return ResponseEntity.ok(responses);
    }

    // 刪除購物車中的指定商品
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long memberId,
            @PathVariable Long cartItemId) {

        cartService.deleteItem(memberId, cartItemId);

        return ResponseEntity.noContent().build();
    }

    // 清空會員購物車
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(
            @PathVariable Long memberId) {

        cartService.clearCart(memberId);

        return ResponseEntity.noContent().build();
    }
}