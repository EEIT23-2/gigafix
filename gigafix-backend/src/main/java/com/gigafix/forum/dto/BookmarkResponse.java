package com.gigafix.forum.dto;

import java.time.LocalDateTime;

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
public class BookmarkResponse {
	private Long bookmarkId; // 收藏 ID
	private LocalDateTime bookmarkCreatedTime; // 收藏時間
	private ArticleResponse article; // 收藏的文章
}
