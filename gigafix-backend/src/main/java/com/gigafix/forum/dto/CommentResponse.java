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
public class CommentResponse {
	private Long commentId; // 留言 ID
	private Long articleId; // 所屬文章 ID
	private Long authorId; // 留言者會員 ID
	private String authorNickName; // 留言者暱稱
	private String content; // 留言內容
	private Integer likeCount; // 按讚數
	private Boolean likedByCurrentMember; // 呼叫查詢時帶的 memberId 是否已對這則留言按讚（沒帶 memberId 時固定為 false）
	private String status; // 留言狀態
	private LocalDateTime commentCreatedTime; // 留言建立時間
}
