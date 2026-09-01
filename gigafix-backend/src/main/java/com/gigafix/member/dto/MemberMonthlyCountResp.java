package com.gigafix.member.dto;

import lombok.Builder;

@Builder
public record MemberMonthlyCountResp( //給後台「每月會員註冊數量」曲線圖用
		String month, //格式yyyy-MM，例如"2026-08"
		Long count) {} //當月新增註冊的會員數
