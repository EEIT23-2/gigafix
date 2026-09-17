package com.gigafix.forum.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.forum.dto.ForumSeedData;
import com.gigafix.forum.dto.ForumSeedResponse;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Category;
import com.gigafix.forum.entity.Comment;
import com.gigafix.forum.entity.Like;
import com.gigafix.forum.exception.ForumException;
import com.gigafix.forum.repository.ArticleRepository;
import com.gigafix.forum.repository.CategoryRepository;
import com.gigafix.forum.repository.CommentRepository;
import com.gigafix.forum.repository.LikeRepository;
import com.gigafix.forum.util.HtmlSanitizer;
import com.gigafix.member.entity.Member;
import com.gigafix.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 論壇展示資料產生器
 *
 * 刻意不走 ArticleService／CommentService／LikeService，而是直接操作 Repository：
 * 那幾支 service 的規則（只有發布中的文章能蓋樓、同一人不能重複按讚、建立時間一律是現在）
 * 是給真實使用者用的，種子資料要指定過去的時間、一次灌入大量資料，走 service 反而綁手綁腳。
 * 代價是計數欄要自己維護，所以這裡每一筆 like_count／comment_count 都等於實際寫入的列數。
 */
@Service
@RequiredArgsConstructor
public class ForumSeedServiceImpl implements ForumSeedService {

	// 種子檔位置（文案與圖片網址分開放，改圖不用動文案）
	private static final String CONTENT_RESOURCE = "forum-seed/forum-seed.json";
	private static final String IMAGE_RESOURCE = "forum-seed/forum-seed-images.json";

	// 內文裡的圖片佔位符，例如 [[IMG-01]]
	private static final Pattern IMAGE_TOKEN = Pattern.compile("\\[\\[(IMG-\\d+)\\]\\]");

	// 固定亂數種子：作者、按讚人數、時間偏移每次都得到同一組結果，換機器重跑也一樣
	private static final long RANDOM_SEED = 20260916L;

	// 根文章的建立時間散佈在過去這麼多天之內
	private static final int SPREAD_DAYS = 60;

	// 按讚數區間；實際筆數還會受會員總數限制（一人對同一個目標只能按一次）
	private static final int ARTICLE_LIKE_MIN = 3;
	private static final int ARTICLE_LIKE_MAX = 30;
	private static final int FLOOR_LIKE_MAX = 4;
	private static final int COMMENT_LIKE_MAX = 2;

	private final CategoryRepository categoryRepository;
	private final ArticleRepository articleRepository;
	private final CommentRepository commentRepository;
	private final LikeRepository likeRepository;
	private final MemberRepository memberRepository;
	private final ObjectMapper objectMapper;

