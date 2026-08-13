package com.gigafix.repair.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairTechniciansResponse {

	private Integer id;
    private String name;
    private String phone;
    private Byte storeId;//帶出分店資訊
    private String storeName;
    private String storeAddress;
    private String storePhone; 
}
