package com.gigafix.forum.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gigafix.forum.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

	Category findByName(String name);
}
