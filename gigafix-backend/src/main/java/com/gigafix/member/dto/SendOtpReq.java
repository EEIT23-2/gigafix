package com.gigafix.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendOtpReq(
		@NotBlank(message = "Email不可為空")
		@Email(message = "Email格式錯誤")
		String email) {}
