package com.gigafix.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.admin.dto.AdminCreateReq;
import com.gigafix.admin.dto.AdminInfoDto;
import com.gigafix.admin.dto.SuperAdminSetupReq;
import com.gigafix.admin.dto.UpdateOwnPasswordReq;
import com.gigafix.admin.entity.AdminAccount;
import com.gigafix.admin.entity.AdminAccount.Role;
import com.gigafix.admin.exception.AdminAccountNotFoundException;
import com.gigafix.admin.exception.AdminBusinessRuleCheckException;
import com.gigafix.admin.repository.AdminAccountRepository;
import com.gigafix.admin.security.AdminSecurityUtils;
import com.gigafix.admin.security.AdminUserDetails;
import com.gigafix.admin.security.SuperAdminSetupState;

import lombok.RequiredArgsConstructor;


@Service
@Transactional
@RequiredArgsConstructor
public class AdminAccountService {
	private final AdminAccountRepository adminRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminSecurityUtils adminSecurityUtils;
	private final SessionRegistry sessionRegistry;
	private final SuperAdminSetupState setupState;
	
	//如果沒有總管理員的話要新建一個
	public AdminInfoDto setupSuperAdmin(SuperAdminSetupReq req) {
		if (!setupState.tryLock()) {
			throw new AdminBusinessRuleCheckException("已建立過總管理員");			
		}
		AdminAccount admin = new AdminAccount();
		try {
	        if (adminRepository.existsByRole(Role.ROLE_SUPER_ADMIN)) {
	            throw new AdminBusinessRuleCheckException("總管理員已存在，無法重複建立");
	        }

	        admin.setName(req.name());
	        admin.setPassword(passwordEncoder.encode(req.password()));
	        admin.setRole(Role.ROLE_SUPER_ADMIN);
	        admin.setCreateTime(LocalDateTime.now());

	        adminRepository.save(admin);

	    } catch (RuntimeException e) {
	        setupState.reset(); //如果管理員建立失敗/管理員已經存在，要讓建立過管理員的鎖被重製
	        throw e;
	    }
		
		return toResp(admin);//建立成公會回傳admin info
	}
	
	public AdminInfoDto createAdmin(AdminCreateReq req) {
		/*JVM 保證 enum 常數全域唯一，所以所有admin物件的Role都指向同一個記憶體，所以不會有值相通但記憶體不同問題，所以可以用 ==*/
		if (req.role() == Role.ROLE_SUPER_ADMIN) {
			throw new AdminBusinessRuleCheckException("總管理員不能有第二位");
		}
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
        forceLogout(account.getName());//更改後把人踢下線
        return toResp(account);
    }
    //總管理員更改其他管理員權限
    public AdminInfoDto updateRole(Integer id, Role role) {
        AdminAccount account = adminRepository.findById(id).orElseThrow(() -> new AdminAccountNotFoundException());
        
        account.setRole(role);
        AdminAccount saved = adminRepository.save(account);
        forceLogout(account.getName());//更改後把人踢下線
        return toResp(saved);
    }
    
    
    //總管理員刪除帳號
    public void deleteAccount(Integer id) {
    	AdminAccount admin = adminRepository.findById(id).orElseThrow(() -> new AdminAccountNotFoundException());
    	forceLogout(admin.getName());//更改後把人踢下線
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
    
    //強制踢人其他管理員下線，只有總管理員操作改其他管理員資訊和刪除帳號時會觸發
    private void forceLogout(String name) {
    	//下面這個方法不只服務spring security可能還服務其他的安全框架(OAuth2、未使用框架String)，因此回傳值是object
    	List<Object> admins = sessionRegistry.getAllPrincipals();
    	
    	for (Object admin : admins) {
    		//admin instanceof AdminUserDetails userDetails除了判斷後會傳boolean之外，會把admin轉型成AdminUserDetails塞到userDetails裡面
			if (admin instanceof AdminUserDetails userDetails && userDetails.getName().equals(name)) {
				List<SessionInformation> sessions = sessionRegistry.getAllSessions(admin, false); //把該使用者所有Session抓出來
				for (SessionInformation sessionInfo : sessions) {//把每個session都標記成過期，雖然config有設定只有一個但是
                    sessionInfo.expireNow(); //這個機制依賴使用者還會再發出請求才會被攔截到
                }
				//可以寫成下面單獨註銷，但是怕之後security config有改成允許多個的話，這邊忘記改所以使用for each
//				if (!sessions.isEmpty()) {
//	                sessions.get(0).expireNow();// 標記這個 session 過期
//	            }
				
			}
		}
    	
    	
    	
    }
	
}
