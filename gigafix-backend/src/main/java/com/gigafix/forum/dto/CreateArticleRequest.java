package com.gigafix.forum.dto;

import com.gigafix.forum.entity.Article;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Request：發文所需要的資訊
@Getter
@Setter
public class CreateArticleRequest {

	@NotNull(message = "分類不能為空")
	private Integer categoryId;

	// 標題/內文允許空白，讓草稿自動儲存可以先存空字串；是否為空只在「發布」時才驗證（見 Service 層）
	@Size(max = 255, message = "標題不能超過255字")
	private String title;

	private String content;

	private String coverImage; // 封面圖網址，允許 NULL

	// 選填，只接受 DRAFT 或 PUBLISHED；未填時預設 PUBLISHED（維持「發文即發布」的既有行為）
	private Article.ArticleStatus status;
}
