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
public class LikeResponse {
	private Long likeId; // 讚的 ID
	private Integer likeCount; // 按讚目標（文章或留言）目前的總讚數
}
