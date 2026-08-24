package com.gigafix.repair.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.repair.entity.RepairTechnicians;

public interface RepairTechniciansRepository extends JpaRepository<RepairTechnicians, Integer> {

	List<RepairTechnicians> findByStore_Id(Byte storeId);
}
