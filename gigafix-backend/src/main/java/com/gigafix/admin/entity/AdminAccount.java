package com.gigafix.admin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin_accounts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminAccount {
	@Id @Column(name = "admin_account_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "admin_account_password", nullable = false)
	private String password;
	@Column(name = "admin_account_name", nullable = false, length = 20)
	private String name;
	@Enumerated(EnumType.STRING)//把Enum的值用字串名稱存到database，因為Enum的內容在底層是array
	@Column(name = "admin_role", nullable = false, length = 25)//雖然腳色寫死，但是長度設小一點每個欄位占用的記憶體空間也會小一點
	private Role role;
	@Column(name = "admin_account_created_time", nullable = false)
	private LocalDateTime createTime;
	
	public enum Role{
		ROLE_SUPER_ADMIN, //總管理員
		ROLE_DEPUTY_ADMIN, //副管理員
		ROLE_FORUM_ADMIN, //論壇管理員
		ROLE_ECOMMERCE_ADMIN, //訂單商品管理員
		ROLE_REPAIR_ADMIN //修繕管理員
	}
}
