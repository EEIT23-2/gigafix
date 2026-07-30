package com.gigafix.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.member.entity.Member;


public interface MemberRepository extends JpaRepository<Member, Long> {
	
	Member findByEmail(String email);
}
