package com.gigafix.forum.service;

import com.gigafix.forum.dto.ForumSeedResponse;

/**
 * 論壇展示資料產生器
 *
 * 後台「一鍵產生展示資料」用：把 resources/forum-seed 下的文案與圖片網址寫進資料庫，
 * 建立分類、根文章、樓層、留言與按讚，讓論壇在展示或測試時有接近真實的內容。
 */
public interface ForumSeedService {

	// 產生展示資料；已經產生過就丟 IllegalStateException（409）
	ForumSeedResponse seedForumData();
}
