package com.gigafix.forum.dto;

import com.gigafix.forum.entity.Comment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Request：後台直接設定留言狀態用
@Getter
@Setter
public class UpdateCommentStatusRequest {

	@NotNull(message = "狀態不能為空")
	private Comment.CommentStatus status;
}
