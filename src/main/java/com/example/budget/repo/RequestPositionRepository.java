package com.example.budget.repo;

import com.example.budget.domain.RequestPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestPositionRepository extends JpaRepository<RequestPosition, Long> {
    List<RequestPosition> findByBoId(Long boId);

    List<RequestPosition> findByContractId(Long contractId);

    List<RequestPosition> findByZgdId(Long zgdId);

    List<RequestPosition> findByCounterpartyId(Long counterpartyId);

    List<RequestPosition> findByRequestId(Long requestId);

    List<RequestPosition> findByMvzId(Long mvzId);
}
