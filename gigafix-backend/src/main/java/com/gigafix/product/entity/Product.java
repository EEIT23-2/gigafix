package com.gigafix.product.entity;


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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId; //主鍵
    @Column(name = "product_name",nullable = false)
    private String productName; //品名(包含型號)
    //@Enumerated(EnumType.STRING)
    @Column(name ="category" ,nullable = false)
    private String category;  //之後要用enum列舉分類:手機 手錶 ipad 之後型別要改成 ProductCategory 暫用String代替
    @Column(name = "image_url" ,nullable = true)
    private String imageUrl; //圖檔連結
    @Column(name = "description",nullable = false)
    private String description; //商品備註
    @Column(name = "appearance", nullable = false)
    private String appearance; //外觀狀況
    @Column(name = "grade" , nullable = false)
    private String grade;  //等級
    @Column(name = "price" , nullable = false)
    private Integer price; //價格
    @Column(name = "sale_status" , nullable = false)
    private Integer saleStatus; //銷售(庫存)狀況 之後用enum常數列舉
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
