package com.gigafix.member.service;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gigafix.common.util.JwtUtils;
import com.gigafix.member.dto.CreateJwtDto;
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

    // 若認證成功則執行登入(發放JWT跟回傳DTO)
    public RegisterAndLoginResult login(Member member) {
        // 發放JWT，並將將id到JWT中發放給使用者
        String jwt = jwtUtils.createToken(CreateJwtDto.builder().subject(String.valueOf(member.getId())).build());
        // 在創建jwt時要把從資料庫撈出來的id轉成字串，避免前端的number型別太小，導致後端的long型別溢位
        ResponseCookie cookie = ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(true)
                .sameSite("None") // 允許跨網域帶cookie
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .build();
        LoginResp loginResp = LoginResp.builder()
                .email(member.getEmail())
                .nickName(member.getNickName())
                .build();
        return RegisterAndLoginResult.builder().loginResp(loginResp).responseCookie(cookie).build();
    }

}
