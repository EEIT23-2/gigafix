package com.gigafix.member.dto;

import com.gigafix.member.entity.Member.Gender;

import lombok.Builder;

@Builder
public record MemberInfoResp( //查詢或修改後回傳給使用者的個人資訊，內容一樣所以合併成同一個DTO，不包含密碼
		String realName,
		String nickName,
		String email,
		String phone,
		String address,
		Gender gender,
		String profileImageUrl) {}
