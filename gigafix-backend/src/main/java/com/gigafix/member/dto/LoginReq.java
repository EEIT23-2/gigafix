package com.gigafix.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder //要使用bulid因為註冊會順便呼叫登入的方法，就必須要手動組裝這個dto
public record LoginReq(
		@NotBlank(message = "Email不可為空")
	    @Email(message = "Email格式錯誤")
		String email,
		@NotBlank(message = "password不可為空")
	    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼需至少8碼，並包含大小寫英文字母及數字")
		String password
		) {}
