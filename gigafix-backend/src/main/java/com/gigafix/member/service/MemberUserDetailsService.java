package com.gigafix.member.service;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gigafix.common.util.JwtUtils;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.RegisterAndLoginResult;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;
import com.gigafix.member.security.MemberUserDetails;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {
    // 不使用spring security的form login，所以要自己寫controller 自己實作這個登入的service
    private final MemberRepository memberRepository;
    private final JwtUtils jwtUtils;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(""));
        // 先不加登入錯誤五次會鎖住
        return new MemberUserDetails(member);
    }

    // 給JWT filter用：token裡存的是memberId不是email，每次request都要重新用id查一次組出MemberUserDetails
    // (不像admin是session-based，組好一次就存進session可以重複利用，這裡沒有session能省略這一步)
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        Member member = memberRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException(""));
        return new MemberUserDetails(member);
    }

    // 若認證成功則執行登入(發放JWT跟回傳DTO)
    public RegisterAndLoginResult login(Member member) {
        // 發放JWT(cookie組裝統一交給jwtUtils處理)
        ResponseCookie cookie = jwtUtils.createTokenCookie(member.getId());
        LoginResp loginResp = LoginResp.builder()
                .email(member.getEmail())
                .nickName(member.getNickName())
                .build();
        return RegisterAndLoginResult.builder().loginResp(loginResp).responseCookie(cookie).build();
    }

}
