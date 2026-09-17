package com.gigafix.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary();// 不用參數因為在環境變數設定了CLOUDINARY_URL，這個套件會自動去讀這個key的value
    }
}
