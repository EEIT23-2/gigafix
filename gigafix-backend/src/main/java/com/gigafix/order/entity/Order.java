package com.gigafix.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id")
	private Long orderId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "order_date", nullable = false, updatable = false)
	private LocalDateTime orderDate;

	@Column(
			name = "total_amount",
			nullable = false,
			precision = 12,
			scale = 2
	)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

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
			nullable = false,
			precision = 10,
			scale = 2
	)
	private BigDecimal shippingFee;

	@Column(
			name = "discount_amount",
			nullable = false,
			precision = 10,
			scale = 2
	)
	private BigDecimal discountAmount;

	@Column(name = "remark", length = 255)
	private String remark;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

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
		if (shippingFee == null) {
			shippingFee = BigDecimal.ZERO;
		}
		if (discountAmount == null) {
			discountAmount = BigDecimal.ZERO;
		}
		if (totalAmount == null) {
			totalAmount = BigDecimal.ZERO;
		}
	}

	@PreUpdate
	private void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public enum OrderStatus {
		PENDING,
		PROCESSING,
		SHIPPED,
		COMPLETED,
		CANCELLED
	}

	public enum PaymentStatus {
		UNPAID,
		PAID,
		PAYMENT_FAILED,
		REFUNDED
	}
}
