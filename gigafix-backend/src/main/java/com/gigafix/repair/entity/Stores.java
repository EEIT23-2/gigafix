package com.gigafix.repair.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "stores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stores {

	@Id @Column(name = "store_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Byte id;
	
	@Column(name = "store_name", nullable = false, length = 20)
	private String name;
	
	@Column(name = "store_address", nullable = false, length = 200)
	private String address;
	
	//預設是會生成nvarchar，要強制為varchar
	@Column(name = "store_phone", nullable = false, columnDefinition = "varchar(20)")
	private String phone;
	
	
	
	
	
}
