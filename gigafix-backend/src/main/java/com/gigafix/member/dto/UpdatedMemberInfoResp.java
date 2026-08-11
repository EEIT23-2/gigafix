package com.gigafix.member.dto;

import com.gigafix.member.entity.Member.Gender;

import lombok.Builder;
@Builder
public record UpdatedMemberInfoResp( //更改後回傳給使用者的資訊，會跟查詢的資訊一樣
		String realName,
		String nickName,
		String email,
		String phone,
		String address,
		Gender gender
		) {}
