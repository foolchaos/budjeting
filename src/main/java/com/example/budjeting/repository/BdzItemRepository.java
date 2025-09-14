package com.example.budjeting.repository;

import com.example.budjeting.entity.BdzItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BdzItemRepository extends JpaRepository<BdzItem, Long> {
}
