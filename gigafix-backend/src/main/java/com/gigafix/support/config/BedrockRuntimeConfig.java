package com.gigafix.support.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class BedrockRuntimeConfig{
	private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(30);
	
	@Bean
	public BedrockRuntimeClient bedrockRuntimeClient(
			@Value("${support.chat.aws-access-key-id}") String accessKeyId,
			@Value("${support.chat.aws-secret-access-key}") String secretAccessKey,
			@Value("${support.chat.aws-region}") String region) {
		if(accessKeyId == null || accessKeyId.isBlank() || secretAccessKey == null || secretAccessKey.isBlank()) {
			throw new IllegalStateException("環境變數 AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY 沒有設定完整，AI 客服無法運作");
		}
		
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
		ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
				.apiCallTimeout(API_CALL_TIMEOUT)
				.build();
		
		return BedrockRuntimeClient.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.overrideConfiguration(overrideConfiguration)
				.build();
	}
	
	//解析回應用Jackson
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}