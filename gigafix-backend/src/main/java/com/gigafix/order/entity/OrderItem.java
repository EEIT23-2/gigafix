package com.gigafix.order.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自動生成主鍵
    @Column(name = "order_item_id")
    private Long orderItemId; // 訂單項目ID
    @Column(name = "order_id", nullable = false)
    private Long orderId; // 訂單ID
    @Column(name = "product_id", nullable = false)
    private Long productId; // 商品ID
    @Column(name = "product_name", nullable = false)
    private String productName; // 商品名稱快照
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice; // 成交價格快照
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 建立時間

    @PrePersist //訂單只會建立一次 不會修改 所以只需要在建立時設定時間
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;

    }

}
