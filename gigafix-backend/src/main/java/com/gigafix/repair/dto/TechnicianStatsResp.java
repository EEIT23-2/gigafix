package com.gigafix.repair.dto;

import lombok.Builder;

@Builder
public record TechnicianStatsResp( // 各技師的維修單量與結案率，給後台統計績效比較用（尚未被任何技師認領的單不計入）
		Integer technicianId,
		String technicianName,
		long totalCount,
		long closedCount,
		double closedRate) {}
