package com.gigafix.repair.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity @Table(name = "repair_technicians")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairTechnicians {

	@Id @Column(name = "technician_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "technician_name", nullable = false, length = 20)
	private String name;
	
	@Column(name = "technician_phone", nullable = false, columnDefinition = "varchar(20)")
	private String phone;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
//	待確認：防止 Lombok 自動產生的 toString() 陷入雙向關聯(store、technician)的無限無窮迴圈，
//	導致記憶體爆掉（StackOverflowError）崩潰。
	private Stores store;
	
	
}
