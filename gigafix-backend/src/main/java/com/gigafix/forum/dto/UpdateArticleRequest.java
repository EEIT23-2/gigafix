package com.gigafix.forum.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Request：編輯文章所需要的資訊
@Getter
@Setter
public class UpdateArticleRequest {

	@NotNull(message = "分類不能為空")
	private Integer categoryId;

	// 標題/內文允許空白，讓草稿自動儲存可以先存空字串；文章不是草稿狀態時才會在 Service 層驗證不能為空
	@Size(max = 255, message = "標題不能超過255字")
	private String title;

	private String content;

	private String coverImage; // 封面圖網址，允許 NULL
}
