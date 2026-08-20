package com.gigafix.forum.service;

import java.util.List;

import com.gigafix.forum.dto.BookmarkResponse;

/**
 * 收藏 Service
 * 定義收藏相關商業功能
 */
public interface BookmarkService {

	// 收藏文章
	BookmarkResponse addBookmark(Long memberId, Long articleId);

	// 取消收藏
	void removeBookmark(Long memberId, Long articleId);

	// 查詢自己收藏的文章列表
	List<BookmarkResponse> getBookmarks(Long memberId);

	// 查詢會員是否已收藏某篇文章
	boolean hasBookmarked(Long memberId, Long articleId);
}
