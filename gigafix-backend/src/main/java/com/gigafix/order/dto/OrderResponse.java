package com.gigafix.order.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long orderId; // 訂單 ID
    private Long memberId; // 會員 ID
    private Integer totalAmount; // 訂單總金額
    private List<OrderItemResponse> orderItems; // 訂單商品列表
    private String orderStatus; // 訂單狀態
    private String paymentMethod; // 付款方式
    private String paymentStatus; // 付款狀態
    private String receiverName; // 收件人姓名
    private String receiverPhone; // 收件人電話
    private String receiverAddress; // 收件人地址
    private String shippingMethod; // 物流方式
    private String trackingNumber; // 物流追蹤號碼
    private String shippingStatus; // 物流狀態
    private String customerRemark; // 客戶備註
    private LocalDateTime createdAt; // 訂單建立時間
    private String transactionId; // 金流交易編號
    private LocalDateTime paidAt; // 訂單付款時間
    private LocalDateTime shippedAt; // 訂單出貨時間
    private LocalDateTime deliveredAt; // 訂單送達時間
   
}