	// 產生展示資料
	// TODO: 角色系統做好後要加 moderator/admin 權限檢查，目前任何呼叫者都可以執行
	@Override
	@Transactional
	public ForumSeedResponse seedForumData() {

		ForumSeedData seedData = readResource(CONTENT_RESOURCE, new TypeReference<ForumSeedData>() {
		});
		Map<String, String> images = readResource(IMAGE_RESOURCE, new TypeReference<Map<String, String>>() {
		});

		// 重複執行保護：只要種子檔裡任何一篇根文章的標題已經存在，整批就不做
		// 樓層標題是「根標題(N樓)」也會落在 articles，所以這裡限定 parent_article_id IS NULL 才算數
		for (ForumSeedData.SeedArticle seedArticle : seedData.getArticles()) {
			if (articleRepository.existsByTitleAndParentArticleIsNull(seedArticle.getTitle())) {
				throw new IllegalStateException("展示資料已經產生過了，請先刪除舊資料再重新產生");
			}
		}

		// 作者與按讚者都從現有會員抽，不另外建立會員
		List<Member> members = memberRepository.findAll();
		if (members.isEmpty()) {
			throw ForumException.badRequest("目前沒有任何會員，無法產生展示資料");
		}

		Random random = new Random(RANDOM_SEED);
		LocalDateTime now = LocalDateTime.now();

		// 分類：同名的沿用既有那一筆，只有不存在時才新建
		Map<String, Category> categoryByName = new HashMap<>();
		int createdCategoryCount = 0;
		for (String categoryName : seedData.getCategories()) {
			Category category = categoryRepository.findByName(categoryName);
			if (category == null) {
				category = new Category();
				category.setName(categoryName);
				category = categoryRepository.save(category);
				createdCategoryCount++;
			}
			categoryByName.put(categoryName, category);
		}

		int articleCount = 0;
		int floorCount = 0;
		int commentCount = 0;
		int likeCount = 0;

		for (ForumSeedData.SeedArticle seedArticle : seedData.getArticles()) {

			Category category = categoryByName.get(seedArticle.getCategory());
			if (category == null) {
				throw new IllegalStateException("種子檔的分類名稱不在 categories 清單中：" + seedArticle.getCategory());
			}

			List<ForumSeedData.SeedFloor> seedFloors = seedArticle.getFloors();

			// 根文章建立時間：過去 1~SPREAD_DAYS 天內的隨機時間點
			LocalDateTime rootTime = now.minusDays(1L + random.nextInt(SPREAD_DAYS))
					.minusMinutes(random.nextInt(24 * 60));

			// 樓層時間用等分切片往後排：樓層編號是依 articleCreatedTime 正序現算的（沒有落地欄位），
			// 時間必須嚴格遞增，前台看到的樓層順序才會跟種子檔寫的一致
			long spanMinutes = Math.max(Duration.between(rootTime, now).toMinutes(), seedFloors.size() + 2L);
			long stepMinutes = Math.max(1L, spanMinutes / (seedFloors.size() + 2L));

			Member rootAuthor = members.get(random.nextInt(members.size()));
			List<Member> rootLikers = pickMembers(members,
					ARTICLE_LIKE_MIN + random.nextInt(ARTICLE_LIKE_MAX - ARTICLE_LIKE_MIN + 1), random);

			Article root = new Article();
			root.setCategory(category);
			root.setAuthor(rootAuthor);
			root.setTitle(seedArticle.getTitle());
			root.setContent(resolveContent(seedArticle.getContent(), images));
			root.setCoverImage(resolveImageUrl(seedArticle.getCoverImage(), images));
			root.setStatus(resolveStatus(seedArticle.getStatus()));
			root.setIsPinned(seedArticle.isPinned());
			root.setLikeCount(rootLikers.size());
			root.setCommentCount(seedArticle.getComments().size());
			root.setViewCount(calculateViewCount(rootLikers.size(), seedFloors.size(),
					seedArticle.getComments().size(), random));
			root.setArticleCreatedTime(rootTime);
			root = articleRepository.save(root);
			articleCount++;

			likeCount += createArticleLikes(root, rootLikers, rootTime, now, random);

			int[] rootComments = createComments(root, seedArticle.getComments(), members, rootTime, now, random);
			commentCount += rootComments[0];
			likeCount += rootComments[1];

			Member previousAuthor = rootAuthor;
			for (int i = 0; i < seedFloors.size(); i++) {

				ForumSeedData.SeedFloor seedFloor = seedFloors.get(i);
				LocalDateTime floorTime = rootTime.plusMinutes(stepMinutes * (i + 1L));
				Member floorAuthor = pickAuthorOtherThan(members, previousAuthor, random);
				List<Member> floorLikers = pickMembers(members, random.nextInt(FLOOR_LIKE_MAX + 1), random);

				Article floor = new Article();
				floor.setCategory(category); // 樓層繼承根文章分類，與 ArticleServiceImpl.createFloor 一致
				floor.setAuthor(floorAuthor);
				floor.setParentArticle(root);
				floor.setTitle(root.getTitle() + "(" + (i + 2) + "樓)"); // 根文章算 1 樓
				floor.setContent(resolveContent(seedFloor.getContent(), images));
				floor.setStatus(Article.ArticleStatus.PUBLISHED); // 樓層不走草稿
				floor.setIsPinned(false);
				floor.setLikeCount(floorLikers.size());
				floor.setCommentCount(seedFloor.getComments().size());
				floor.setViewCount(0); // 樓層沒有自己的瀏覽頁，瀏覽數固定 0
				floor.setArticleCreatedTime(floorTime);
				floor = articleRepository.save(floor);
				floorCount++;

				likeCount += createArticleLikes(floor, floorLikers, floorTime, now, random);

				int[] floorComments = createComments(floor, seedFloor.getComments(), members, floorTime, now, random);
				commentCount += floorComments[0];
				likeCount += floorComments[1];

				previousAuthor = floorAuthor;
			}
		}

		return ForumSeedResponse.builder()
				.categoryCount(createdCategoryCount)
				.articleCount(articleCount)
				.floorCount(floorCount)
				.commentCount(commentCount)
				.likeCount(likeCount)
				.build();
	}

