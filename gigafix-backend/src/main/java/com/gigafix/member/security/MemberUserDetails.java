package com.gigafix.member.security;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gigafix.admin.security.AdminUserDetails;
import com.gigafix.member.entity.Member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class MemberUserDetails implements UserDetails {

    private final Member member;

    public Long getId() {
        return member.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();// 前台使用者並沒有設定特別權限(VIP系統)
    }

    @Override
    public @Nullable String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getNickName();
    }

    // 用member id判斷兩個物件相等
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
