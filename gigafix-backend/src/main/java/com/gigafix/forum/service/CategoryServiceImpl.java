package com.gigafix.forum.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.CategoryResponse;
import com.gigafix.forum.dto.CreateCategoryRequest;
import com.gigafix.forum.dto.UpdateCategoryRequest;
import com.gigafix.forum.entity.Category;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

	// 分類 Repository
	private final CategoryRepository categoryRepository;

	// 文章 Repository（刪除分類前檢查是否還有文章使用中）
	private final ArticleRepository articleRepository;

	// ---------------後台管理功能（暫無權限檢查）----------------------

	// 建立分類
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public CategoryResponse createCategory(CreateCategoryRequest request) {

		// 檢查分類名稱是否重複
		if (categoryRepository.findByName(request.getName()) != null) {
			throw new IllegalStateException("分類名稱已存在");
		}

		Category category = new Category();
		category.setName(request.getName());

		Category savedCategory = categoryRepository.save(category);

		return toCategoryResponse(savedCategory);
	}

	// 修改分類名稱
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public CategoryResponse updateCategory(Integer categoryId, UpdateCategoryRequest request) {

		// 查詢分類
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("分類不存在，categoryId：" + categoryId));

		// 檢查新名稱是否已被其他分類使用
		Category existing = categoryRepository.findByName(request.getName());
		if (existing != null && !existing.getCategoryId().equals(categoryId)) {
			throw new IllegalStateException("分類名稱已存在");
		}

		category.setName(request.getName());
		Category savedCategory = categoryRepository.save(category);

		return toCategoryResponse(savedCategory);
	}

	// 刪除分類
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public void deleteCategory(Integer categoryId) {

		// 查詢分類
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("分類不存在，categoryId：" + categoryId));

		// 還有文章使用這個分類就不能刪除（category_id 是 articles 的 NOT NULL 外鍵）
		if (articleRepository.existsByCategory_CategoryId(categoryId)) {
			throw new IllegalStateException("此分類仍有文章使用中，無法刪除");
		}

		categoryRepository.delete(category);
	}

	// ---------------公開查詢----------------------

	// 查詢所有分類
	@Override
	public List<CategoryResponse> getCategories() {

		return categoryRepository.findAll().stream()
				.map(this::toCategoryResponse)
				.toList();
	}

	// 將 Category Entity 轉成 CategoryResponse DTO
	private CategoryResponse toCategoryResponse(Category category) {

		return CategoryResponse.builder()
				.categoryId(category.getCategoryId())
				.name(category.getName())
				.build();
	}
}
