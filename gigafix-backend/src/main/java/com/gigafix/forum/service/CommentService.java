package com.gigafix.forum.service;

import java.util.List;

import com.gigafix.forum.dto.CommentResponse;
import com.gigafix.forum.dto.CreateCommentRequest;

/**
 * 留言 Service
 * 定義留言相關商業功能
 */
public interface CommentService {

	// 留言
	CommentResponse createComment(Long memberId, Long articleId, CreateCommentRequest request);

	// 查詢文章底下所有留言（依時間排序，排除已下架留言）
	// memberId 選填，帶了的話回傳的每則留言會附上呼叫者是否已對它按讚
	List<CommentResponse> getComments(Long articleId, Long memberId);

	// 刪除（軟刪除）自己的留言
	void deleteComment(Long memberId, Long commentId);
}
