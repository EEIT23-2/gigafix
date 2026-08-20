package com.gigafix.forum.service;

import java.util.List;

import com.gigafix.forum.dto.CategoryResponse;
import com.gigafix.forum.dto.CreateCategoryRequest;
import com.gigafix.forum.dto.UpdateCategoryRequest;

/**
 * 分類 Service
 * 定義分類相關商業功能
 */
public interface CategoryService {

	// ---------------後台管理功能（暫無權限檢查）------------------

	// 建立分類
	CategoryResponse createCategory(CreateCategoryRequest request);

	// 修改分類名稱
	CategoryResponse updateCategory(Integer categoryId, UpdateCategoryRequest request);

	// 刪除分類（有文章使用中則不可刪除）
	void deleteCategory(Integer categoryId);

	// ---------------公開查詢------------------

	// 查詢所有分類
	List<CategoryResponse> getCategories();
}