	// 建立某篇文章（或樓層）的留言與留言的讚
	// 回傳 [建立的留言數, 建立的讚數]：兩個數字都要往外累加，用長度 2 的陣列比多開一個小類別省事
	private int[] createComments(Article article, List<String> contents, List<Member> members,
			LocalDateTime from, LocalDateTime to, Random random) {

		int createdComments = 0;
		int createdLikes = 0;

		for (String content : contents) {

			List<Member> likers = pickMembers(members, random.nextInt(COMMENT_LIKE_MAX + 1), random);
			LocalDateTime commentTime = randomTimeBetween(from, to, random);

			Comment comment = new Comment();
			comment.setArticle(article);
			comment.setAuthor(members.get(random.nextInt(members.size())));
			comment.setContent(content); // 留言是純文字，不需要淨化 HTML
			comment.setStatus(Comment.CommentStatus.VISIBLE);
			comment.setLikeCount(likers.size());
			comment.setCommentCreatedTime(commentTime);
			Comment savedComment = commentRepository.save(comment);
			createdComments++;

			for (Member liker : likers) {
				Like like = new Like();
				like.setMember(liker);
				like.setComment(savedComment);
				like.setLikeCreatedTime(randomTimeBetween(commentTime, to, random));
				likeRepository.save(like);
				createdLikes++;
			}
		}

		return new int[] { createdComments, createdLikes };
	}

	// 建立文章／樓層的讚，回傳建立的筆數
	private int createArticleLikes(Article article, List<Member> likers, LocalDateTime from, LocalDateTime to,
			Random random) {

		for (Member liker : likers) {
			Like like = new Like();
			like.setMember(liker);
			like.setArticle(article);
			like.setLikeCreatedTime(randomTimeBetween(from, to, random));
			likeRepository.save(like);
		}

		return likers.size();
	}

	// 從會員清單抽出不重複的 count 位；likes 有 (member_id, article_id, comment_id) 唯一約束，同一人不能重複按
	private List<Member> pickMembers(List<Member> members, int count, Random random) {

		int size = Math.min(count, members.size());
		if (size <= 0) {
			return List.of();
		}

		List<Member> shuffled = new ArrayList<>(members);
		Collections.shuffle(shuffled, random);
		return new ArrayList<>(shuffled.subList(0, size));
	}

	// 挑一位跟上一樓不同的作者，讓一串看起來像多人對話；只有一位會員時就只能是同一人
	private Member pickAuthorOtherThan(List<Member> members, Member previous, Random random) {

		if (members.size() == 1) {
			return members.get(0);
		}

		Member candidate;
		do {
			candidate = members.get(random.nextInt(members.size()));
		} while (candidate.getId().equals(previous.getId()));

		return candidate;
	}

	// 取 from 與 to 之間的隨機時間點：留言與按讚都要晚於所屬文章、早於現在
	private LocalDateTime randomTimeBetween(LocalDateTime from, LocalDateTime to, Random random) {

		long span = Duration.between(from, to).toMinutes();
		if (span <= 1) {
			return to;
		}

		return from.plusMinutes(1L + (long) (random.nextDouble() * (span - 1)));
	}

	// 瀏覽數：依熱度推估，樓層、留言、讚越多瀏覽數越高，結果必定大於讚數
	private int calculateViewCount(int likes, int floors, int comments, Random random) {

		return 80 + likes * 12 + floors * 25 + comments * 8 + random.nextInt(600);
	}

	// 把內文裡的 [[IMG-xx]] 換成實際圖片，再過一次淨化（與使用者發文走同一套規則）
	// 對應網址是空字串時整個佔位符移除，不會留下沒有 src 的壞圖
	private String resolveContent(String content, Map<String, String> images) {

		Matcher matcher = IMAGE_TOKEN.matcher(content);
		StringBuilder resolved = new StringBuilder();
		while (matcher.find()) {
			String url = images.get(matcher.group(1));
			String replacement = (url == null || url.isBlank()) ? "" : "<p><img src=\"" + url + "\"></p>";
			matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(resolved);

		return HtmlSanitizer.clean(resolved.toString());
	}

	// 封面圖：種子檔存的是圖片代號，沒填或對應網址是空的就不放封面
	private String resolveImageUrl(String imageKey, Map<String, String> images) {

		if (imageKey == null || imageKey.isBlank()) {
			return null;
		}

		String url = images.get(imageKey);
		return (url == null || url.isBlank()) ? null : url;
	}

	// 狀態：種子檔沒寫就是發布中
	private Article.ArticleStatus resolveStatus(String status) {

		return (status == null || status.isBlank()) ? Article.ArticleStatus.PUBLISHED
				: Article.ArticleStatus.valueOf(status);
	}

	// 讀種子檔。檔案是跟著 jar 打包的資源，讀不到代表建置有問題，不是使用者操作錯誤，所以不轉成 4xx
	private <T> T readResource(String path, TypeReference<T> type) {

		try (InputStream input = new ClassPathResource(path).getInputStream()) {
			return objectMapper.readValue(input, type);
		} catch (IOException e) {
			throw new UncheckedIOException("讀取展示資料種子檔失敗：" + path, e);
		}
	}
}
