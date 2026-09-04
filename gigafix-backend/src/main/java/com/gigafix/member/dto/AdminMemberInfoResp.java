package com.gigafix.member.dto;

import java.time.LocalDateTime;

import com.gigafix.member.entity.Member.Gender;

import lombok.Builder;

@Builder
public record AdminMemberInfoResp( //後台查詢會員列表用，跟GetMemberInfoResp相比多了id讓後台可以指定操作對象，同樣不包含密碼
		Long id,
		String realName,
		String nickName,
		String email,
		String phone,
		String address,
		Gender gender,
		LocalDateTime createTime) {}
