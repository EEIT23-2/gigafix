package com.gigafix.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.gigafix.order.entity.Order;
import com.gigafix.payment.enums.PaymentMethod;
import com.gigafix.payment.enums.PaymentRecordStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
/**
 * 保存單筆付款紀錄，並以一對一關係對應訂單。
 */
public class Payment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payment_id")
	private Long paymentId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	// 最終仍需由正式資料庫的 UNIQUE(order_id) 防止併發重複建立。
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 30)
	private PaymentMethod paymentMethod;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", nullable = false, length = 20)
	private PaymentRecordStatus paymentStatus;

	@Column(name = "transaction_id", length = 100)
	private String transactionId;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	// 金額由訂單總額帶入，不接受前端直接指定。
	private BigDecimal amount;

	@Column(name = "paid_at")
	private LocalDateTime paidAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) createdAt = now;
		if (updatedAt == null) updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
