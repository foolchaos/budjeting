package com.example.budjeting.repository;

import com.example.budjeting.entity.BdzArticle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BdzArticleRepository extends JpaRepository<BdzArticle, Long> {
}
