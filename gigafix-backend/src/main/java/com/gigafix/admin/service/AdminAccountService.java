package com.gigafix.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.admin.dto.AdminCreateReq;
import com.gigafix.admin.dto.AdminInfoDto;
import com.gigafix.admin.dto.UpdateOwnPasswordReq;
import com.gigafix.admin.entity.AdminAccount;
import com.gigafix.admin.entity.Role;
import com.gigafix.admin.repository.AdminAccountRepository;
import com.gigafix.admin.security.AdminUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class AdminAccountService {
	private final AdminAccountRepository adminRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecurityContextRepository securityContextRepository;
	
	public AdminInfoDto createAdmin(AdminCreateReq req) {
		AdminAccount adminCreated = adminRepository.save(AdminAccount.builder()
				.adminAccountName(req.adminName())
				.adminAccountPassword(passwordEncoder.encode(req.password())) //要編碼才可以儲存
				.adminRole(req.adminRole())
				.adminAccountCreateTime(LocalDateTime.now()).build());
		return AdminInfoDto.builder()
				.adminId(adminCreated.getAdminAccountId())
				.adminName(adminCreated.getAdminAccountName())
				.adminRole(adminCreated.getAdminRole().getRoleName().name())
				.createDateTime(adminCreated.getAdminAccountCreateTime()).build();
	}
	
	//查全部
    public List<AdminInfoDto> getAllAccounts() {
        return adminRepository.findAll().stream()
                .map(account -> toResp(account))
                .toList();
    }
	
	//更新自己的名稱(把驗證掛到SecurityContextHolder這個抽成工具來用，因為login也有用到)
    public AdminInfoDto updateupdateMyName(Integer id, String adminName,
            HttpServletRequest request, HttpServletResponse response) {
        AdminAccount account = adminRepository.findById(id).orElseThrow(() -> new RuntimeException(""));

        account.setAdminAccountName(adminName);
        AdminAccount saved = adminRepository.save(account);
        
        UserDetails updatedUserDetails = new AdminUserDetails(saved);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                updatedUserDetails,
                null,
                updatedUserDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        
        return toResp(saved);
    }
    
    public void updateOwnPassword(Integer adminId, UpdateOwnPasswordReq req) {
        AdminAccount account = adminRepository.findById(adminId).orElseThrow(() ->new RuntimeException(""));

        if (!passwordEncoder.matches(req.oldPassword(), account.getAdminAccountPassword())) {
            throw new RuntimeException("舊密碼錯誤"); //自訂錯誤
        }
        account.setAdminAccountPassword(passwordEncoder.encode(req.newPassword()));
        adminRepository.save(account);
    }

    public void resetPassword(Integer id, String newPassword) {
        AdminAccount account = adminRepository.findById(id).orElseThrow(() -> new RuntimeException(""));
        // 這裡不用驗證舊密碼——因為是「總管理員幫別人重設」，不是「使用者自己改」
        account.setAdminAccountPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(account);
    }
    
    public AdminInfoDto updateRole(Integer id, Role role) {
        AdminAccount account = adminRepository.findById(id).orElseThrow(() -> new RuntimeException(""));
        
        account.setAdminRole(role);
        AdminAccount saved = adminRepository.save(account);
        return toResp(saved);
    }
    
    
    //刪除帳號
    public void deleteAccount(Integer id) {
    	adminRepository.findById(id).orElseThrow(() -> new RuntimeException()); //要自訂義錯誤
    	adminRepository.deleteById(id);
    }
	
    //把admin物件轉成可以給前端顯示的info
    private AdminInfoDto toResp(AdminAccount account) {
        return AdminInfoDto.builder()
        		.adminId(account.getAdminAccountId())
                .adminName(account.getAdminAccountName())
                .adminRole(account.getAdminRole().getRoleName().name())
                .createDateTime(account.getAdminAccountCreateTime())
                .build();
    }
	
}
