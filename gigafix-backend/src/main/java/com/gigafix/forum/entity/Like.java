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
// UK_likes：(member_id, article_id, comment_id) 唯一約束，防止同一會員對同一篇文章/留言重複按讚
// 注意：SQL Server 的唯一約束比較整組 key 時會把 NULL 視為彼此相等（跟 PostgreSQL/ANSI 的「NULL 互不相等」不同），
// 所以即使 article_id/comment_id 互斥、一定有一個是 NULL，例如 (member_id=1, article_id=NULL, comment_id=1) 這組值仍然只能存在一筆，約束是有效的
// CK_likes_target：強制 article_id / comment_id 恰好一個有值（互斥弧）
// 寫成 AND/OR 而非 (x IS NULL) <> (y IS NULL)，因為 T-SQL 的 IS NULL 是述詞不是布林值，不能直接用 <> 比較，SQL Server 會語法錯誤
@Table(name = "likes", uniqueConstraints = @UniqueConstraint(name = "UK_likes", columnNames = { "member_id",
		"article_id", "comment_id" }), check = @CheckConstraint(name = "CK_likes_target", constraint = "(article_id IS NULL AND comment_id IS NOT NULL) OR (article_id IS NOT NULL AND comment_id IS NULL)"))
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
