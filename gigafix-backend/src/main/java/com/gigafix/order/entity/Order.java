package com.gigafix.order.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自動生成主鍵
    @Column(name = "order_id")
    private Long orderId; // 訂單ID
    @Column(name = "member_id", nullable = false)
    private Long memberId; // 會員ID

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount; // 訂單總金額

    @Column(name = "order_status", nullable = false)
    private String orderStatus; // 訂單狀態

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // 付款方式

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus; // 付款狀態

    @Column(name = "transaction_id")
    private String transactionId; // 交易編號

    @Column(name = "paid_at")
    private LocalDateTime paidAt; // 付款時間
    
    @Column(name = "receiver_name", nullable = false)
    private String receiverName; // 收件人姓名

    @Column(name = "receiver_phone", nullable = false)
    private String receiverPhone; // 收件人電話

    @Column(name = "receiver_address", nullable = false)
    private String receiverAddress; // 收件人地址

    @Column(name = "shipping_method", nullable = false)
    private String shippingMethod; // 運送方式

    @Column(name = "tracking_number")
    private String trackingNumber; // 物流追蹤號碼

    @Column(name = "shipping_status", nullable = false)
    private String shippingStatus; // 運送狀態

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt; // 出貨時間

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt; // 送達時間

    @Column(name = "customer_remark")
    private String customerRemark; // 客戶備註

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 建立時間

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 更新時間
    // 新增資料前自動執行 建立createdAt、updatedAt
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    // 更新資料前自動執行 更新updatedAt
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
