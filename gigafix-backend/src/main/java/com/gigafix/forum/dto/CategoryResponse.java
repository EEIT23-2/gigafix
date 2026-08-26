package com.gigafix.forum.dto;

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
public class CategoryResponse {
	private Integer categoryId; // 分類 ID
	private String name; // 分類名稱
	private Long articleCount; // 使用此分類的文章數（含已下架文章與樓層，與刪除防護的判斷一致）
}
