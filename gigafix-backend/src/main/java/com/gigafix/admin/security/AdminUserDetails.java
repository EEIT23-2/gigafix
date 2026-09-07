package com.gigafix.admin.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gigafix.admin.entity.AdminAccount;
import com.gigafix.admin.entity.AdminAccount.Role;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class AdminUserDetails implements UserDetails {
	/*
	 * 不想繼承AdminAccount，因為如果繼承了
	 * 一、別人可以直接改Authication的內容(改成另一個admin，唯一的欄位屬性，final沒必要給他setter)
	 * 二、來這樣這個class因為繼承了entity，所以可以直接變成一個資料庫table，Hibernate自動建表會建立這個東西(但登入的資僅限當次查詢，
	 * 不需要持久化！該持久化的都存在Admin的table了)
	 * 三、這個class本來就不是entity，不該繼承entity使其具備entity功能
	 */
	private final AdminAccount adminAccount;

	// 把AdminAccount常用(id,name)或要包很多層(role name)的屬性抓出來方便呼叫
	public Integer getId() {
		return adminAccount.getId();
	}

	public String getName() {
		return adminAccount.getName();
	}

	public Role getRole() {
		return adminAccount.getRole();
	}

	public LocalDateTime getCreatedTime() {
		return adminAccount.getCreateTime();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(adminAccount.getRole().name()));
	}

	@Override
	public String getPassword() {
		return adminAccount.getPassword();
	}

	@Override
	public String getUsername() {
		return adminAccount.getName();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	// 用帳號 id判斷兩個物件相等
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof AdminUserDetails other))
			return false;
		return this.getId() != null && this.getId().equals(other.getId());
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}
}
