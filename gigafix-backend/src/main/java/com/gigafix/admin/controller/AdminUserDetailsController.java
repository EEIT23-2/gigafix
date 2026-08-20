package com.gigafix.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.admin.dto.AdminLoginReq;
import com.gigafix.admin.dto.AdminLoginResp;
import com.gigafix.admin.security.AdminUserDetails;
import com.gigafix.admin.security.AdminSecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AdminUserDetailsController {
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	
	@PostMapping("/adminlogin") //因為是前後端分離專案，前端回傳Json，spring security的預設登入formLogin()不讀取Json，所以不寫Json轉換器的話只能自己寫login
	public ResponseEntity<AdminLoginResp> adminLogin(@RequestBody AdminLoginReq adminLoginReq, HttpServletRequest req, HttpServletResponse resp) {
		//Spring security的方法會做登入認證，自動呼叫我自己在AdminUserDetailsService寫的loadUserByUsername() + 密碼比對
		//這裡回傳的authenticate裡面有包著我在loadUserByUsername()回傳的AdminUserDetails物件，所以自帶admin所有的資訊
		//尚未認證時new UsernamePasswordAuthenticationToken(使用者名稱, 使用者密碼)
		Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(adminLoginReq.userName(), adminLoginReq.password()));
		//回傳的Authentication裡面包含→Principal(可以拿到UserDetails物件)、Authorities(權限)、Credentials密碼、認證後結果
		
		if (req.getSession(false) == null) {
		    req.getSession(true); // 確保 session 存在，不然會報錯
		}
		req.changeSessionId(); //代替spring security透過formLogin時會做的抽換session id，因為不想寫SessionAuthenticationStrategy
		
		SecurityContext context = SecurityContextHolder.createEmptyContext(); //創建一個SecurityContext，該物件專們用來裝Authentication
		context.setAuthentication(authentication); //把登入驗證後使用者所有資訊放進這個context內，回傳值是void不能用方法鍊
		SecurityContextHolder.setContext(context);  //把這個context掛在SecurityContextHolder，如果這次的請求期間有其他人要用使用者資訊可以從SecurityContextHolder抓
		securityContextRepository.saveContext(context, req, resp);
		//把context物件(裡面裝使用者資訊)設在session內  (HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY : context)
		
		AdminUserDetails userDetails = AdminSecurityUtils.getCurrentAdmin(authentication);
		return ResponseEntity.ok(AdminLoginResp.builder()
				.name(userDetails.getName())
				.role(userDetails.getRole())
				.build());
	}
	
	@PostMapping("/adminlogout")
	public ResponseEntity<?> logout(HttpServletRequest request) {
	    HttpSession session = request.getSession(false);
	    if (session != null) {
	        session.invalidate();
	    }
	    SecurityContextHolder.clearContext();
	    return ResponseEntity.ok().build();
	}
	
}
