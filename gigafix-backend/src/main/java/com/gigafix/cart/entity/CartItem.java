package com.gigafix.cart.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.gigafix.member.entity.Member;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor // JPA必須要有無參數建構子
@AllArgsConstructor // 自動生成有參數建構子
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId; //購物車明細

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "product_id", nullable = false)
    private Long productId;  //產品ID

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  //建立時間

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;  //更新時間

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
