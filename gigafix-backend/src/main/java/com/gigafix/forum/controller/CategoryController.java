package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gigafix.forum.dto.CategoryResponse;
import com.gigafix.forum.dto.CreateCategoryRequest;
import com.gigafix.forum.dto.UpdateCategoryRequest;
import com.gigafix.forum.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 分類 Controller
 * 提供分類相關 REST API
 */
@RestController
@RequiredArgsConstructor
public class CategoryController {

	// 分類 Service
	private final CategoryService categoryService;

	// 查詢所有分類（公開）
	@GetMapping("/api/categories")
	public ResponseEntity<List<CategoryResponse>> getCategories() {

		List<CategoryResponse> responses = categoryService.getCategories();

		return ResponseEntity.ok(responses);
	}

	// 建立分類
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@PostMapping("/api/admin/forum/categories")
	public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {

		CategoryResponse response = categoryService.createCategory(request);

		return ResponseEntity.ok(response);
	}

	// 修改分類名稱
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@PutMapping("/api/admin/forum/categories/{categoryId}")
	public ResponseEntity<CategoryResponse> updateCategory(
			@PathVariable Integer categoryId,
			@Valid @RequestBody UpdateCategoryRequest request) {

		CategoryResponse response = categoryService.updateCategory(categoryId, request);

		return ResponseEntity.ok(response);
	}

	// 刪除分類
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@DeleteMapping("/api/admin/forum/categories/{categoryId}")
	public ResponseEntity<Void> deleteCategory(@PathVariable Integer categoryId) {

		categoryService.deleteCategory(categoryId);

		return ResponseEntity.noContent().build();
	}
}
