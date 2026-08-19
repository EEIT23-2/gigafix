package com.gigafix.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gigafix.admin.dto.AdminCreateReq;
import com.gigafix.admin.dto.AdminInfoDto;
import com.gigafix.admin.dto.UpdateOwnPasswordReq;
import com.gigafix.admin.entity.Role;
import com.gigafix.admin.security.AdminSecurityUtils;
import com.gigafix.admin.security.AdminUserDetails;
import com.gigafix.admin.service.AdminAccountService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/account")
public class AdminAccountController {
	private final AdminAccountService accountService;
	
	@PostMapping 
	public ResponseEntity<AdminInfoDto> creatAdmin(AdminCreateReq req){
		return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAdmin(req)); //201
	}
	
	@GetMapping
    public ResponseEntity<List<AdminInfoDto>> getAll() {
        return ResponseEntity.ok(accountService.getAllAccounts()); //200
    }
	
	//查自己/me
	@GetMapping("/me")
    public ResponseEntity<AdminInfoDto> getMyInfo(Authentication authentication) {
        AdminUserDetails currentAdmin = AdminSecurityUtils.getCurrentAdmin(authentication);
        //直接從這次請求的登入認證裡抓登入資訊，從request在登入驗證時手動把authentication掛到SecurityContextHolder，spring MVC就可以把authentication撈出來從參數取得
        return ResponseEntity.ok(AdminInfoDto.builder()
        		.adminId(currentAdmin.getId())
        		.adminName(currentAdmin.getName())
        		.adminRole(currentAdmin.getRoleName())
        		.createDateTime(currentAdmin.getCreatedTime())
        		.build());
    }
	
	//總管理員幫某個管理員重設的帳密(被改的人要重新登入才會變)
	@PatchMapping("/{id}/password")
	public ResponseEntity<Void> resetPassword(@PathVariable Integer id, @RequestBody String newPassword) {
		accountService.resetPassword(id, newPassword);
	    return ResponseEntity.ok().build();
	}
	
	//總管理員改某個管理員的權限→PUT
	@PatchMapping("/{id}/role")
	public ResponseEntity<AdminInfoDto> updateRole(@PathVariable Integer id, @RequestBody Role newRole) {
	    return ResponseEntity.ok(accountService.updateRole(id, newRole));
	}
	
	//自己的名稱(只有名稱可以改，權限只有總管理員可以改，創建時間跟id不能改)
	@PatchMapping("/name")
    public ResponseEntity<AdminInfoDto> updateMeName(Authentication authentication, @RequestBody String adminName
    		, HttpServletRequest request, HttpServletResponse response) {
		Integer userId = AdminSecurityUtils.getCurrentAdmin(authentication).getId();
        return ResponseEntity.ok(accountService.updateupdateMyName(userId, adminName, request ,response));
    }
	
	//me/password → 改自己密碼
	@PatchMapping("/me/password")
    public ResponseEntity<Void> updateMyPassword(Authentication authentication, @RequestBody UpdateOwnPasswordReq req) {
        Integer userId = AdminSecurityUtils.getCurrentAdmin(authentication).getId();
        accountService.updateOwnPassword(userId, req);
        return ResponseEntity.ok().build(); //200
    }
	
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
    	accountService.deleteAccount(id);
        return ResponseEntity.noContent().build(); //204
    }
	
	
}
