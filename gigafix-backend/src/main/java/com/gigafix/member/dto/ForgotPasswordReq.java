package com.gigafix.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordReq(
		@NotBlank(message = "Email不可為空")
		@Email(message = "Email格式錯誤")
		String email,
		@NotBlank(message = "password不可為空")
		@Size(min = 8, message = "密碼長度不可低於8個字元")
		String newPassword,
		@NotBlank(message = "OTP驗證碼不可為空")
		@Pattern(regexp = "^\\d{6}$", message = "OTP驗證碼格式錯誤")
		String otp) {}