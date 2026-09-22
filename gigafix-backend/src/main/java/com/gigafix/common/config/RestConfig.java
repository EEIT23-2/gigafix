package com.gigafix.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {
    //AI客服另外有一顆有設逾時的groqRestTemplate，容器裡不只一顆RestTemplate，
    //標成@Primary讓原本就靠型別注入的地方(匯率、reCAPTCHA、綠界)維持拿到這顆，不受新增的bean影響
    @Primary
    @Bean  //此class用作 Jack商品管理 呼喚外部轉匯率api用
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
