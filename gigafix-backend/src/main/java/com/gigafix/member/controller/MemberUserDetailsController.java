package com.gigafix.member.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.member.dto.LoginReq;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.RegisterAndLoginResult;
import com.gigafix.member.entity.Member;
import com.gigafix.member.exception.InvalidCredentialsException;
import com.gigafix.member.security.MemberUserDetails;
import com.gigafix.member.service.MemberUserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gigafix")
public class MemberUserDetailsController {
    private final MemberUserDetailsService memberUserDetailsService;
    @Qualifier("memberAuthenticationManager")
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<LoginResp> memberLogin(@Valid @RequestBody LoginReq loginReq,
            HttpServletRequest req,
            HttpServletResponse resp) {
        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginReq.email(), loginReq.password()));
            // 使用AuthenticationManager調用登入的方法，做登入
        } catch (BadCredentialsException e) {// 如果登入失敗authenticate()會拋出這個例外
            throw new InvalidCredentialsException();
        }
        // 若認證成功則執行登入(發放JWT認證及回傳前端所需資料)
        Member member;
        if (authentication.getPrincipal() instanceof MemberUserDetails memberDetails) {
            member = memberDetails.getMember();
            RegisterAndLoginResult loginResult = memberUserDetailsService.login(member);
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                    loginResult.responseCookie().toString()).body(loginResult.loginResp());
            // 有參數的ok()回傳值是ResponseEntity<T>，不是BodyBuilder，所以無法用方法鏈串下去
        }
        throw new InvalidCredentialsException();
    }

}
