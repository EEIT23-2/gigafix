package com.gigafix.member.dto;

import com.gigafix.member.entity.Member.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberInfoReq( //以下是使用者可以改的欄位，前端不管有沒有改都要傳回來
		@NotBlank(message = "修改暱稱不可為空")
		@Size(max = 40, message = "暱稱字數上限為40")
		String nickName,
		@NotBlank(message = "修改手機號碼不可為空")
		@Pattern(regexp = "^09\\d{8}$", message = "手機號碼格式錯誤，需為09開頭的10碼數字")
		String phone,
		@NotBlank(message = "修改地址不可為空")
		String address,
		@NotNull(message = "修改性別不可為空")
		Gender gender
		) {}
