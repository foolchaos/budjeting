package com.example.budget.repo;

import com.example.budget.domain.ContractAmount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractAmountRepository extends JpaRepository<ContractAmount, Long> {
    Optional<ContractAmount> findByRequestIdAndContractId(Long requestId, Long contractId);
}
