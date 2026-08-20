package com.gigafix.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.admin.entity.AdminAccount;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Integer> {
	
	AdminAccount findByName(String name);

}
