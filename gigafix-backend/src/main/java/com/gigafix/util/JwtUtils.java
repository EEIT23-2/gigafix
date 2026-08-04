package com.gigafix.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gigafix.member.dto.JwtDto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

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
	
	public String createToken(JwtDto jwtDto) {
		return Jwts.builder().
				claim("userName", jwtDto.username()).
				claim("userId", jwtDto.subject()).
				issuedAt(Date.from(Instant.now())).
				expiration(Date.from(Instant.now().plus(expireTime,ChronoUnit.SECONDS))).
				signWith(key).
				compact();
	}
	
	//以下都在(inteception)要用
	//驗證使用者傳來的 JWT 是不是合法的
	//從 JWT 取出裡面的 Subject (使用者的 username)
	// 把 properties 裡面的 JWT 密鑰，轉換成 Java 的 SecretKey (簽名密鑰)
	// 從 JWT 取出裡面的 Expiration (過期時間)
	// 取出 JWT 的所有 Claims
	// JWT 有沒有過期
	
	
}
