package com.gigafix.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 訂單商品明細 Entity，對應 {@code order_items} 資料表。
 * 每筆資料隸屬一個 Order，保存結帳當下的商品資訊，供 Order Service 組成訂單回應 DTO。
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

	/** 訂單項目主鍵。 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_item_id")
	private Long orderItemId;

	/** 此項目所屬的訂單，多筆項目可關聯同一個 Order。 */
	@ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "product_name", nullable = false, length = 100)
	private String productName;

	@Column(
			name = "unit_price",
			precision = 10,
			scale = 2,
			nullable = false
	)
	private BigDecimal unitPrice;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(
			name = "subtotal",
			precision = 12,
			scale = 2,
			nullable = false
	)
	private BigDecimal subtotal;

	/** 訂單項目建立時間，首次寫入後不再更新。 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 訂單項目最近更新時間。 */
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	private void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (subtotal == null && unitPrice != null && quantity != null) {
			subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
		}
	}

	@PreUpdate
	private void preUpdate() {
		updatedAt = LocalDateTime.now();
		if (unitPrice != null && quantity != null) {
			subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
		}
	}
}
