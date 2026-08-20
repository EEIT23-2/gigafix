package com.gigafix.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.admin.dto.AdminCreateReq;
import com.gigafix.admin.dto.AdminInfoDto;
import com.gigafix.admin.dto.UpdateOwnPasswordReq;
import com.gigafix.admin.entity.AdminAccount;
import com.gigafix.admin.entity.AdminAccount.Role;
import com.gigafix.admin.exception.AdminAccountNotFoundException;
import com.gigafix.admin.repository.AdminAccountRepository;
import com.gigafix.admin.security.AdminSecurityUtils;
import com.gigafix.admin.security.AdminUserDetails;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class AdminAccountService {
	private final AdminAccountRepository adminRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminSecurityUtils adminSecurityUtils;
	
	public AdminInfoDto createAdmin(AdminCreateReq req) {
		AdminAccount adminCreated = adminRepository.save(AdminAccount.builder()
				.name(req.adminName())
				.password(passwordEncoder.encode(req.password())) //要編碼才可以儲存
				.role(req.role())//創建
				.createTime(LocalDateTime.now()).build());
		return AdminInfoDto.builder()
				.adminId(adminCreated.getId())
				.adminName(adminCreated.getName())
				.role(adminCreated.getRole())
				.createDateTime(adminCreated.getCreateTime()).build();
	}
	
	//查全部
    public List<AdminInfoDto> getAllAccounts() {
        return adminRepository.findAll().stream()
                .map(account -> toResp(account))
                .toList();
    }
	
	//更新自己的名稱(把驗證掛到SecurityContextHolder這個抽成工具來用，因為login也有用到)
    public AdminInfoDto updateupdateMyName(Integer id, String adminName) {
        AdminAccount account = adminRepository.findById(id).orElseThrow(() -> new AdminAccountNotFoundException());

        account.setName(adminName);
        AdminAccount saved = adminRepository.save(account);
        
        //把SecurityContextHolder(請求時從參數上抓的內容)及securityContextRepository(回傳的session)中使用者資訊修改成更新後的認證
        adminSecurityUtils.refreshAuthentication(new AdminUserDetails(saved));
        
        return toResp(saved);
    }
  //更新自己的密碼
    public void updateOwnPassword(Integer adminId, UpdateOwnPasswordReq req) {
        AdminAccount account = adminRepository.findById(adminId).orElseThrow(() ->new AdminAccountNotFoundException());

        if (!passwordEncoder.matches(req.oldPassword(), account.getPassword())) {
            throw new RuntimeException("舊密碼錯誤"); //自訂錯誤
        }
        account.setPassword(passwordEncoder.encode(req.newPassword()));
        AdminAccount saved = adminRepository.save(account);
        adminSecurityUtils.refreshAuthentication(new AdminUserDetails(saved));
    }
    
    //總管理員幫其他管理員重設密碼
    public AdminInfoDto resetPassword(Integer id, String newPassword) {
        AdminAccount account = adminRepository.findById(id).orElseThrow(() -> new AdminAccountNotFoundException());
        // 這裡不用驗證舊密碼——因為是「總管理員幫別人重設」，不是「使用者自己改」
        account.setPassword(passwordEncoder.encode(newPassword));
        adminRepository.save(account);
        return toResp(account);
    }
    
    public AdminInfoDto updateRole(Integer id, Role role) {
        AdminAccount account = adminRepository.findById(id).orElseThrow(() -> new AdminAccountNotFoundException());
        
        account.setRole(role);
        AdminAccount saved = adminRepository.save(account);
        return toResp(saved);
    }
    
    
    //刪除帳號
    public void deleteAccount(Integer id) {
    	adminRepository.findById(id).orElseThrow(() -> new AdminAccountNotFoundException());
    	adminRepository.deleteById(id);
    }
	
    //把admin物件轉成可以給前端顯示的info
    private AdminInfoDto toResp(AdminAccount account) {
        return AdminInfoDto.builder()
        		.adminId(account.getId())
                .adminName(account.getName())
                .role(account.getRole())
                .createDateTime(account.getCreateTime())
                .build();
    }
	
}
