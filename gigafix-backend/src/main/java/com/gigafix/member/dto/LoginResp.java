package com.gigafix.member.dto;

import lombok.Builder;

@Builder //record自帶全參數建構式，所以可以直接使用builder
public record LoginResp(
		String email,
		String nickName
		) {}
