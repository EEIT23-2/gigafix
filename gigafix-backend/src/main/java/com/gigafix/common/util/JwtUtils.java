package com.gigafix.common.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.gigafix.member.dto.CreateJwtDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;

@Validated
@Component
public class JwtUtils {
	private final int expireTime;
	
	private final SecretKey key;
	
	public JwtUtils(
            @Value("${jwt.expire_time}") int expireTime,
            @Value("${jwt.secret}") String secret) {
        this.expireTime = expireTime;
     //加密properties裡面的key
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
	
	//創建JWT
	public String createToken(@Valid CreateJwtDto createJwtDto) { //在給前端時id要是String避免前端的number把long搞爛
		return Jwts.builder().
				claim("memberName", createJwtDto.membername()).
				claim("memberId", createJwtDto.subject()).
				issuedAt(Date.from(Instant.now())).
				expiration(Date.from(Instant.now().plus(expireTime,ChronoUnit.SECONDS))).
				signWith(key).
				compact();
	}
	
	//以下都在(inteception)要用
	//驗證使用者傳來的 JWT 是不是合法的
	public boolean validateToken(String token) {
		try {
			Jwts.parser().
				verifyWith(key).
				build().
				parseSignedClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}		
	}
	
	//從 JWT 取出所有的Claims
	private Claims extractAllClaims(String token) {
		return Jwts.parser().
				verifyWith(key).
				build().
				parseSignedClaims(token).getPayload();
	}
	
	//從 JWT 的Claims取出裡面的使用者的 member name
	public String extractMemberName(String token) {
		return extractAllClaims(token).get("memberName",String.class);
	}
	//從 JWT 的Claims取出裡面的使用者的 member id(進來的token是String要轉回Long)
		public Long extractMemberId(String token) {
			String stringId = extractAllClaims(token).get("memberId",String.class);
			return Long.parseLong(stringId);
		}
	
	
}
