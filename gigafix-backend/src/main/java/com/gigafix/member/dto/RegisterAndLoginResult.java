package com.gigafix.member.dto;

import org.springframework.http.ResponseCookie;

import lombok.Builder;

@Builder
public record RegisterAndLoginResult(LoginResp loginResp, ResponseCookie responseCookie) {}
