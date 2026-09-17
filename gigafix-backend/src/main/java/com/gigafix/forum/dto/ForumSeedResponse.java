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
public class ForumSeedResponse {
	private int categoryCount; // 新建立的分類數（已存在的同名分類會沿用，不計入）
	private int articleCount; // 建立的根文章數
	private int floorCount; // 建立的樓層數
	private int commentCount; // 建立的留言數
	private int likeCount; // 建立的按讚數（likes 實際列數）
}
