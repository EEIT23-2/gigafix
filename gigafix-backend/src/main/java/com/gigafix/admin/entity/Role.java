package com.gigafix.admin.entity;


import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "roles")
@Getter
//不設定@Setter因為Role寫死不給新增，也不使用@buider因為不會new自訂義的role
@AllArgsConstructor
@NoArgsConstructor
public class Role {
	@Id
	@Column(name = "role_name")
	@Enumerated(EnumType.STRING)
	private RoleName roleName;
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "adminRole") //cascade = CascadeType.ALL不加，因為寫死的不會對role做CRUD，就不需要透過對方維護這張表
	private Set<AdminAccount> adminAccounts;
	
	//真的有必要做成一個class嗎?
	public enum RoleName{
		ROLE_SUPER_ADMIN, //總管理員
		ROLE_DEPUTY_ADMIN, //副管理員
		ROLE_FORUM_ADMIN, //論壇管理員
		ROLE_ECOMMERCE_ADMIN, //訂單商品管理員
		ROLE_REPAIR_ADMIN //修繕管理員
	}
}
