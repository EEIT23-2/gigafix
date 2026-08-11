package com.gigafix.forum.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.forum.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByArticle_ArticleIdOrderByCommentCreatedTimeAsc(Long articleId);

	List<Comment> findByAuthor_IdOrderByCommentCreatedTimeDesc(Long authorId);
}
