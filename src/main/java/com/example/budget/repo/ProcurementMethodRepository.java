package com.example.budget.repo;

import com.example.budget.domain.ProcurementMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcurementMethodRepository extends JpaRepository<ProcurementMethod, Long> {
}
