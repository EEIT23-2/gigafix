package com.gigafix.member.dto;

import com.gigafix.member.entity.Member.Gender;

import lombok.Builder;

@Builder //record自帶全參數建構式，所以可以直接使用builder
public record LoginResp(
		String realName,
		String nickName,
		String email,
		String phone,
		String address,
		Gender gender,
		String token
							) {}
