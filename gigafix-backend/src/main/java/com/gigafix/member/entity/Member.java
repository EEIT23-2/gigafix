package com.gigafix.member.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.gigafix.cart.entity.CartItem;
import com.gigafix.forum.entity.Article;
import com.gigafix.forum.entity.Bookmark;
import com.gigafix.forum.entity.Comment;
import com.gigafix.forum.entity.Like;
import com.gigafix.forum.entity.Report;
import com.gigafix.order.entity.Order;
import com.gigafix.product.entity.RecycleApplication;
import com.gigafix.repair.entity.Repairs;

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
import jakarta.validation.constraints.Email;
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
	@Id @Column(name = "member_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Email 
	@Column(name = "email", nullable = false, unique = true)
	private String email;
	
	@Column(name = "password", nullable = false)
	private String password;
	
	@Column(name = "real_name", nullable = false, length = 40)
	private String realName;
	
	@Column(name = "nick_name", nullable = false, length = 40)
	private String nickName;
	
	@Column(name = "phone", nullable = false)
	private String phone;
	
	@Column(name = "address", nullable = false)
	private String address; //要跟地址表以及常用地址做關聯
	
	@Enumerated(EnumType.STRING)//把Enum的值用字串名稱存到database，因為Enum的內容在底層是array
	@Column(name = "gender", nullable = false)
	private Gender gender; //限定男女所以用inner class(enum)，變數無法放入男或女以外的內容
	
	@Column(name = "member_created_time", nullable = false, updatable = false, length = 6)
	private LocalDateTime createTime;
	
	@Column(name = "profile_image_url", columnDefinition = "VARCHAR(MAX)")
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
	private Set<Repairs> fixOrdersObject = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "author")
	private Set<Article> articles = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "author")
	private Set<Comment> comments = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "reporter")
	private Set<Report> reports = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<Like> likes = new HashSet<>();
	
	@Builder.Default
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "member")
	private Set<Bookmark> bookmarks = new HashSet<>();
	
	public enum Gender {
        MALE, FEMALE
    }
	
}
