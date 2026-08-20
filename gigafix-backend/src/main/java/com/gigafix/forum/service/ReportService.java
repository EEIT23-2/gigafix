package com.gigafix.forum.service;

import java.util.List;

import com.gigafix.forum.dto.CreateReportRequest;
import com.gigafix.forum.dto.ReportResponse;
import com.gigafix.forum.dto.UpdateReportStatusRequest;
import com.gigafix.forum.entity.Report;

/**
 * 檢舉 Service
 * 定義檢舉相關商業功能
 */
public interface ReportService {

	// 檢舉文章
	ReportResponse reportArticle(Long memberId, Long articleId, CreateReportRequest request);

	// 檢舉留言
	ReportResponse reportComment(Long memberId, Long commentId, CreateReportRequest request);

	// ---------------後台管理功能（暫無權限檢查）------------------

	// 查詢檢舉列表，status 為 null 表示查全部
	List<ReportResponse> getReports(Report.ReportStatus status);

	// 處理／關閉檢舉
	ReportResponse updateReportStatus(Long reportId, UpdateReportStatusRequest request);
}
