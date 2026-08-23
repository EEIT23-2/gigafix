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
	private Boolean visible; // 呼叫者是否看得到完整內容；false 時 title/content/coverImage 為 null
	private String visibilityMessage; // visible=false 時的說明文字；visible=true 時為 null
	private Long parentArticleId; // 所屬根文章 id；一般文章/專欄為 null，樓層才有值
	private Integer floorNumber; // 第幾樓；只有蓋樓相關 API（列表/建立）會填，其餘為 null
	private Integer floorCount; // 這篇文章底下有幾樓；樓層本身查出來固定是 0
}
