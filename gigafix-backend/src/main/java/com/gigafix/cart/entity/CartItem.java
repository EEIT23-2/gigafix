package com.gigafix.cart.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 購物車商品明細 Entity，對應 {@code cart_items} 資料表。
 * 每筆資料隸屬一個 Cart，並由 CartItem Repository 提供給 Cart Service 查詢與更新。
 */
@Entity
@Table(
		name = "cart_items",
		uniqueConstraints = {
				@UniqueConstraint(
						name = "uk_cart_items_cart_product",
						columnNames = {"cart_id", "product_id"}
				)
		}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

	/** 購物車項目主鍵。 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_item_id")
	private Long cartItemId;

	/** 此項目所屬的購物車，多筆項目可關聯同一個 Cart。 */
	@ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	/** 商品模組中的商品識別碼。 */
	@Column(name = "product_id", nullable = false)
	private Long productId;

	/** 此商品在購物車中的數量。 */
	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	/** 購物車項目建立時間，首次寫入後不再更新。 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 購物車項目最近更新時間。 */
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
	}

	@PreUpdate
	private void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
