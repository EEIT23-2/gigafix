package com.gigafix.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.gigafix.admin.service.AdminUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final AdminUserDetailsService adminUserDetailsService;
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)throws Exception{
		return httpSecurity
				.securityMatcher("/admin/manager/**", "/adminlogin") //只針對某些請求路徑作用，之後要把/manager拿掉改成/admin/**
//				.cors(null)
				//因為前端先用vite做反向代理，所以根本不會觸發crsf跟cros因此先不寫
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/adminlogin","/adminlogout").permitAll()  // 不需要登入，但享有Security的保護
//						.requestMatchers("/admin/manager/**").hasAuthority("ROLE_SUPER_ADMIN")
						.requestMatchers("/admin/**").hasAnyAuthority("ROLE_DEPUTY_ADMIN","ROLE_SUPER_ADMIN")
//						.requestMatchers("/admin/product/**","/admin/order/**").hasAnyAuthority("ROLE_ECOMMERCE_ADMIN")
//						.requestMatchers("/admin/forum/**").hasAuthority("ROLE_FORUM_ADMIN")
//						.requestMatchers("/admin/repair/**").hasAuthority("ROLE_REPAIR_ADMIN")
						)
				.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
	    return new BCryptPasswordEncoder(); // 先給一個預設能動的版本，晚點要換演算法再改
	}
	
	@Bean
	public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository(); // 把這個設定Spring security Context的工具註冊成 Bean來使用
    }
	
	@Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = 
		        http.getSharedObject(AuthenticationManagerBuilder.class);
	    
	    authenticationManagerBuilder
	        .userDetailsService(adminUserDetailsService)
	        .passwordEncoder(passwordEncoder()); //回傳值不是AuthenticationManagerBuilder，只能分開寫
	    
	    return authenticationManagerBuilder.build();
    }
	
}
