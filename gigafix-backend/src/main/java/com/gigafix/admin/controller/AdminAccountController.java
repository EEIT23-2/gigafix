package com.gigafix.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gigafix.admin.dto.AdminCreateReq;
import com.gigafix.admin.dto.AdminInfoDto;
import com.gigafix.admin.dto.DeleteAdminReq;
import com.gigafix.admin.dto.ResetPasswordReq;
import com.gigafix.admin.dto.SuperAdminSetupReq;
import com.gigafix.admin.dto.UpdateMeNameReq;
import com.gigafix.admin.dto.UpdateOwnPasswordReq;
import com.gigafix.admin.dto.UpdateRoleReq;
import com.gigafix.admin.security.AdminSecurityUtils;
import com.gigafix.admin.security.AdminUserDetails;
import com.gigafix.admin.service.AdminAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/admin/account")
public class AdminAccountController {
	private final AdminAccountService accountService;
	
	@PostMapping("/super-admin")
    public ResponseEntity<AdminInfoDto> setupSuperAdmin(@Valid @RequestBody SuperAdminSetupReq req) {
        return ResponseEntity.ok(accountService.setupSuperAdmin(req));
    }
	
	@PostMapping
	@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") //檢查該登入帳號的權限是否為總管理員,'ROLE_SUPER_ADMIN'必須用單引號，不能用雙引號
	public ResponseEntity<AdminInfoDto> creatAdmin(@Valid @RequestBody AdminCreateReq req){
		return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAdmin(req)); //201
	}
	
	@GetMapping
	@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
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
        		.role(currentAdmin.getRole())
        		.createDateTime(currentAdmin.getCreatedTime())
        		.build());
    }
	
	//總管理員幫某個管理員重設的帳密(被改的人要重新登入才會變)
	@PatchMapping("/password")
	@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
	public ResponseEntity<AdminInfoDto> resetPassword(@Valid @RequestBody ResetPasswordReq resetPasswordReq) {
	    return ResponseEntity.ok(accountService.resetPassword(resetPasswordReq.id(), resetPasswordReq.newPassword()));
	}
	
	//總管理員改某個管理員的權限(權限只有總管理員可以改)
	@PatchMapping("/role")
	@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
	public ResponseEntity<AdminInfoDto> updateRole(@Valid @RequestBody UpdateRoleReq updateRoleReq) {
	    return ResponseEntity.ok(accountService.updateRole(updateRoleReq.id(), updateRoleReq.role()));
	}
	
	//自己的名稱(只有名稱可以改，創建時間跟id不能改)
	@PatchMapping("/me/name")
    public ResponseEntity<AdminInfoDto> updateMeName(Authentication authentication,@Valid @RequestBody UpdateMeNameReq updateMeNameReq) {
		Integer userId = AdminSecurityUtils.getCurrentAdmin(authentication).getId();
        return ResponseEntity.ok(accountService.updateupdateMyName(userId, updateMeNameReq.newName()));
    }
	
	//→ 改自己密碼
	@PatchMapping("/me/password")
    public ResponseEntity<Void> updateMyPassword(Authentication authentication,@Valid @RequestBody UpdateOwnPasswordReq req) {
        Integer userId = AdminSecurityUtils.getCurrentAdmin(authentication).getId();
        accountService.updateOwnPassword(userId, req);
        return ResponseEntity.ok().build(); //200
    }
	
	//總管理員刪除其他管理員
    @DeleteMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteAdmin(@Valid @RequestBody DeleteAdminReq deleteAdminReq) {
    	accountService.deleteAccount(deleteAdminReq);
        return ResponseEntity.noContent().build(); //204
    }
	
	
}
