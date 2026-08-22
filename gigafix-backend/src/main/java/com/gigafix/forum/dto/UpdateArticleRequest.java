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

	// 標題/內文允許空白，讓草稿自動儲存可以先存空字串；但欄位本身必須存在（不能整個省略），
	// 否則 PUT 只帶其中一欄時，另一欄會被誤判成「使用者要清空」而覆蓋掉既有內容
	@NotNull(message = "標題不能省略（可傳空字串，但不能省略此欄位）")
	@Size(max = 255, message = "標題不能超過255字")
	private String title;

	@NotNull(message = "內文不能省略（可傳空字串，但不能省略此欄位）")
	private String content;

	private String coverImage; // 封面圖網址，允許 NULL
}
