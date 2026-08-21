package com.gigafix.order.service;

import java.util.List;

import com.gigafix.order.dto.AdminOrderCreateOptionsResponse;
import com.gigafix.order.dto.PaymentSuccessRequest;
import com.gigafix.order.dto.CreateOrderRequest;
import com.gigafix.order.dto.OrderResponse;
import com.gigafix.order.dto.ShipOrderRequest;
import com.gigafix.order.dto.UpdateOrderRequest;
import com.gigafix.order.dto.AdminCreateOrderRequest;

/**
 * 訂單 Service
 * 定義訂單相關商業功能
 */
public interface OrderService {

        // ---------------會員前台功能------------------

        // 建立訂單（購物車結帳）
        OrderResponse createOrder(
                        Long memberId,
                        CreateOrderRequest request);

        // 查詢會員自己的所有訂單
        List<OrderResponse> getOrders(Long memberId);

        // 查詢會員自己的指定訂單
        OrderResponse getOrder(
                        Long memberId,
                        Long orderId);

        // 會員訂單付款成功後，更新付款資訊與商品狀態
        OrderResponse payOrder(
                        Long memberId,
                        Long orderId,
                        PaymentSuccessRequest request);

        // 取消自己未付款訂單
        OrderResponse cancelOrder(
                        Long memberId,
                        Long orderId);

        // ---------------管理員後台功能------------------

        // 管理員依會員及商品建立訂單
        OrderResponse createOrderByAdmin(
                        AdminCreateOrderRequest request);

        // 查詢所有會員的訂單
        List<OrderResponse> getAllOrders();

        // 依會員 ID 查詢該會員所有訂單
        List<OrderResponse> getOrdersByMemberId(Long memberId);

        // 查詢指定訂單
        OrderResponse getAdminOrder(Long orderId);

        // 修改未出貨訂單資訊
        OrderResponse updateOrderInfo(
                        Long orderId,
                        UpdateOrderRequest request);

        // 出貨訂單
        OrderResponse shipOrder(
                        Long orderId,
                        ShipOrderRequest request);

        // 訂單送達
        OrderResponse deliverOrder(
                        Long orderId);

        // 刪除符合條件的訂單
        void deleteOrder(Long orderId);

        // 後台新增訂單頁：取得會員與可購買商品選項
        AdminOrderCreateOptionsResponse getAdminCreateOptions();

        // 管理員取消訂單
        OrderResponse adminCancelOrder(Long orderId);
}