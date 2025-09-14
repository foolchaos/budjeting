package com.example.budjeting.repository;

import com.example.budjeting.entity.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetItemRepository extends JpaRepository<BudgetItem, Long> {
    List<BudgetItem> findByParent(BudgetItem parent);
}
