package com.gigafix.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.user.entity.GigaFixUsers;
import java.util.List;


public interface GigaFixUsersRepository extends JpaRepository<GigaFixUsers, Long> {
	
	GigaFixUsers findByEmail(String email);
}
