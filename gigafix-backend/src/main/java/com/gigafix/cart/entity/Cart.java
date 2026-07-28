package com.gigafix.cart.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.gigafix.user.entity.Member;

/**
 * 購物車主檔 Entity，對應 {@code carts} 資料表。
 * 保存會員目前或歷史購物車的狀態，並由 Cart Repository 存取、Cart Service 組成回應 DTO。
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

	/** 購物車主鍵。 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_id")
	private Long cartId;

	/** 購物車所屬會員，多筆購物車可關聯同一位會員。 */
	@ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** 並行更新版本號，供 JPA 偵測同一購物車被同時修改。 */
	@Version
	@Column(name = "version")
	private Long version;

	/** 購物車目前的生命週期狀態。 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private CartStatus status;

	/** 購物車建立時間，首次寫入後不再更新。 */
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/** 購物車最近更新時間。 */
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	private void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (status == null) {
			status = CartStatus.ACTIVE;
		}
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

	/**
	 * 購物車狀態：使用中、已結帳或已放棄。
	 */
	public enum CartStatus {
		ACTIVE,
		CHECKED_OUT,
		ABANDONED
	}
}
