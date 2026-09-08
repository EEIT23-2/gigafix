package com.gigafix.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginReq(
        @NotBlank(message = "Google登入驗證失敗") String idToken) {
}