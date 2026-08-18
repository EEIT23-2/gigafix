package com.gigafix.admin.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gigafix.admin.entity.AdminAccount;
import com.gigafix.admin.repository.AdminAccountRepository;
import com.gigafix.admin.security.AdminUserDetails;

import lombok.RequiredArgsConstructor;
@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {
	private final AdminAccountRepository accountRepository;

	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AdminAccount adminAccount = accountRepository.findByAdminAccountName(username);
		if (adminAccount == null) {
			throw new UsernameNotFoundException(""); 
			//這個錯誤會被spring security的DaoAuthenticationProvider攔截並轉成拋回來adCredentialsException("Bad credentials")
		}
		
		return new AdminUserDetails(adminAccount);
	}

}
