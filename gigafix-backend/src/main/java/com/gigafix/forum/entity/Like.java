package com.gigafix.forum.entity;

import java.time.LocalDateTime;

import com.gigafix.member.entity.Member;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(name = "UK_likes", columnNames = { "member_id",
		"article_id", "comment_id" }), check = @CheckConstraint(name = "CK_likes_target", constraint = "([article_id] IS NULL) <> ([comment_id] IS NULL)"))
public class Like {

	@Id
	@Column(name = "like_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long likeId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	// article_id / comment_id 互斥弧設計，恰好一個有值，由 CK_likes_target 於資料庫端強制
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id")
	private Article article;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comment_id")
	private Comment comment;

	@Column(name = "like_created_time", nullable = false, updatable = false)
	private LocalDateTime likeCreatedTime;

	@PrePersist
	private void prePersist() {
		if (likeCreatedTime == null) {
			likeCreatedTime = LocalDateTime.now();
		}
	}
}
