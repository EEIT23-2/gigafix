package com.gigafix.member.dto;

import com.gigafix.member.entity.Member.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateMemberReq( //後台修改會員資料用，前端要把該會員整包資料(密碼除外)傳回來
		@NotBlank(message = "真實姓名不可為空")
		@Size(max = 40, message = "真實姓名字數上限為40")
		String realName,
		@NotBlank(message = "暱稱不可為空")
		@Size(max = 40, message = "暱稱字數上限為40")
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
		Gender gender
		) {}
