package com.gigafix.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.gigafix.member.entity.Member;

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
 * 保存會員、收件、金額與狀態資料，並由 Order Repository 存取、Order Service 組成回應 DTO。
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

	@Column(name = "receiver_name", nullable = false, length = 50)
	private String receiverName;

	@Column(name = "receiver_phone", nullable = false, length = 20)
	private String receiverPhone;

	@Column(name = "shipping_address", nullable = false, length = 255)
	private String shippingAddress;

	@Column(
			name = "shipping_fee",
			precision = 10,
			scale = 2,
			nullable = false
	)
	private BigDecimal shippingFee;

	@Column(
			name = "discount_amount",
			precision = 10,
			scale = 2,
			nullable = false
	)
	private BigDecimal discountAmount;

	@Column(name = "remark", length = 255)
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
