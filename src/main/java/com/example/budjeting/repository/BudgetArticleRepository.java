package com.example.budjeting.repository;

import com.example.budjeting.entity.BudgetArticle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetArticleRepository extends JpaRepository<BudgetArticle, Long> {}
