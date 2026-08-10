package com.gigafix.cart.service;

import com.gigafix.cart.dto.AddCartItemRequest;
import com.gigafix.cart.dto.CartItemResponse;

import java.util.List;

/**
 * 購物車 Service
 * 定義購物車相關商業功能
 */
public interface CartService {

    // 新增商品到購物車
    CartItemResponse addItem(Long memberId, AddCartItemRequest request);

    // 查詢會員的購物車商品
    List<CartItemResponse> getCartItems(Long memberId);

    // 刪除購物車中的指定商品
    void deleteItem(Long memberId, Long cartItemId);

    // 清空會員購物車
    void clearCart(Long memberId);
}