package com.gigafix.repair.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.repair.entity.Repairs;

public interface RepairsRepository extends JpaRepository<Repairs, Long> {

}
