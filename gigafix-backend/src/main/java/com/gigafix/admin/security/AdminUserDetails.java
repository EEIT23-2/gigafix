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
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class AdminUserDetails implements UserDetails { //不想繼承AdminAccount，怕有人拿去用JPA操作
	private final AdminAccount adminAccount;
	
	//把AdminAccount常用(id,name)或要包很多層(role name)的屬性抓出來方便呼叫
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
	public boolean isAccountNonExpired() {return true;}

	@Override
	public boolean isAccountNonLocked() {return true;}

	@Override
	public boolean isCredentialsNonExpired() {return true;}

	@Override
	public boolean isEnabled() {return true;}

}
