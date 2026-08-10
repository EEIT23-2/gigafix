package com.gigafix.member.dto;

import java.time.LocalDateTime;

import com.gigafix.member.entity.Member.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


public record RegisterReq(
		@NotBlank(message = "password不可為空")
		String password,
		@NotBlank(message = "真實姓名不可為空")
		String realName,
		@NotBlank(message = "暱稱不可為空")
		String nickName, 
		@NotBlank(message = "Email不可為空")
	    @Email(message = "Email格式錯誤")
		String email,
		@NotBlank(message = "手機號碼不可為空")
		@Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式錯誤，需為09開頭的10碼數字")
		String phone,
		@NotBlank(message = "地址不可為空")
		String address,
		@NotNull(message = "性別不可為空")
		Gender gender,
		//在controller才創建這個帳號創立時間，所以不去檢查，因為這個物件在controller跟service共用
		LocalDateTime createDateTime) {}
