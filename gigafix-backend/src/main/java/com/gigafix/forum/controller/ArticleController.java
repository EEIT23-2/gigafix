package com.gigafix.forum.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gigafix.common.util.ImageValidator;
import com.gigafix.forum.dto.ArticleResponse;
import com.gigafix.forum.dto.CreateArticleRequest;
import com.gigafix.forum.dto.CreateFloorRequest;
import com.gigafix.forum.dto.UpdateArticleRequest;
import com.gigafix.forum.dto.UpdateArticleStatusRequest;
import com.gigafix.forum.dto.UpdateFloorRequest;
import com.gigafix.forum.dto.UpdatePinRequest;
import com.gigafix.forum.dto.ForumImageResponse;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.exception.ForumException;
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

	// 圖片上傳用。共用的 Bean（common/config/CloudinaryConfig），憑證由 CLOUDINARY_URL 環境變數帶入
	private final Cloudinary cloudinary;

	// 討論區的圖片獨立一個資料夾，方便日後在 Cloudinary 後台按模組盤點
	private static final String IMAGE_FOLDER = "gigafix/forum";

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

	// 最近瀏覽（公開）：依 id 批次帶回根文章，刻意不加瀏覽數。
	// 寫在 /api/articles/{articleId} 前面純粹是好讀——Spring 的字面路徑本來就優先於路徑變數
	@GetMapping("/api/articles/recent")
	public ResponseEntity<List<ArticleResponse>> getRecentArticles(
			@RequestParam List<Long> ids,
			Authentication authentication) {

		List<ArticleResponse> responses = articleService.getArticlesByIds(ids, currentMemberId(authentication));

		return ResponseEntity.ok(responses);
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

	// 圖片上傳，回傳可直接使用的 HTTPS 網址
	//
	// 內文插圖與封面圖共用這一支。上傳跟「某一篇文章」無關——它沒有 articleId，
	// 而且建立流程裡使用者可能在草稿還沒被自動存檔建立之前就先插圖，所以做不成
	// /articles/{id}/images，路徑改放在 /api/members/me/forum/images。
	//
	// 權限：路徑收在 /api/members/** 底下，由 SecurityConfig 的 member filter chain
	// 以 anyRequest().authenticated() 要求有效的會員 JWT。刻意「不」放在 /api/articles/**，
	// 那整段在 MemberPublicApiPaths 是完全公開的，放進去會變成任何人都能匿名丟檔案。
	//
	// 檔案大小上限由 application.properties 的 spring.servlet.multipart.max-file-size 控制（10MB），
	// 超過時由 common 的 GlobalExceptionHandler 回 413，這裡不用再擋一次。
	@PostMapping(value = "/api/members/me/forum/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ForumImageResponse> uploadImage(@RequestParam("file") MultipartFile file) {

		if (file == null || file.isEmpty()) {
			throw ForumException.badRequest("請選擇圖片檔案");
		}

		byte[] bytes = readBytes(file);

		if (!ImageValidator.isSupportedImage(bytes)) {
			throw ForumException.unsupportedMediaType("只支援 JPG／PNG／GIF／WebP 圖片");
		}

		// 先確認憑證齊全再打遠端 API，否則 SDK 會丟 IllegalArgumentException，
		// 而 ForumExceptionHandler 把 IllegalArgumentException 一律對應成 404，訊息完全不對
		if (cloudinary.config.cloudName == null
				|| cloudinary.config.apiKey == null
				|| cloudinary.config.apiSecret == null) {
			throw ForumException.serviceUnavailable("圖片服務尚未設定，請聯繫管理員");
		}

		try {
			Map<?, ?> uploadResult = cloudinary.uploader().upload(
					bytes,
					ObjectUtils.asMap("folder", IMAGE_FOLDER, "resource_type", "image"));

			return ResponseEntity.ok(new ForumImageResponse((String) uploadResult.get("secure_url")));
		} catch (IOException exception) {
			throw ForumException.badGateway("圖片上傳失敗，請確認網路連線後再試一次");
		}
	}

	// MultipartFile.getBytes() 的 IOException 是「連暫存檔都讀不到」，屬於伺服器端故障，
	// 但對呼叫端來說一樣是「這次上傳沒成功」，統一收斂成 502
	private byte[] readBytes(MultipartFile file) {

		try {
			return file.getBytes();
		} catch (IOException exception) {
			throw ForumException.badGateway("圖片讀取失敗，請重新選擇檔案");
		}
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
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@GetMapping("/api/admin/forum/articles")
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
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@GetMapping("/api/admin/forum/articles/{articleId}")
	public ResponseEntity<ArticleResponse> getArticleForAdmin(@PathVariable Long articleId) {

		ArticleResponse response = articleService.getArticleForAdmin(articleId);

		return ResponseEntity.ok(response);
	}

	// 審核／下架／強制處分
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@PatchMapping("/api/admin/forum/articles/{articleId}/status")
	public ResponseEntity<ArticleResponse> updateArticleStatus(
			@PathVariable Long articleId,
			@Valid @RequestBody UpdateArticleStatusRequest request) {

		ArticleResponse response = articleService.updateArticleStatus(articleId, request);

		return ResponseEntity.ok(response);
	}

	// 置頂／取消置頂
	// 權限：路徑收在 /api/admin/forum/** 底下，由 SecurityConfig 要求 ROLE_FORUM_ADMIN
	@PatchMapping("/api/admin/forum/articles/{articleId}/pin")
	public ResponseEntity<ArticleResponse> updateArticlePin(
			@PathVariable Long articleId,
			@Valid @RequestBody UpdatePinRequest request) {

		ArticleResponse response = articleService.updateArticlePin(articleId, request.getIsPinned());

		return ResponseEntity.ok(response);
	}
}
