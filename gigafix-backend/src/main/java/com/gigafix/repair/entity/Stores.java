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
	
	@Column(name = "store_name", nullable = false)
	private String name;
	
	@Column(name = "store_address", nullable = false)
	private String address;
	
	@Column(name = "store_phone", nullable = false)
	private String phone;
	
	
	
	
	
}
