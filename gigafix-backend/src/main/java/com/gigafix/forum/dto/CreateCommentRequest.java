package com.gigafix.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Request：留言所需要的資訊
@Getter
@Setter
public class CreateCommentRequest {

	@NotBlank(message = "留言內容不能為空")
	@Size(max = 1000, message = "留言內容不能超過1000字")
	private String content;
}
