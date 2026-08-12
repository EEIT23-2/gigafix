package com.gigafix.member.dto;

import org.springframework.http.ResponseCookie;

import lombok.Builder;

@Builder
public record LoginResult(LoginResp loginResp, ResponseCookie responseCookie) {}
