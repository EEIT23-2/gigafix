package com.gigafix.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder //要使用bulid因為註冊會順便呼叫登入的方法，就必須要手動組裝這個dto
public record LoginReq(
		@NotBlank(message = "Email不可為空")
	    @Email(message = "Email格式錯誤")
		String email,
		@NotBlank(message = "password不可為空")
	    @Size(min = 8, message = "密碼長度不可低於8個字元")
		String password
		) {}
