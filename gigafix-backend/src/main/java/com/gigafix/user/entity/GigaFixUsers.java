package com.gigafix.user.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gigafix_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GigaFixUsers {
	@Id @Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "password", nullable = false)
	private String password;
	
	@Column(name = "real_name", nullable = false)
	private String realName;
	
	@Column(name = "nick_name", nullable = false)
	private String nickName;
	
	@Column(name = "email", nullable = false, unique = true)
	private String email;
	
	@Column(name = "phone", nullable = false)
	private String phone;
	
	
	@Column(name = "address", nullable = false)
	private String address; //要跟地址表以及常用地址做關聯
	
	@Enumerated(EnumType.STRING)//把Enum的值用字串名稱存到database，因為Enum的內容在底層是array
	@Column(name = "gender", nullable = false)
	private Gender gender; //限定男女所以用inner class(enum)，變數無法放入男或女以外的內容
	
	@Column(name = "create_date_time", nullable = false, updatable = false)
	private LocalDateTime createDateTime;
	
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
	
	public enum Gender {
        MALE, FEMALE
    }
	
}
