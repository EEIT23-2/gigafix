package com.gigafix.shipment.entity;

import java.time.LocalDateTime;

import com.gigafix.order.entity.Order;
import com.gigafix.shipment.enums.ShippingMethod;
import com.gigafix.shipment.enums.ShippingStatus;

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
@Table(name = "shipment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
/** 保存訂單的單筆物流紀錄與建立當下的收件資料快照。 */
public class Shipment {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "shipment_id")
	private Long shipmentId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	// 最終仍需由正式資料庫的 UNIQUE(order_id) 防止併發重複建立。
	private Order order;

	// 以下收件欄位是建立物流紀錄時的快照，不受會員資料後續變更影響。
	@Column(name = "receiver_name", nullable = false, length = 50)
	private String receiverName;
	@Column(name = "receiver_phone", nullable = false, length = 20)
	private String receiverPhone;
	@Column(name = "receiver_address", nullable = false, length = 255)
	private String receiverAddress;
	@Enumerated(EnumType.STRING)
	@Column(name = "shipping_method", nullable = false, length = 30)
	private ShippingMethod shippingMethod;
	@Column(name = "tracking_number", length = 100)
	private String trackingNumber;
	@Enumerated(EnumType.STRING)
	@Column(name = "shipping_status", nullable = false, length = 20)
	private ShippingStatus shippingStatus;
	@Column(name = "shipped_at")
	private LocalDateTime shippedAt;
	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist void prePersist() { LocalDateTime now=LocalDateTime.now(); if(createdAt==null)createdAt=now; if(updatedAt==null)updatedAt=now; }
	@PreUpdate void preUpdate() { updatedAt=LocalDateTime.now(); }
}
