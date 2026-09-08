package com.gigafix.member.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gigafix.member.entity.Member;
import com.gigafix.member.entity.Member.Gender;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberUserDetails implements UserDetails {

    private final Member member;

    public Member getMember() {
        return member;
    }

    // 把Member常用的屬性抓出來方便呼叫，跟AdminUserDetails同樣的設計，密碼不額外開getter(UserDetails介面規定的getPassword()已經有了)
    public Long getId() {
        return member.getId();
    }

    public String getEmail() {
        return member.getEmail();
    }

    public String getRealName() {
        return member.getRealName();
    }

    public String getNickName() {
        return member.getNickName();
    }

    public String getPhone() {
        return member.getPhone();
    }

    public String getAddress() {
        return member.getAddress();
    }

    public Gender getGender() {
        return member.getGender();
    }

    public LocalDateTime getCreateTime() {
        return member.getCreateTime();
    }

    public String getProfileImageUrl() {
        return member.getProfileImageUrl();
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
        if (!(o instanceof MemberUserDetails other))
            return false;
        return this.getId() != null && this.getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

}
