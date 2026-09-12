package com.gigafix.repair.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record RepairStatsResp( // 給後台維修單統計圖表用
		long totalCount, // 全部維修單數
		long rejectedCount, // approvalStatus=REJECTED的筆數（客戶曾拒絕報價，不論該單目前是否已結案）
		double rejectedPercentage, // 拒絕維修佔全部的百分比
		long closedCount, // repairStatus=CLOSED的筆數
		double closedPercentage, // 結案佔全部的百分比
		Double avgCloseDurationHours, // 已結案單「建立到結案」平均耗時(小時)，用repairUpdatedTime近似結案時間，沒有已結案單時為null
		List<CloseDurationBucketResp> closeDurationDistribution, // 已結案單的耗時分布，給長條圖用
		List<RepairStatusCountResp> statusBreakdown, // 全部9種repairStatus各自的筆數與佔全部的百分比（含已結案/已取消/未送檢），依enum宣告順序(維修流程順序)排列
		List<StoreStatsResp> storeStats, // 各分店的維修單量與結案率
		List<TechnicianStatsResp> technicianStats) {} // 各技師的維修單量與結案率（未認領的單不計入任何技師）
