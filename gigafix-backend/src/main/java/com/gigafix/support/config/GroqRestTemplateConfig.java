package com.gigafix.support.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// AI 客服專用的 RestTemplate。
// 不共用 RestConfig 那顆，是因為那顆是匯率、reCAPTCHA、綠界在用的，逾時需求跟「等 AI 生成回覆」不一樣，
// 而且那顆完全沒設逾時：Groq 一旦慢回應或連線卡住，執行緒會一直等下去，拖垮整個後端的連線數。
@Configuration
public class GroqRestTemplateConfig {

	// 建立 TCP 連線本來就該很快，連不上就是網路/DNS 有問題，沒必要久等
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	// 讀取要等模型把整段回覆生完，抓得比連線逾時寬鬆很多；
	// 超過就當上游異常，讓前端拿到 502 而不是一直轉圈
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

	@Bean
	public RestTemplate groqRestTemplate() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return new RestTemplate(requestFactory);
	}
}
