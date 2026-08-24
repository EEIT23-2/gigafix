package com.gigafix.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.admin.entity.AdminAccount;
import com.gigafix.admin.entity.AdminAccount.Role;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Integer> {
	
	AdminAccount findByName(String name);
	
	boolean existsByRole(Role role); //檢查某管理類型是否存在
	
	List<AdminAccount> findByRole(AdminAccount.Role role);

}
