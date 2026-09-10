package com.gigafix.common.util;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
	private final int expireTime; // 單位是秒，值來自application.properties的jwt.expire_time

	private final SecretKey key;

	public JwtUtils(
			// @Value把application.properties裡對應key的值注入進來，這裡不用寫死1800這種魔術數字，改properties就能全域生效
			@Value("${jwt.expire_time}") int expireTime,
			@Value("${jwt.secret}") String secret) {
		this.expireTime = expireTime;
		// 加密properties裡面的key
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	// 統一在這裡發JWT並組成cookie，login/register/JWT filter滑動過期都共用同一份設定，避免各處各自寫一份對不起來
	public ResponseCookie createTokenCookie(Long memberId) { // 在給前端時id要是String避免前端的number把long搞爛
		String jwt = Jwts.builder().claim("memberId", String.valueOf(memberId)).issuedAt(Date.from(Instant.now()))
				.expiration(Date.from(Instant.now().plus(expireTime, ChronoUnit.SECONDS))).signWith(key).compact();
		return ResponseCookie.from("token", jwt)
				.httpOnly(true)
				.secure(true)
				.sameSite("None") // 允許跨網域帶cookie
				.path("/")
				// maxAge()要吃的參數型別是Duration(一段時間長度)，不是單純的數字，expireTime只是一個int(單位是秒)，所以要用Duration.ofSeconds(...)把它包成Duration物件才能傳進去
				// (Duration類別也有ofMinutes/ofHours/ofDays等工廠方法，看你手上的數字單位是什麼就用對應的那個)
				.maxAge(Duration.ofSeconds(expireTime))
				.build();
	}

	// 驗證使用者傳來的 JWT 是不是合法的
	public boolean validateToken(String token) {
		try {
			Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// 從 JWT 取出所有的Claims
	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

	// 從 JWT 的Claims取出裡面的使用者的 member id(進來的token是String要轉回Long)
	public Long extractMemberId(String token) {
		String stringId = extractAllClaims(token).get("memberId", String.class);
		return Long.parseLong(stringId);
	}

}
