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
	private LocalDateTime articleEditedTime; // 內文最後被實際編輯的時間；狀態變更/置頂/建立不會動到它
	private Boolean visible; // 呼叫者是否看得到完整內容；false 時 title/content/coverImage 為 null
	private String visibilityMessage; // 一般情況下 visible=false 時才有說明文字，visible=true 時為 null；
										// 例外是作者預覽自己被隱藏/強制隱藏的內容：visible=true 但仍會帶一句只有作者看得到的提示
	private Long parentArticleId; // 所屬根文章 id；一般文章/專欄為 null，樓層才有值
	private Integer floorNumber; // 第幾樓；只有蓋樓相關 API（列表/建立）會填，其餘為 null
	private Integer floorCount; // 這篇文章底下有幾樓；樓層本身查出來固定是 0
	// 以下兩個欄位行為相同：只有「文章詳情」與「樓層列表」會填，其餘路徑（列表、發文、編輯、後台）一律為 null。
	// 有填的情況下：沒帶 memberId、或內容因下架/隱藏而看不到時為 null（不做多餘查詢），其餘為 true/false
	private Boolean likedByCurrentMember; // 查詢時帶的 memberId 是否已對這篇按讚
	private Boolean bookmarkedByCurrentMember; // 查詢時帶的 memberId 是否已收藏這篇
}
