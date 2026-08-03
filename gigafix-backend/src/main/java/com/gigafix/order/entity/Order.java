package com.gigafix.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.gigafix.member.entity.Member;
import com.gigafix.order.enums.OrderType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 訂單主檔 Entity，對應 {@code orders} 資料表。
 * 保存會員、訂單類型、金額與狀態，收件資料改由 Shipment 保存。
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

	/** 訂單主鍵。 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Long orderId;

	/** 訂單所屬會員，多筆訂單可關聯同一位會員。 */
	@ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** 區分一般商品訂單與維修訂單。 */
	@Enumerated(EnumType.STRING)
	@Column(name = "order_type", nullable = false, length = 20)
	private OrderType orderType;

	/** 訂單成立時間。 */
	@Column(name = "order_date", nullable = false, updatable = false)
	private LocalDateTime orderDate;

	@Column(
			name = "total_amount",
			precision = 12,
			scale = 2,
			nullable = false
	)
	private BigDecimal totalAmount;

	/** 訂單目前的處理進度。 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	/** 訂單目前的付款結果。 */
	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 20)
	private PaymentStatus paymentStatus;

	@Column(name = "remark", length = 500)
	private String remark;

	/** 訂單資料建立時間，首次寫入後不再更新。 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 訂單資料最近更新時間。 */
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	private void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (orderDate == null) {
			orderDate = now;
		}
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
		if (status == null) {
			status = OrderStatus.PENDING;
		}
		if (paymentStatus == null) {
			paymentStatus = PaymentStatus.UNPAID;
		}
	}

	@PreUpdate
	private void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	/**
	 * 訂單處理狀態，涵蓋待處理、處理中、已出貨、已完成與已取消。
	 */
	public enum OrderStatus {
		PENDING,
		PROCESSING,
		SHIPPED,
		COMPLETED,
		CANCELLED
	}

	/**
	 * 訂單付款狀態，涵蓋未付款、已付款、付款失敗與已退款。
	 */
	public enum PaymentStatus {
		UNPAID,
		PAID,
		PAYMENT_FAILED,
		REFUNDED
	}
}
