package com.gigafix.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.gigafix.admin.service.AdminUserDetailsService;
import com.gigafix.common.security.RestAccessDeniedHandler;
import com.gigafix.common.security.RestAuthEntryPoint;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final AdminUserDetailsService adminUserDetailsService;
	private final RestAccessDeniedHandler restAccessDeniedHandler;
	private final RestAuthEntryPoint restAuthEntryPoint;
	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity)throws Exception{
		return httpSecurity
				.securityMatcher("/admin/account/**", "/adminlogin","/adminlogout") //只針對某些請求路徑作用，之後要把/manager拿掉改成/admin/**
//				.cors(null) //因為前端先用vite做反向代理，所以根本不會觸發cros因此先不寫
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/adminlogin","/adminlogout").permitAll()  // 不需要登入，但享有Security的保護
						.requestMatchers("/admin/account/me/**").hasAnyAuthority("ROLE_DEPUTY_ADMIN", "ROLE_SUPER_ADMIN")
						.requestMatchers("/admin/account/**").hasAuthority("ROLE_SUPER_ADMIN")
						.requestMatchers("/admin/**").hasAnyAuthority("ROLE_DEPUTY_ADMIN","ROLE_SUPER_ADMIN")
//						.requestMatchers("/admin/product/**","/admin/order/**").hasAnyAuthority("ROLE_ECOMMERCE_ADMIN")
//						.requestMatchers("/admin/forum/**").hasAuthority("ROLE_FORUM_ADMIN")
//						.requestMatchers("/admin/repair/**").hasAuthority("ROLE_REPAIR_ADMIN")
						)
				.sessionManagement(session -> session //session-based 認證的核心設定
						// IF_REQUIRED = 預設值，有需要時才建立 session（例如登入成功時)
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.exceptionHandling(ex -> ex //filter所拋出的兩大錯誤沒辦法用@ExceptionHandler抓，必須複寫他的兩個抓錯誤的介面
						.authenticationEntryPoint(restAuthEntryPoint) // 未登入 → 401，在自訂一個EntryPoint裡面
						.accessDeniedHandler(restAccessDeniedHandler)) // 已登入但權限不足 → 403
				 // 因為自己手寫 login/logout Controller，所以這裡不需要 .formLogin()
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
