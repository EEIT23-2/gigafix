package com.gigafix.repair.dto;

import lombok.Builder;

@Builder
public record StoreStatsResp( // 各分店的維修單量與結案率，給後台統計績效比較用
		Byte storeId,
		String storeName,
		long totalCount,
		long closedCount,
		double closedRate) {}
