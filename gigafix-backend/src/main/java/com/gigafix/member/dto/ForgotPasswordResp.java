package com.gigafix.member.dto;

import lombok.Builder;

@Builder
public record ForgotPasswordResp(String email) {}