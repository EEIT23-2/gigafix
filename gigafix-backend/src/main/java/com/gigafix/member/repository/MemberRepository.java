package com.gigafix.member.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gigafix.member.entity.Member;


public interface MemberRepository extends JpaRepository<Member, Long> {

	Member findByEmail(String email);

	//後台會員管理：依關鍵字(姓名/暱稱/Email/手機模糊比對)、性別、縣市(地址開頭比對)、加入時間區間做條件查詢
	@Query("SELECT m FROM Member m WHERE " +
			"(:keyword IS NULL OR m.realName LIKE %:keyword% OR m.nickName LIKE %:keyword% OR m.email LIKE %:keyword% OR m.phone LIKE %:keyword%) AND " +
			"(:gender IS NULL OR m.gender = :gender) AND " +
			"(:city IS NULL OR m.address LIKE CONCAT(:city, '%')) AND " +
			"(:startTime IS NULL OR m.createTime >= :startTime) AND " +
			"(:endTime IS NULL OR m.createTime <= :endTime)")
	Page<Member> findByConditions(@Param("keyword") String keyword,
			@Param("gender") Member.Gender gender,
			@Param("city") String city,
			@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			Pageable pageable);
}
