package com.gigafix.common.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	//註冊OTP驗證碼的快取名稱，key:email value:otp，只存在記憶體不進資料庫
	public static final String REGISTER_OTP_CACHE = "registerOtpCache";
	//忘記密碼OTP驗證碼的快取名稱，跟註冊OTP分開存放，避免兩種用途的驗證碼互相冒用
	public static final String FORGOT_PASSWORD_OTP_CACHE = "forgotPasswordOtpCache";

	@Bean
	public CaffeineCacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager(REGISTER_OTP_CACHE, FORGOT_PASSWORD_OTP_CACHE);
		cacheManager.setCaffeine(Caffeine.newBuilder()
				.expireAfterWrite(5, TimeUnit.MINUTES) //OTP驗證碼5分鐘後自動失效，逾時要重新寄送
				.maximumSize(10_000));
		return cacheManager;
	}

}
