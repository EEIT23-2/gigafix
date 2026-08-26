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

	// 用 NVARCHAR 而不是 VARCHAR：VARCHAR(n) 的 n 是位元組數，中文一字佔 2 bytes，
	// 會跟 CreateReportRequest 的 @Size（算字元數）對不起來。NVARCHAR(250) 的 250 就是字元數，兩邊一致。
	// 長度取 250 是因為規劃書跟組員談定的規格就是「250 個中文字」（當初的 VARCHAR(500) 剛好等於這個量），
	// 不是 500——改成 NVARCHAR 時若直接沿用 500 會把容量變成談定的兩倍
	// 這個欄位不指定 COLLATE：檢舉原因從來不排序、也不做等值比對，定序在這裡沒有作用，
	// 跟著資料庫預設值即可（Category.name 有保留定序，因為判斷名稱重複要靠它）
	@Column(name = "reason", nullable = false, columnDefinition = "NVARCHAR(250)")
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
