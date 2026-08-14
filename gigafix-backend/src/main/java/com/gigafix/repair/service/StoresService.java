package com.gigafix.repair.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.repository.StoresRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
//1. Lombok 會自動產生帶有 final 欄位的建構子，Spring 會藉此進行依賴注入
public class StoresService {

//2. 宣告為 private final，不用寫 @Autowired
	private final StoresRepository storesRepos;
	
//	新增分店
	public Stores insert(Stores store) {
		return storesRepos.save(store);
	}
	
//	修改
	public Stores update(Stores store) {
		return storesRepos.save(store);
	}
	
//	刪除
	public void deleteById(Byte id) {
		storesRepos.deleteById(id);
	}
	
//	id查詢
	public Stores selectById(Byte id) {
		Optional<Stores> op = storesRepos.findById(id);
		
		if(op.isPresent()) {
			return op.get();
		}
		return null;
	}
	
//	查詢全部
	public List<Stores> selectAll(){
		return storesRepos.findAll();
	}
	
	
	
	
}
