package com.gigafix.member.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.member.dto.GoogleLoginReq;
import com.gigafix.member.dto.LoginResp;
import com.gigafix.member.dto.RegisterAndLoginResult;
import com.gigafix.member.service.GoogleAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gigafix")
public class GoogleAuthController {
    private final GoogleAuthService googleAuthService;

    @PostMapping("/login/google")
    public ResponseEntity<LoginResp> googleLogin(@RequestBody @Valid GoogleLoginReq req) throws Exception {
        // googleLogin()內會使用Google第三方入認證的函示庫，用前端傳來的使用者idtoken來替使用者登入(沒登入過就會註冊)
        RegisterAndLoginResult result = googleAuthService.googleLogin(req.idToken());

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                result.responseCookie().toString()).body(result.loginResp());
    }
}
