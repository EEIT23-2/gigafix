package com.gigafix.member.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.gigafix.cart.entity.CartItem;
import com.gigafix.forum.entity.Articles;
import com.gigafix.forum.entity.Bookmarks;
import com.gigafix.forum.entity.Comments;
import com.gigafix.forum.entity.Likes;
import com.gigafix.forum.entity.Reports;
import com.gigafix.order.entity.Order;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.repair.entity.RepairAppointments;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "members")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member {
	@Id @Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "email", nullable = false, unique = true)
	private String email;
	
	@Column(name = "password", nullable = false)
	private String password;
	
	@Column(name = "real_name", nullable = false)
	private String realName;
	
	@Column(name = "nick_name", nullable = false)
	private String nickName;
	
	@Column(name = "phone", nullable = false)
	private String phone;
	
	@Column(name = "address", nullable = false)
	private String address; //要跟地址表以及常用地址做關聯
	
	@Enumerated(EnumType.STRING)//把Enum的值用字串名稱存到database，因為Enum的內容在底層是array
	@Column(name = "gender", nullable = false)
	private Gender gender; //限定男女所以用inner class(enum)，變數無法放入男或女以外的內容
	
	@Column(name = "create_date_time", nullable = false, updatable = false)
	private LocalDateTime createDateTime;
	
	@Column(name = "profile_image_url")
	private String profileImageUrl;
	
	//關聯的外建FK
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<RecycleApplication> recycleApplications = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<CartItem> cartItems = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<Order> orders = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<RepairAppointments> fixOrdersObject = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "author")
	private Set<Articles> articles = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "author")
	private Set<Comments> comments = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "reporter")
	private Set<Reports> reports = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<Likes> likes = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<Bookmarks> bookmarks = new HashSet<>();
	
	public enum Gender {
        MALE, FEMALE
    }
	
}
