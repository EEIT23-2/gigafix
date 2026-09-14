package com.gigafix.repair.dto;

import lombok.Builder;

@Builder
public record CloseDurationBucketResp( // 結案耗時分布的其中一個區間
		String label, // 例如"1小時內"、"1~3天"
		long count) {} // 落在這個區間的已結案單數
