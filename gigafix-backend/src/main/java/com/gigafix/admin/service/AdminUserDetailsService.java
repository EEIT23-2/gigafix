package com.gigafix.admin.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.admin.entity.AdminAccount;
import com.gigafix.admin.exception.AdminBusinessRuleCheckException;
import com.gigafix.admin.repository.AdminAccountRepository;
import com.gigafix.admin.security.AdminUserDetails;

import lombok.RequiredArgsConstructor;
@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {
	private final AdminAccountRepository accountRepository;
	private final LoginLockService lockService;

	
	@Override //
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AdminAccount adminAccount = accountRepository.findByName(username).orElseThrow(() -> new UsernameNotFoundException(""));
		
		if (lockService.getLockRemainingMinutes(adminAccount.getId()) != 0) {
			throw new AdminBusinessRuleCheckException("該帳號密碼錯誤超過5次，請稍後再嘗試登入");
		}
		System.out.println("role: " + adminAccount.getRole()); 
		
		return new AdminUserDetails(adminAccount);
	}

}
