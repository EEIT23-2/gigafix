package com.gigafix.product.entity;

import com.gigafix.member.entity.Member;

import com.gigafix.product.constant.ProductCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recycle_application")
public class RecycleApplication {
	
	@Id@Column(name = "apply_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long applyId; // 申請單主鍵
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member; // 申請會員 ID
	@Column(name ="product_name" ,nullable = false)
	private String productName;
	@Enumerated(EnumType.ORDINAL)
	@Column(name ="category" ,nullable = false)
	private ProductCategory category; //之後以前端以下拉示選單選取
	@Column(name="appearance" ,nullable = false)
	private String appearance;
	@Column(name = "description")// 使用者外觀描述 下拉式選單供顧客選擇 最終等級是檢測後 以商品管理呼叫修改功能修改等級
	private String description;
	@Column(name="estimated_price")// 備註說明 使用者自述 最後是檢測完畢後 由商品管理修改
	private Integer estimatedPrice;    // 顧客自己期望的評估金額 (允許為空，現場檢測後再填入)
	@Column(name="recycle_status" ,nullable = false)
	private Integer recycleStatus;     // 回收進度狀態碼 之後要改成Enum型別改狀態
	@Column(name ="created_time" ,nullable = false)
	private LocalDateTime createdTime; // 申請時間
	@Column(name ="last_modified_time" ,nullable = false)
	private LocalDateTime lastModifiedTime; //狀態更新時間
	
}
