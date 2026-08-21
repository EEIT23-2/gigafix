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
public class ArticleResponse {
	private Long articleId; // 文章 ID
	private Integer categoryId; // 分類 ID
	private String categoryName; // 分類名稱
	private Long authorId; // 作者會員 ID
	private String authorNickName; // 作者暱稱
	private String title; // 標題
	private String content; // 內文
	private Integer viewCount; // 瀏覽數
	private Integer likeCount; // 按讚數
	private Integer commentCount; // 留言數
	private String coverImage; // 封面圖網址
	private String status; // 文章狀態
	private Boolean isPinned; // 是否置頂
	private LocalDateTime articleCreatedTime; // 文章建立時間
	private LocalDateTime articleUpdatedTime; // 文章最後更新時間
}
