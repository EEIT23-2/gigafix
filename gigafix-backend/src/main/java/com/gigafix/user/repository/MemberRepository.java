package com.gigafix.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.user.entity.Member;


public interface MemberRepository extends JpaRepository<Member, Long> {
	
	Member findByEmail(String email);
}
