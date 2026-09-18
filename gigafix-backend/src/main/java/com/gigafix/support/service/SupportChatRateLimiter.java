package com.gigafix.support.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gigafix.support.exception.SupportChatException;

// 依會員 id 限流，避免被濫用把 Groq 免費額度燒光。
// 直接用 Caffeine 建計數用的 Cache，而不是走 CacheConfig 那套給 @Cacheable 用的 CacheManager——
// 這裡要的是「讀取並遞增」的計數器語意，直接操作 Cache 物件比硬塞進宣告式快取自然。
// 注意：這是單一 container instance 內的記憶體計數，Azure Container Apps 開多副本時不是全域精確限流，
// v1 先接受這個折衷，之後真的需要精準全域限流再考慮換成 Redis 之類的共用儲存。
@Component
public class SupportChatRateLimiter {

	private final int perMinuteLimit;
	private final int perDayLimit;

	private final Cache<Long, AtomicInteger> perMinuteCache = Caffeine.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build();
	private final Cache<Long, AtomicInteger> perDayCache = Caffeine.newBuilder()
			.expireAfterWrite(1, TimeUnit.DAYS)
			.build();

	public SupportChatRateLimiter(
			@Value("${support.chat.rate-limit.per-minute}") int perMinuteLimit,
			@Value("${support.chat.rate-limit.per-day}") int perDayLimit) {
		this.perMinuteLimit = perMinuteLimit;
		this.perDayLimit = perDayLimit;
	}

	// 超過任一個限制就丟例外；沒超過的話兩個計數器都會遞增
	public void checkAndIncrement(Long memberId) {
		int minuteCount = perMinuteCache.get(memberId, id -> new AtomicInteger(0)).incrementAndGet();
		if (minuteCount > perMinuteLimit) {
			throw SupportChatException.rateLimited("發送太頻繁，請稍後再試");
		}

		int dayCount = perDayCache.get(memberId, id -> new AtomicInteger(0)).incrementAndGet();
		if (dayCount > perDayLimit) {
			throw SupportChatException.rateLimited("今日客服訊息已達上限，請明天再試");
		}
	}
}
