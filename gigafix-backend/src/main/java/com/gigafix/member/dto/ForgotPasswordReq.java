package com.gigafix.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForgotPasswordReq(
		@NotBlank(message = "Email不可為空")
		@Email(message = "Email格式錯誤")
		@Pattern(regexp = "^\\S+$", message = "Email不可包含空格")
		String email,
		@NotBlank(message = "password不可為空")
		@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)\\S{8,}$", message = "密碼需至少8碼，並包含大小寫英文字母及數字，且不可包含空格")
		String newPassword,
		@NotBlank(message = "OTP驗證碼不可為空")
		@Pattern(regexp = "^\\d{6}$", message = "OTP驗證碼格式錯誤")
		String otp) {}