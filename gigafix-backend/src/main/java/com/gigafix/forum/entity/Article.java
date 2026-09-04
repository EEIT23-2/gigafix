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
import jakarta.persistence.PreUpdate;
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
@Table(name = "articles")
public class Article {

	@Id
	@Column(name = "article_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long articleId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private Member author;

	// 蓋樓用：NULL = 一般文章/專欄；有值 = 樓層，指向該串的根文章本身（不是上一樓）
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_article_id")
	private Article parentArticle;

	@Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(255)")
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
	private String content;

	@Column(name = "view_count", nullable = false)
	private Integer viewCount;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "comment_count", nullable = false)
	private Integer commentCount;

	@Column(name = "cover_image", columnDefinition = "VARCHAR(255)")
	private String coverImage;

	// 狀態代碼：0=草稿 1=發布 2=隱藏 3=下架 4=關閉 5=強制隱藏 6=強制關閉，用宣告順序對應 TINYINT，新增狀態只能加在最後面
	// CK_articles_status：資料庫端再擋一次，避免非法數值繞過應用層直接寫進 TINYINT 欄位
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status", nullable = false, columnDefinition = "TINYINT", check = @CheckConstraint(name = "CK_articles_status", constraint = "status IN (0,1,2,3,4,5,6)"))
	private ArticleStatus status;

	@Column(name = "is_pinned", nullable = false)
	private Boolean isPinned;

	@Column(name = "article_created_time", nullable = false, updatable = false)
	private LocalDateTime articleCreatedTime;

	@Column(name = "article_updated_time")
	private LocalDateTime articleUpdatedTime;

	// 只在使用者真的編輯內文時（updateArticle / updateFloor）手動寫入，不放進 @PreUpdate——
	// articleUpdatedTime 那個舊欄位的教訓就是任何欄位變動（含軟刪除改狀態、管理員操作）都會動到它，
	// 沒辦法拿來判斷「內容是否被編輯過」
	@Column(name = "article_edited_time")
	private LocalDateTime articleEditedTime;

	@PrePersist
	private void prePersist() {
		if (viewCount == null) {
			viewCount = 0;
		}
		if (likeCount == null) {
			likeCount = 0;
		}
		if (commentCount == null) {
			commentCount = 0;
		}
		if (status == null) {
			status = ArticleStatus.DRAFT;
		}
		if (isPinned == null) {
			isPinned = false;
		}
		if (articleCreatedTime == null) {
			articleCreatedTime = LocalDateTime.now();
		}
	}

	@PreUpdate
	private void preUpdate() {
		articleUpdatedTime = LocalDateTime.now();
	}

	public enum ArticleStatus {
		DRAFT, PUBLISHED, HIDDEN, TAKEN_DOWN, CLOSED, FORCE_HIDDEN, FORCE_CLOSED
	}
}
