package com.gigafix.member.dto;

import com.gigafix.member.entity.Member.Gender;

import lombok.Builder;
@Builder
public record GetMemberInfoResp(
		String realName,
		String nickName,
		String email,
		String phone,
		String address,
		Gender gender) {}
