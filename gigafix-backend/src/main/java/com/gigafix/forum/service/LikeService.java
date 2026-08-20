package com.gigafix.forum.service;

import com.gigafix.forum.dto.LikeResponse;

/**
 * 讚 Service
 * 定義按讚相關商業功能
 */
public interface LikeService {

	// 對文章按讚
	LikeResponse likeArticle(Long memberId, Long articleId);

	// 取消對文章的讚
	void unlikeArticle(Long memberId, Long articleId);

	// 對留言按讚
	LikeResponse likeComment(Long memberId, Long commentId);

	// 取消對留言的讚
	void unlikeComment(Long memberId, Long commentId);

	// 查詢會員是否已對某篇文章按讚
	boolean hasLikedArticle(Long memberId, Long articleId);

	// 查詢會員是否已對某則留言按讚
	boolean hasLikedComment(Long memberId, Long commentId);
}
