package com.gigafix.repair.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gigafix.repair.dto.StoresRequest;
import com.gigafix.repair.dto.StoresResponse;
import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.StoresRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
//1. Lombok 會自動產生帶有 final 欄位的建構子，Spring 會藉此進行依賴注入
public class StoresService {

//2. 宣告為 private final，不用寫 @Autowired
	private final StoresRepository storesRepos;
	
	
	private StoresResponse toResponse(Stores s) {
		return StoresResponse.builder()
				.id(s.getId())
				.name(s.getName())
				.address(s.getAddress())
				.phone(s.getPhone())
				.build();
	}
	
//	新增分店
	public StoresResponse insert(StoresRequest req) {
		Stores store = new Stores();
		store.setName(req.getName());
		store.setAddress(req.getAddress());
		store.setPhone(req.getPhone());
		return toResponse(storesRepos.save(store)); 
	}
	
//	修改
	public StoresResponse updateById(Byte id, StoresRequest req) {
		Stores store = storesRepos.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("分店ID: " + id + " 找不到!"));
		store.setName(req.getName());
		store.setAddress(req.getAddress());
		store.setPhone(req.getPhone());
		return toResponse(storesRepos.save(store)); 
	}
	
//	刪除
//  controller有回傳狀態碼204，就不用回傳字串提醒刪除成功了
	public void deleteById(Byte id) {
		if(!storesRepos.existsById(id)) {
			throw new EntityNotFoundException("分店ID: " + id + " 找不到!");
		}
		storesRepos.deleteById(id);
	}
	
//	id查詢
	public StoresResponse selectById(Byte id) {
//		lambda 寫法
		Stores store = storesRepos.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("分店ID: " + id + " 找不到!"));
	    return toResponse(store);
	    
//	 // 不用箭頭寫法（if 判斷）
//	    Optional<Stores> op = storesRepos.findById(id);
//	    if (op.isEmpty()) {
//	        throw new EntityNotFoundException("分店ID: " + id + " 找不到!");
//	    }
//	    Stores store = op.get();
	}
	
//	查詢全部
	public List<StoresResponse> selectAll(){
		List<Stores> list = storesRepos.findAll();
	    List<StoresResponse> result = new ArrayList<>();
	    for (Stores s : list) {
	        result.add(toResponse(s));
	    }
	    return result;
	    
//	    //:: 是 Java 8 開始的一個特殊語法，叫做「方法參考」
//		return storesRepos.findAll().stream()
//				.map(this::toResponse)
//                .toList();
	}
	
	
	
	
}
