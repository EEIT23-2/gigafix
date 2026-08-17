package com.gigafix.forum.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.CreateReportRequest;
import com.gigafix.forum.dto.ReportResponse;
import com.gigafix.forum.dto.UpdateReportStatusRequest;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Comment;
import com.gigafix.forum.entity.Report;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.CommentRepository;
import com.gigafix.forum.repository.ReportRepository;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

	// 檢舉 Repository
	private final ReportRepository reportRepository;

	// 文章 Repository
	private final ArticleRepository articleRepository;

	// 留言 Repository
	private final CommentRepository commentRepository;

	// 會員 Repository
	private final MemberRepository memberRepository;

	// 檢舉文章
	@Override
	@Transactional
	public ReportResponse reportArticle(Long memberId, Long articleId, CreateReportRequest request) {

		// 檢查會員是否存在
		Member reporter = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		// 檢查文章是否存在（不限狀態，已下架的文章也可以被檢舉）
		Article article = articleRepository.findById(articleId)
				.orElseThrow(() -> new IllegalArgumentException("文章不存在，articleId：" + articleId));

		// 建立檢舉
		Report report = new Report();
		report.setReporter(reporter);
		report.setArticle(article);
		report.setReason(request.getReason());

		// 儲存檢舉（status/建立時間交給 @PrePersist 處理）
		Report savedReport = reportRepository.save(report);

		return toReportResponse(savedReport);
	}

	// 檢舉留言
	@Override
	@Transactional
	public ReportResponse reportComment(Long memberId, Long commentId, CreateReportRequest request) {

		// 檢查會員是否存在
		Member reporter = memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("會員不存在，memberId：" + memberId));

		// 檢查留言是否存在（不限狀態）
		Comment comment = commentRepository.findById(commentId)
				.orElseThrow(() -> new IllegalArgumentException("留言不存在，commentId：" + commentId));

		// 建立檢舉
		Report report = new Report();
		report.setReporter(reporter);
		report.setComment(comment);
		report.setReason(request.getReason());

		Report savedReport = reportRepository.save(report);

		return toReportResponse(savedReport);
	}

	// ---------------後台管理功能（暫無權限檢查）----------------------

	// 查詢檢舉列表
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	public List<ReportResponse> getReports(Report.ReportStatus status) {

		List<Report> reports = status == null ? reportRepository.findAll() : reportRepository.findByStatus(status);

		return reports.stream()
				.map(this::toReportResponse)
				.toList();
	}

	// 處理／關閉檢舉
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public ReportResponse updateReportStatus(Long reportId, UpdateReportStatusRequest request) {

		// 查詢檢舉
		Report report = reportRepository.findById(reportId)
				.orElseThrow(() -> new IllegalArgumentException("檢舉不存在，reportId：" + reportId));

		report.setStatus(request.getStatus());
		Report savedReport = reportRepository.save(report);

		return toReportResponse(savedReport);
	}

	// 將 Report Entity 轉成 ReportResponse DTO
	private ReportResponse toReportResponse(Report report) {

		return ReportResponse.builder()
				.reportId(report.getReportId())
				.reporterId(report.getReporter().getId())
				.reporterNickName(report.getReporter().getNickName())
				.articleId(report.getArticle() != null ? report.getArticle().getArticleId() : null)
				.commentId(report.getComment() != null ? report.getComment().getCommentId() : null)
				.status(report.getStatus().name())
				.reason(report.getReason())
				.reportCreatedTime(report.getReportCreatedTime())
				.build();
	}
}
