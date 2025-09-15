package com.example.budget.repo;
import com.example.budget.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContractRepository extends JpaRepository<Contract, Long> { }
