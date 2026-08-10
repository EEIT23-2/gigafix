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
// CK_reports_target：強制 article_id / comment_id 恰好一個有值（互斥弧）
// 寫成 AND/OR 而非 (x IS NULL) <> (y IS NULL)，因為 T-SQL 的 IS NULL 是述詞不是布林值，不能直接用 <> 比較，SQL Server 會語法錯誤
@Table(name = "reports", check = @CheckConstraint(name = "CK_reports_target", constraint = "(article_id IS NULL AND comment_id IS NOT NULL) OR (article_id IS NOT NULL AND comment_id IS NULL)"))
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
	// CK_reports_status：資料庫端再擋一次，避免非法數值繞過應用層直接寫進 TINYINT 欄位
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status", nullable = false, columnDefinition = "TINYINT", check = @CheckConstraint(name = "CK_reports_status", constraint = "status IN (0,1,2)"))
	private ReportStatus status;

	// COLLATE Chinese_Taiwan_Stroke_CI_AS：指定繁體中文筆畫排序定序（不分大小寫、不分腔調），確保檢舉原因文字排序/比較符合中文筆畫順序
	// 注意：SQL Server 沒有 Chinese_Taiwan_Stroke_100_CI_AS 這個定序名稱，帶 _100_ 版本的筆畫排序定序全名是 Chinese_Traditional_Stroke_Order_100_CI_AS，寫錯會導致建表失敗
	@Column(name = "reason", nullable = false, columnDefinition = "VARCHAR(500) COLLATE Chinese_Taiwan_Stroke_CI_AS")
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
