package com.gigafix.admin.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gigafix.admin.entity.AdminAccount;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class AdminUserDetails implements UserDetails { //不想繼承AdminAccount，怕有人拿去用JPA操作
	private final AdminAccount adminAccount;
	
	//把AdminAccount常用(id,name)或要包很多層(role name)的屬性抓出來方便呼叫
	public Integer getId() {
        return adminAccount.getAdminAccountId();
    }
	public String getName() {
        return adminAccount.getAdminAccountName();
    }
	public String getRoleName() {
        return adminAccount.getAdminRole().getRoleName().name();
    }
	public LocalDateTime getCreatedTime() {
        return adminAccount.getAdminAccountCreateTime();
    }

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(adminAccount.getAdminRole().getRoleName().name()));
	}

	@Override
	public String getPassword() {
		return adminAccount.getAdminAccountPassword();
	}

	@Override
	public String getUsername() {
		return adminAccount.getAdminAccountName();
	}

	@Override
	public boolean isAccountNonExpired() {return true;}

	@Override
	public boolean isAccountNonLocked() {return true;}

	@Override
	public boolean isCredentialsNonExpired() {return true;}

	@Override
	public boolean isEnabled() {return true;}

}
