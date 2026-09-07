package com.gigafix.member.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.gigafix.member.entity.Member;

//後台會員管理列表的查詢條件，Controller直接把Query String綁定成這個物件(比照ProductQueryParams的作法)，避免方法簽章塞一堆參數
public record AdminMemberQueryParams(
		Integer page,
		Integer size,
		String keyword, //模糊比對真實姓名/暱稱/Email/手機
		Member.Gender gender,
		String city, //比對地址開頭的縣市
		//加入時間區間(起)；@DateTimeFormat(iso=DATE)指定只接受yyyy-MM-dd格式的字串轉成LocalDate
		//iso分成幾個模式，NONE(不套用ISO格式)、DATE(只有日期)、TIME(只有時間)、DATE_TIME(完整日期時間)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
		//加入時間區間(迄)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {}