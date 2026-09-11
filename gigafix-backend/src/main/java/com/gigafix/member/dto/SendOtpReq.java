package com.gigafix.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendOtpReq(
		@NotBlank(message = "Email不可為空")
		@Email(message = "Email格式錯誤")
		String email,
		String captchaToken) {} //只有註冊那支endpoint會驗證這個欄位，忘記密碼共用同一個DTO但不會用到，所以不加@NotBlank
