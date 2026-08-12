package com.gigafix.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {
    @Bean  //此class用作 Jack商品管理 呼喚外部轉匯率api用
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
