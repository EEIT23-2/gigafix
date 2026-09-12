package com.gigafix.common.config;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.beans.factory.annotation.Value;

import com.gigafix.admin.service.AdminUserDetailsService;
import com.gigafix.common.dto.ErrorResp;
import com.gigafix.common.security.RestAccessDeniedHandler;
import com.gigafix.common.security.RestAuthEntryPoint;
import com.gigafix.common.util.JwtUtils;
import com.gigafix.common.util.MemberPublicApiPaths;
import com.gigafix.member.security.MemberJwtAuthenticationFilter;
import com.gigafix.member.service.MemberUserDetailsService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final AdminUserDetailsService adminUserDetailsService;
	private final MemberUserDetailsService memberUserDetailsService;
	private final RestAccessDeniedHandler restAccessDeniedHandler;
	private final RestAuthEntryPoint restAuthEntryPoint;
	private final ObjectMapper objectMapper;
	private final JwtUtils jwtUtils;

	@Bean
	@Order(1)
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
		return httpSecurity
				.securityMatcher("/api/admin/account/**", "/api/adminlogin", "/api/adminlogout") // 只針對某些請求路徑作用，之後要把/manager拿掉改成/admin/**
				// .cors(null) //因為前端先用vite做反向代理，所以根本不會觸發cros因此先不寫
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/api/adminlogin", "/api/adminlogout", "/api/admin/account/super-admin")
						.permitAll() // 不需要登入，但享有Security的保護
						.requestMatchers("/api/admin/account/me", "/api/admin/account/me/**")
						.hasAnyAuthority("ROLE_REPAIR_ADMIN", "ROLE_FORUM_ADMIN", "ROLE_ECOMMERCE_ADMIN",
								"ROLE_DEPUTY_ADMIN", "ROLE_SUPER_ADMIN")
						.requestMatchers("/api/admin/account/**").hasAuthority("ROLE_SUPER_ADMIN")
						.requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_DEPUTY_ADMIN", "ROLE_SUPER_ADMIN")
				// .requestMatchers("/admin/product/**","/admin/order/**").hasAnyAuthority("ROLE_ECOMMERCE_ADMIN")
				// .requestMatchers("/admin/forum/**").hasAuthority("ROLE_FORUM_ADMIN")
				// .requestMatchers("/admin/repair/**").hasAuthority("ROLE_REPAIR_ADMIN")
				)
				.sessionManagement(session -> session // session-based 認證的核心設定
						.maximumSessions(1) // 可選：限制同一使用者同時只能有一個 session
						.sessionRegistry(sessionRegistry())// IF_REQUIRED = 預設值，有需要時才建立 session（例如登入成功時)
						.expiredSessionStrategy(event -> { // 自訂策略丟出錯誤
							HttpServletResponse response = event.getResponse();
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
							response.setContentType("application/json;charset=UTF-8");

							ErrorResp errorResp = ErrorResp.builder()
									.errorCode("SESSION_EXPIRED")
									.message("此帳號已於其他裝置登入，請重新登入")
									.build();

							response.getWriter().write(objectMapper.writeValueAsString(errorResp));
						}))
				.exceptionHandling(ex -> ex // filter所拋出的兩大錯誤沒辦法用@ExceptionHandler抓，必須複寫他的兩個抓錯誤的介面
						.authenticationEntryPoint(restAuthEntryPoint) // 未登入 → 401，在自訂一個EntryPoint裡面
						.accessDeniedHandler(restAccessDeniedHandler)) // 已登入但權限不足 → 403
				// 因為自己手寫 login/logout Controller，所以這裡不需要 .formLogin()
				.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain memberSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
		MemberJwtAuthenticationFilter memberJwtAuthenticationFilter = new MemberJwtAuthenticationFilter(jwtUtils,
				memberUserDetailsService);

		return httpSecurity
				.securityMatcher("/api/gigafix/**", "/api/articles/**", "/api/members/**", "/api/categories",
						"/api/categories/**", "/api/repairs/appointment", "/api/repairs/me",
						"/api/repairs/*/approval", "/api/repairs/*/pickup-payment", "/api/repairs/*/ecpay-payment")
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(MemberPublicApiPaths.PATHS)
						.permitAll()
						.anyRequest().authenticated()// 因為member沒有做權限設計，所以統一其他的有認證過就可以請求
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))// 明確表示這條filter的session政策是無狀態
				.exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler))
				.addFilterBefore(memberJwtAuthenticationFilter, BasicAuthenticationFilter.class).build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(); // 先給一個預設能動的版本，晚點要換演算法再改
	}

	@Bean
	public SessionRegistry sessionRegistry() { // 註冊spring secrity內建的session管理工具
		return new SessionRegistryImpl();
	}

	// 這個 Listener 是必要的，確保 session 銷毀時 SessionRegistry 也同步更新
	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
		// JSESSIONID 對應到哪個 session 物件的map tomcat(Servlet 容器)本來就有
		// 這個Bean會去讀裡面的的內容，也可以用這個工具設定Spring security Context
		// SecurityContextHolderFilter的驗證機制也會透過這個工具去查前端傳來的Jsesssionid否有在tomcat的管理中
	}

	// Spring Security內部組裝HttpSecurity時會找一個全域預設的AuthenticationManager，
	// 這裡沒有標會因為存在2個AuthenticationManager bean而啟動失敗；
	// 實際登入都是Controller自己用@Qualifier指定要用哪個AuthenticationManager.authenticate()
	// 注意：一定要手動把parent設成null！不設的話Spring會把@Primary的這顆bean自己
	// 拿去當全域AuthenticationManager，變成兩顆bean(包括自己)的parent，
	// 一旦某次認證失敗要往parent問，就會繞回自己造成無限遞迴、StackOverflowError
	@Bean
	@Primary
	public AuthenticationManager adminAuthenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);

		authenticationManagerBuilder
				.userDetailsService(adminUserDetailsService)
				.passwordEncoder(passwordEncoder()); // 回傳值不是AuthenticationManagerBuilder，只能分開寫
		authenticationManagerBuilder.parentAuthenticationManager(null); // 禁止往parent查，避免上述的無限遞迴

		return authenticationManagerBuilder.build();
	}

	// 同上，避免落到同一個全域parent造成無限遞迴
	@Bean
	public AuthenticationManager memberAuthenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);

		authenticationManagerBuilder
				.userDetailsService(memberUserDetailsService)
				.passwordEncoder(passwordEncoder()); // 這個是專門給member驗證的AuthenticationManager
		authenticationManagerBuilder.parentAuthenticationManager(null);

		return authenticationManagerBuilder.build();
	}

	// 註冊Google帳號快速登入的工具
	@Bean
	public GoogleIdTokenVerifier googleIdTokenVerifier(@Value("${google.client-id}") String clientId) {
		return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(Collections.singletonList(clientId))
				.build();
	}

}