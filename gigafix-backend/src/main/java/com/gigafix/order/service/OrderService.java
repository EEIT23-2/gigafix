package com.gigafix.order.service;

import java.util.List;

import com.gigafix.order.dto.PaymentSuccessRequest;
import com.gigafix.order.dto.CreateOrderRequest;
import com.gigafix.order.dto.OrderResponse;
import com.gigafix.order.dto.ShipOrderRequest;

/**
 * 訂單 Service
 * 定義訂單相關商業功能
 */
public interface OrderService {

        // 建立訂單（購物車結帳）
        OrderResponse createOrder(
                        Long memberId,
                        CreateOrderRequest request);

        // 查詢會員的所有訂單
        List<OrderResponse> getOrders(Long memberId);

        // 查詢會員的指定訂單
        OrderResponse getOrder(
                        Long memberId,
                        Long orderId);

        // 處理訂單付款成功
        OrderResponse payOrder(
                        Long memberId,
                        Long orderId,
                        PaymentSuccessRequest request);

        // 取消訂單
        OrderResponse cancelOrder(
                        Long memberId,
                        Long orderId);
        // 出貨訂單
        OrderResponse shipOrder(
                        Long memberId,
                        Long orderId,
                        ShipOrderRequest request);
        //訂單送達
        OrderResponse deliverOrder(
                        Long memberId,
                        Long orderId);
}