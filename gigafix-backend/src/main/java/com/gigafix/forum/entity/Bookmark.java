package com.gigafix.forum.entity;

import java.time.LocalDateTime;

import com.gigafix.member.entity.Member;

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
@Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(name = "UK_bookmarks", columnNames = { "member_id",
		"article_id" }))
public class Bookmark {

	@Id
	@Column(name = "bookmark_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bookmarkId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;

	@Column(name = "bookmark_created_time", nullable = false, updatable = false)
	private LocalDateTime bookmarkCreatedTime;

	@PrePersist
	private void prePersist() {
		if (bookmarkCreatedTime == null) {
			bookmarkCreatedTime = LocalDateTime.now();
		}
	}
}
