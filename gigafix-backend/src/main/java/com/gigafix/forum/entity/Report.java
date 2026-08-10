package com.gigafix.forum.entity;

import java.time.LocalDateTime;

import com.gigafix.member.entity.Member;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "reports", check = @CheckConstraint(name = "CK_reports_target", constraint = "([article_id] IS NULL) <> ([comment_id] IS NULL)"))
public class Report {

	@Id
	@Column(name = "report_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reportId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reporter_id", nullable = false)
	private Member reporter;

	// article_id / comment_id 互斥弧設計，恰好一個有值，由 CK_reports_target 於資料庫端強制
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id")
	private Article article;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comment_id")
	private Comment comment;

	// 狀態代碼：0=待處理 1=已處理 2=關閉，用宣告順序對應 TINYINT，新增狀態只能加在最後面
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status", nullable = false, columnDefinition = "TINYINT", check = @CheckConstraint(name = "CK_reports_status", constraint = "status IN (0,1,2)"))
	private ReportStatus status;

	@Column(name = "reason", nullable = false, columnDefinition = "VARCHAR(500) COLLATE Chinese_Taiwan_Stroke_100_CI_AS")
	private String reason;

	@Column(name = "report_created_time", nullable = false, updatable = false)
	private LocalDateTime reportCreatedTime;

	@PrePersist
	private void prePersist() {
		if (status == null) {
			status = ReportStatus.PENDING;
		}
		if (reportCreatedTime == null) {
			reportCreatedTime = LocalDateTime.now();
		}
	}

	public enum ReportStatus {
		PENDING, RESOLVED, CLOSED
	}
}
