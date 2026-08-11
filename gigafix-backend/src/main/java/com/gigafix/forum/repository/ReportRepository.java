package com.gigafix.forum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.forum.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

	List<Report> findByStatus(Report.ReportStatus status);

	List<Report> findByArticle_ArticleId(Long articleId);

	List<Report> findByComment_CommentId(Long commentId);
}
