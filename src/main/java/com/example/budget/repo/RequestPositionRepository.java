package com.example.budget.repo;

import com.example.budget.domain.RequestPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RequestPositionRepository extends JpaRepository<RequestPosition, Long> {
    List<RequestPosition> findByBoId(Long boId);

    List<RequestPosition> findByContractId(Long contractId);

    List<RequestPosition> findByZgdId(Long zgdId);

    List<RequestPosition> findByCounterpartyId(Long counterpartyId);

    List<RequestPosition> findByRequestId(Long requestId);

    List<RequestPosition> findByMvzId(Long mvzId);

    List<RequestPosition> findByProcurementMethodId(Long procurementMethodId);

    @Query("select coalesce(sum(r.amountNoVat), 0) from RequestPosition r " +
            "where r.request.id = :requestId and r.contract.id = :contractId")
    BigDecimal sumAmountNoVatByRequestIdAndContractId(@Param("requestId") Long requestId,
                                                      @Param("contractId") Long contractId);

    long countByRequestIdAndContractId(Long requestId, Long contractId);
}
