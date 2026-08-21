package com.gigafix.forum.dto;

import com.gigafix.forum.entity.Article;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Request：後台審核/下架/置頂用
@Getter
@Setter
public class UpdateArticleStatusRequest {

	@NotNull(message = "狀態不能為空")
	private Article.ArticleStatus status;

	private Boolean isPinned; // null 代表不變更置頂狀態
}
