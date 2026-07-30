package com.gigafix.forum.entity;

import com.gigafix.member.entity.Member;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "articles")
public class Articles {
	@Id@Column(name = "articles_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articlesId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	private Member author;
}
