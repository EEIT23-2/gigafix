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
@Table(name = "comments")
public class Comment {

	@Id
	@Column(name = "comment_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long commentId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	private Member author;

	@Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(1000)")
	private String content;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	// 狀態代碼：0=可見 1=隱藏 2=下架，用宣告順序對應 TINYINT，新增狀態只能加在最後面
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status", nullable = false, columnDefinition = "TINYINT", check = @CheckConstraint(name = "CK_comments_status", constraint = "status IN (0,1,2)"))
	private CommentStatus status;

	@Column(name = "comment_created_time", nullable = false, updatable = false)
	private LocalDateTime commentCreatedTime;

	@PrePersist
	private void prePersist() {
		if (likeCount == null) {
			likeCount = 0;
		}
		if (status == null) {
			status = CommentStatus.VISIBLE;
		}
		if (commentCreatedTime == null) {
			commentCreatedTime = LocalDateTime.now();
		}
	}

	public enum CommentStatus {
		VISIBLE, HIDDEN, TAKEN_DOWN
	}
}
