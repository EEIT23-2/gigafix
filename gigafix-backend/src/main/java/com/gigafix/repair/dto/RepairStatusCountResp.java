package com.gigafix.repair.dto;

import com.gigafix.repair.entity.status.RepairStatus;

import lombok.Builder;

@Builder
public record RepairStatusCountResp( // 各repairStatus的筆數與佔全部的百分比，給後台統計圖表用
		RepairStatus status,
		long count,
		double percentage) {}
