package com.gigafix.forum.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
	private Long reportId; // 檢舉 ID
	private Long reporterId; // 檢舉人會員 ID
	private String reporterNickName; // 檢舉人暱稱
	private Long articleId; // 被檢舉文章 ID（與 commentId 恰好一個有值）
	private Long commentId; // 被檢舉留言 ID（與 articleId 恰好一個有值）
	private String status; // 處理狀態
	private String reason; // 檢舉原因
	private LocalDateTime reportCreatedTime; // 檢舉時間
}
