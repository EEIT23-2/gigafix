package com.gigafix.repair.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.repair.dto.RepairTechniciansRequest;
import com.gigafix.repair.dto.RepairTechniciansResponse;
import com.gigafix.repair.entity.RepairTechnicians;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.exception.RepairNotFoundException;
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
    
    
    private RepairTechniciansResponse toResponse(RepairTechnicians rt) {
        return RepairTechniciansResponse.builder()
                .id(rt.getId())
                .name(rt.getName())
                .phone(rt.getPhone())
                .storeId(rt.getStore().getId())
                .storeName(rt.getStore().getName())
                .storeAddress(rt.getStore().getAddress())
                .storePhone(rt.getStore().getPhone())
                .build();
    }
    

    // 新增：先知道是哪間分店，再新增技師
    public RepairTechniciansResponse insert(RepairTechniciansRequest req) {
    	Stores store = storesRepos.findById(req.getStoreId())
    			.orElseThrow(() -> new RepairNotFoundException("找不到分店，ID: " + req.getStoreId()));
        RepairTechnicians rt = new RepairTechnicians(null, req.getName(), req.getPhone(), store);
        return toResponse(rtRepos.save(rt));
    }

    // 修改：依據技師id去修改姓名、電話、分店
    public RepairTechniciansResponse updateById(Integer id, RepairTechniciansRequest req) {
        // 前端回傳id數字，要先變物件
    	RepairTechnicians rt = rtRepos.findById(id)
                .orElseThrow(() -> new RepairNotFoundException("找不到技師，ID: " + id));
        Stores store = storesRepos.findById(req.getStoreId())
                .orElseThrow(() -> new RepairNotFoundException("找不到分店，ID: " + req.getStoreId()));
        rt.setName(req.getName());
        rt.setPhone(req.getPhone());
        rt.setStore(store);
        return toResponse(rtRepos.save(rt));
    }

    // 刪除
//    controller有回傳狀態碼204，就不用回傳字串提醒刪除成功了
    public void deleteById(Integer id) {
    	if (!rtRepos.existsById(id)) {
            throw new RepairNotFoundException("找不到技師，ID: " + id);
        }
        rtRepos.deleteById(id);
    }

    // id查詢
    public RepairTechniciansResponse selectById(Integer id) {
    	RepairTechnicians rt = rtRepos.findById(id)
                .orElseThrow(() -> new RepairNotFoundException("找不到技師，ID: " + id));
        return toResponse(rt);
    }

    // 查詢全部技師
    public List<RepairTechniciansResponse> selectAll() {
    	List<RepairTechnicians> list = rtRepos.findAll();
	    List<RepairTechniciansResponse> result = new ArrayList<>();
	    for (RepairTechnicians rt : list) {
	        result.add(toResponse(rt));
	    }
	    return result;
    }
    
	// 查詢：某分店底下的所有技師
    public List<RepairTechniciansResponse> selectByStore(Byte storeId) {
    	List<RepairTechnicians> list = rtRepos.findByStore_Id(storeId);
    	List<RepairTechniciansResponse> result = new ArrayList<>();
    	for (RepairTechnicians rt : list) {
    		result.add(toResponse(rt));
    	}
    	return result;
    }

}
