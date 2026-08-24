package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gigafix.forum.dto.CreateReportRequest;
import com.gigafix.forum.dto.ReportResponse;
import com.gigafix.forum.dto.UpdateReportStatusRequest;
import com.gigafix.forum.entity.Report;
import com.gigafix.forum.service.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 檢舉 Controller
 * 提供檢舉相關 REST API
 */
@RestController
@RequiredArgsConstructor
public class ReportController {

	// 檢舉 Service
	private final ReportService reportService;

	// 檢舉文章
	@PostMapping("/api/members/{memberId}/articles/{articleId}/reports")
	public ResponseEntity<ReportResponse> reportArticle(
			@PathVariable Long memberId,
			@PathVariable Long articleId,
			@Valid @RequestBody CreateReportRequest request) {

		ReportResponse response = reportService.reportArticle(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// 檢舉留言
	@PostMapping("/api/members/{memberId}/comments/{commentId}/reports")
	public ResponseEntity<ReportResponse> reportComment(
			@PathVariable Long memberId,
			@PathVariable Long commentId,
			@Valid @RequestBody CreateReportRequest request) {

		ReportResponse response = reportService.reportComment(memberId, commentId, request);

		return ResponseEntity.ok(response);
	}

	// 查詢檢舉列表（後台用）
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/reports")
	public ResponseEntity<List<ReportResponse>> getReports(
			@RequestParam(required = false) Report.ReportStatus status) {

		List<ReportResponse> responses = reportService.getReports(status);

		return ResponseEntity.ok(responses);
	}

	// 單筆檢舉詳情（後台用）
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/reports/{reportId}")
	public ResponseEntity<ReportResponse> getReport(@PathVariable Long reportId) {

		ReportResponse response = reportService.getReport(reportId);

		return ResponseEntity.ok(response);
	}

	// 處理／關閉檢舉
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@PatchMapping("/api/reports/{reportId}/status")
	public ResponseEntity<ReportResponse> updateReportStatus(
			@PathVariable Long reportId,
			@Valid @RequestBody UpdateReportStatusRequest request) {

		ReportResponse response = reportService.updateReportStatus(reportId, request);

		return ResponseEntity.ok(response);
	}
}
