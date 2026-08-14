package com.gigafix.repair.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gigafix.repair.entity.Stores;
import com.gigafix.repair.service.StoresService;

import lombok.RequiredArgsConstructor;

@RestController //(Controller + ResponseBody)
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoresController {
	
	private final StoresService storesServ;
	
//	新增分店：完整的請求網址會是 POST /stores
	@PostMapping
	public Stores insert(@RequestParam String name,
			@RequestParam String address, @RequestParam String phone) {
		
		Stores store = new Stores();
		store.setName(name);
		store.setAddress(address);
		store.setPhone(phone);
		
		return storesServ.insert(store);
	}
	
//	修改
	@PutMapping("/{id}")
	public Stores update(@PathVariable Byte id, @RequestParam String name,
			@RequestParam String address, @RequestParam String phone) {
		
		Stores store = new Stores();
		store.setId(id);
		store.setName(name);
		store.setAddress(address);
		store.setPhone(phone);
		
		return storesServ.update(store);
	}
	
	
//	刪除
	@DeleteMapping("/{id}")
	public String deleteById(@PathVariable Byte id) {
		storesServ.deleteById(id);
		return "Delete Success, ID:" + id;
	}
	
//	id查
	@GetMapping("/{id}")
	public Stores queryById(@PathVariable Byte id) {
		return storesServ.selectById(id);
	}
	
//	查全部
	@GetMapping
	public List<Stores> queryAll(){
		return storesServ.selectAll();
	}
	

}
