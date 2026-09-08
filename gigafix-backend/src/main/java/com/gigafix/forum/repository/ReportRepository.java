package com.gigafix.forum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.forum.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

	List<Report> findByStatus(Report.ReportStatus status);

	List<Report> findByArticle_ArticleId(Long articleId);

	List<Report> findByComment_CommentId(Long commentId);

	// 防止重複檢舉：同一人對同一目標若已有待處理的檢舉就不允許再送。
	// 帶 status 而不是單純看有沒有檢舉過，是為了讓已處理/已關閉的案件之後仍可再次檢舉
	boolean existsByReporter_IdAndArticle_ArticleIdAndStatus(Long reporterId, Long articleId,
			Report.ReportStatus status);

	boolean existsByReporter_IdAndComment_CommentIdAndStatus(Long reporterId, Long commentId,
			Report.ReportStatus status);
}
