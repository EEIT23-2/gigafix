package com.gigafix.repair.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.repair.dto.RepairTechniciansRequest;
import com.gigafix.repair.entity.RepairTechnicians;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.RepairTechniciansRepository;
import com.gigafix.repair.repository.StoresRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RepairTechniciansService {
	
	private final RepairTechniciansRepository rtRepos;
	//	關聯式 也要帶出 StoresRepository
    private final StoresRepository storesRepos;

    // 新增：先知道是哪間分店，再新增技師
    public RepairTechnicians insert(RepairTechniciansRequest req) {
    	Stores store = storesRepos.findById(req.getStoreId()).orElse(null);
        RepairTechnicians rt = new RepairTechnicians(null, req.getName(), req.getPhone(), store);
        return rtRepos.save(rt);
    }

    // 修改：依據技師id去修改姓名、電話、分店
    public RepairTechnicians update(Integer id, RepairTechniciansRequest req) {
        // 前端回傳id數字，要先變物件
        Stores store = storesRepos.findById(req.getStoreId()).orElse(null);
        RepairTechnicians rt = new RepairTechnicians(id, req.getName(), req.getPhone(), store);
        return rtRepos.save(rt);
    }

    // 刪除
    public void deleteById(Integer id) {
        rtRepos.deleteById(id);
    }

    // id查詢
    public RepairTechnicians selectById(Integer id) {
        return rtRepos.findById(id).orElse(null);
    }

    // 查詢全部技師
    public List<RepairTechnicians> selectAll() {
        return rtRepos.findAll();
    }

}
