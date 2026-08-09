package com.gigafix.product.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.gigafix.product.constant.ProductCategory;
import com.gigafix.product.constant.ProductSaleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id@Column(name = "product_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId; //主鍵
    @Column(name = "product_name",nullable = false)
    @JsonProperty("product_name")
    private String productName; //品名(包含型號)
    @Enumerated(EnumType.STRING)
    @Column(name ="category" ,nullable = false)
    private ProductCategory category;  //之後要用enum列舉分類:手機 手錶 ipad 之後型別要改成 ProductCategory 暫用String代替
    @Column(name = "image_url" ,nullable = true)
    @JsonProperty("image_url")
    private String imageUrl; //圖檔連結
    @Column(name = "description",nullable = false)
    private String description; //商品備註
    @Column(name = "appearance", nullable = false)
    private String appearance; //外觀狀況
    @Column(name = "grade" , nullable = false)
    private String grade;  //等級
    @Column(name = "price" , nullable = false)
    private Integer price; //價格
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "sale_status" , nullable = false)
    @JsonProperty("sale_status")
    private ProductSaleStatus saleStatus; //銷售(庫存)狀況 之後用enum常數列舉
    @Column(name = "created_date" ,nullable = false)
    private LocalDateTime createdDate; //創建時間
    @Column(name = "last_modified_date" ,nullable = false)
    private LocalDateTime lastModifiedDate; //最後更新時間

//	//關聯的外建FK,資料型別待組員建立後更改
//	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JoinColumn(name = "invoice_id")
//	private Object cartItems;
//	@Builder.Default
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = )
//	private List<> productOrders;
//	@Builder.Default
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = )
//	private List<> socialPosts;
//	@Builder.Default
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = )
//	private List<> fixOrdersObject;


}
