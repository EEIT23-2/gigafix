package com.gigafix.repair.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class RepairAsyncConfig {

	//repair模組寄信專用的執行緒池，讓RepairNotificationService的@Async方法背景執行，不擋住API主流程
	@Bean(name = "mailTaskExecutor")
	public Executor mailTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("repair-mail-");
		executor.initialize();
		return executor;
	}

}
