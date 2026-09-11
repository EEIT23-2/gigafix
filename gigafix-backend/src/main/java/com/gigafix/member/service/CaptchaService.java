package com.gigafix.member.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.gigafix.member.dto.RecaptchaResponse;
import com.gigafix.member.exception.InvalidCaptchaException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CaptchaService {
	private final RestTemplate restTemplate; // 沿用RestConfig.java已經註冊好的那顆bean

	@Value("${recaptcha.secret-key}")
	private String secretKey;

	// 驗證前端傳來的captchaToken是不是Google認可的合法token，失敗直接丟例外，呼叫端不用另外寫if判斷
	public void verify(String captchaToken) {
		String url = "https://www.google.com/recaptcha/api/siteverify"
				+ "?secret=" + secretKey
				+ "&response=" + captchaToken;
		RecaptchaResponse response = restTemplate.postForObject(url, null, RecaptchaResponse.class);
		if (response == null || !response.success()) {
			throw new InvalidCaptchaException();
		}
	}
}
