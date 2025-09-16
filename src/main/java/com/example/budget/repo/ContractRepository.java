package com.example.budget.repo;
import com.example.budget.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByCounterpartyId(Long counterpartyId);
}
