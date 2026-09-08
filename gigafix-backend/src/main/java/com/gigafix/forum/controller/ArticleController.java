package com.gigafix.forum.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.CreateFloorRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;
import com.gigafix.forum.dto.UpdateFloorRequest;
import com.gigafix.forum.dto.UpdatePinRequest;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.service.ArticleService;
import com.gigafix.common.util.SecurityUtils;
import com.gigafix.member.security.MemberUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 文章 Controller
 * 提供文章相關 REST API
 */
@RestController
@RequiredArgsConstructor
public class ArticleController {

	// 文章 Service
	private final ArticleService articleService;

	// 文章列表（公開）
	@GetMapping("/api/articles")
	public ResponseEntity<Page<ArticleResponse>> getArticles(
			@RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "latest") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		Page<ArticleResponse> response = articleService.getArticles(categoryId, keyword, sort, page, size);

		return ResponseEntity.ok(response);
	}

	// 文章詳情（公開，瀏覽數 +1；有登入的話會附上是否為作者本人、按讚/收藏狀態）
	@GetMapping("/api/articles/{articleId}")
	public ResponseEntity<ArticleResponse> getArticle(
			@PathVariable Long articleId,
			Authentication authentication) {

		ArticleResponse response = articleService.getArticle(articleId, currentMemberId(authentication));

		return ResponseEntity.ok(response);
	}

	// 發文
	@PostMapping("/api/members/me/articles")
	public ResponseEntity<ArticleResponse> createArticle(
			Authentication authentication,
			@Valid @RequestBody CreateArticleRequest request) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		ArticleResponse response = articleService.createArticle(memberId, request);

		return ResponseEntity.ok(response);
	}

	// 編輯自己的文章
	@PutMapping("/api/members/me/articles/{articleId}")
	public ResponseEntity<ArticleResponse> updateArticle(
			Authentication authentication,
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleRequest request) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		ArticleResponse response = articleService.updateArticle(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// 刪除（軟刪除）自己的文章
	@DeleteMapping("/api/members/me/articles/{articleId}")
	public ResponseEntity<Void> deleteArticle(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		articleService.deleteArticle(memberId, articleId);

		return ResponseEntity.noContent().build();
	}

	// 捨棄草稿：真的刪列。路徑用 /drafts 而不是 /articles，讓「只吃草稿」這件事從 URL 就看得出來，
	// 也不會跟上面那支軟刪除的 DELETE /articles/{id} 混淆
	@DeleteMapping("/api/members/me/drafts/{articleId}")
	public ResponseEntity<Void> deleteDraft(
			Authentication authentication,
			@PathVariable Long articleId) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		articleService.deleteDraft(memberId, articleId);

		return ResponseEntity.noContent().build();
	}

	// 會員自行變更自己文章的狀態（發布/隱藏/下架/關閉）
	@PatchMapping("/api/members/me/articles/{articleId}/status")
	public ResponseEntity<ArticleResponse> updateOwnArticleStatus(
			Authentication authentication,
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleStatusRequest request) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		ArticleResponse response = articleService.updateOwnArticleStatus(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}

	// 會員自己的文章列表（個人中心）
	@GetMapping("/api/members/me/articles")
	public ResponseEntity<List<ArticleResponse>> getMyArticles(Authentication authentication) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		List<ArticleResponse> responses = articleService.getMyArticles(memberId);

		return ResponseEntity.ok(responses);
	}

	// 樓層列表（公開；有登入的話套用與文章詳情相同的可見性規則）
	@GetMapping("/api/articles/{articleId}/floors")
	public ResponseEntity<List<ArticleResponse>> getFloors(
			@PathVariable Long articleId,
			Authentication authentication) {

		List<ArticleResponse> responses = articleService.getFloors(articleId, currentMemberId(authentication));

		return ResponseEntity.ok(responses);
	}

	// 蓋樓
	@PostMapping("/api/members/me/articles/{articleId}/floors")
	public ResponseEntity<ArticleResponse> createFloor(
			Authentication authentication,
			@PathVariable Long articleId,
			@Valid @RequestBody CreateFloorRequest request) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		ArticleResponse response = articleService.createFloor(memberId, articleId, request);

		return ResponseEntity.ok(response);
	}
	// 編輯樓層。路徑放在頂層 /floors/{floorId} 而不是掛在樓主底下，
	// 因為更新只需要樓層自己的 id——與 DELETE /api/members/me/comments/{commentId} 同一種形狀
	@PutMapping("/api/members/me/floors/{floorId}")
	public ResponseEntity<ArticleResponse> updateFloor(
			Authentication authentication,
			@PathVariable Long floorId,
			@Valid @RequestBody UpdateFloorRequest request) {

		Long memberId = SecurityUtils.getCurrentMember(authentication).getId();
		ArticleResponse response = articleService.updateFloor(memberId, floorId, request);

		return ResponseEntity.ok(response);
	}

	// 從 Authentication 解出目前登入的 memberId；未登入（匿名 principal）時回傳 null，
	// 給公開端點（文章詳情、樓層列表）用來判斷「訪客」還是「登入但還沒查到身分」
	private Long currentMemberId(Authentication authentication) {
		if (authentication != null && authentication.getPrincipal() instanceof MemberUserDetails details) {
			return details.getId();
		}
		return null;
	}

	// ---------------後台管理功能----------------------

	// 後台文章列表，不受狀態限制
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/articles")
	public ResponseEntity<Page<ArticleResponse>> getArticlesForAdmin(
			@RequestParam(required = false) Article.ArticleStatus status,
			@RequestParam(required = false) Integer categoryId,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long authorId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		Page<ArticleResponse> response = articleService.getArticlesForAdmin(status, categoryId, keyword, authorId,
				page, size);

		return ResponseEntity.ok(response);
	}

	// 後台文章詳情，不受狀態限制
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@GetMapping("/api/admin/articles/{articleId}")
	public ResponseEntity<ArticleResponse> getArticleForAdmin(@PathVariable Long articleId) {

		ArticleResponse response = articleService.getArticleForAdmin(articleId);

		return ResponseEntity.ok(response);
	}

	// 審核／下架／強制處分
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@PatchMapping("/api/admin/articles/{articleId}/status")
	public ResponseEntity<ArticleResponse> updateArticleStatus(
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleStatusRequest request) {

		ArticleResponse response = articleService.updateArticleStatus(articleId, request);

		return ResponseEntity.ok(response);
	}

	// 置頂／取消置頂
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@PatchMapping("/api/admin/articles/{articleId}/pin")
	public ResponseEntity<ArticleResponse> updateArticlePin(
			@PathVariable Long articleId,
			@Valid @RequestBody UpdatePinRequest request) {

		ArticleResponse response = articleService.updateArticlePin(articleId, request.getIsPinned());

		return ResponseEntity.ok(response);
	}
}
