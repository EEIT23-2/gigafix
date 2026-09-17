package com.gigafix.forum.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 展示資料種子檔（resources/forum-seed/forum-seed.json）的對應結構
 *
 * 文案與程式刻意分離：之後要改標題、內文或回覆只需要動 JSON，不必重新改 Java。
 * 內文中的 [[IMG-xx]] 是圖片佔位符，會在產生資料時換成 forum-seed-images.json 裡的網址；
 * 對應的網址若是空字串，整個佔位符會被移除（那一篇就沒有圖）。
 */
@Getter
@Setter
public class ForumSeedData {

	// 分類名稱，依序建立
	private List<String> categories = new ArrayList<>();

	// 根文章
	private List<SeedArticle> articles = new ArrayList<>();

	@Getter
	@Setter
	public static class SeedArticle {
		private String category; // 分類名稱，必須出現在上面的 categories 裡
		private String title;
		private String content; // 富文本 HTML，可含 [[IMG-xx]]
		private String coverImage; // 圖片代號（IMG-xx），沒有封面就不填
		private boolean pinned; // 是否置頂
		private String status; // 不填 = PUBLISHED；目前只會用到 CLOSED
		private List<String> comments = new ArrayList<>(); // 掛在根文章上的留言
		private List<SeedFloor> floors = new ArrayList<>(); // 樓層，依序就是樓層順序
	}

	@Getter
	@Setter
	public static class SeedFloor {
		private String content; // 富文本 HTML，可含 [[IMG-xx]]
		private List<String> comments = new ArrayList<>(); // 掛在這一樓上的留言
	}
}
