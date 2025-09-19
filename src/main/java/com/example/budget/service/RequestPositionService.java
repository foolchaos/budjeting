package com.example.budget.service;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class RequestPositionService {
    private final RequestPositionRepository requestPositionRepository;
    private final RequestRepository requestRepository;
    private final BoRepository boRepository;
    private final BdzRepository bdzRepository;
    private final CfoTwoRepository cfoTwoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;
    private final ContractAmountRepository contractAmountRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final ZgdRepository zgdRepository;
    private final ProcurementMethodRepository procurementMethodRepository;

    public RequestPositionService(RequestPositionRepository requestPositionRepository, RequestRepository requestRepository,
                                  BoRepository boRepository, BdzRepository bdzRepository,
                                  CfoTwoRepository cfoTwoRepository, MvzRepository mvzRepository,
                                  ContractRepository contractRepository, ContractAmountRepository contractAmountRepository,
                                  CounterpartyRepository counterpartyRepository, ZgdRepository zgdRepository,
                                  ProcurementMethodRepository procurementMethodRepository) {
        this.requestPositionRepository = requestPositionRepository;
        this.requestRepository = requestRepository;
        this.boRepository = boRepository;
        this.bdzRepository = bdzRepository;
        this.cfoTwoRepository = cfoTwoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;
        this.contractAmountRepository = contractAmountRepository;
        this.counterpartyRepository = counterpartyRepository;
        this.zgdRepository = zgdRepository;
        this.procurementMethodRepository = procurementMethodRepository;
    }

    @Transactional(readOnly = true)
    public List<RequestPosition> findAll() {
        List<RequestPosition> list = requestPositionRepository.findAll();
        initializeRelations(list);
        return list;
    }

    @Transactional(readOnly = true)
    public List<RequestPosition> findByRequestId(Long requestId) {
        if (requestId == null) {
            return List.of();
        }
        List<RequestPosition> list = requestPositionRepository.findByRequestId(requestId);
        initializeRelations(list);
        return list;
    }

    @Transactional(readOnly = true)
    public RequestPosition findDetailedById(Long id) {
        RequestPosition request = requestPositionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request position not found: " + id));
        initializeRelations(request);
        return request;
    }

    @Transactional
    public RequestPosition save(RequestPosition r) {
        Long previousRequestId = null;
        Long previousContractId = null;
        if (r.getId() != null) {
            RequestPosition existing = requestPositionRepository.findById(r.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Request position not found: " + r.getId()));
            if (existing.getRequest() != null) {
                previousRequestId = existing.getRequest().getId();
            }
            if (existing.getContract() != null) {
                previousContractId = existing.getContract().getId();
            }
        }
        if (r.getRequest() != null && r.getRequest().getId() != null) {
            r.setRequest(requestRepository.getReferenceById(r.getRequest().getId()));
        }
        if (r.getBdz() != null && r.getBdz().getId() != null) {
            r.setBdz(bdzRepository.getReferenceById(r.getBdz().getId()));
        }
        if (r.getBo() != null && r.getBo().getId() != null) {
            r.setBo(boRepository.getReferenceById(r.getBo().getId()));
        }
        if (r.getCfo2() != null && r.getCfo2().getId() != null) {
            r.setCfo2(cfoTwoRepository.getReferenceById(r.getCfo2().getId()));
        }
        if (r.getMvz() != null && r.getMvz().getId() != null) {
            r.setMvz(mvzRepository.getReferenceById(r.getMvz().getId()));
        }
        if (r.getContract() != null && r.getContract().getId() != null) {
            Long contractId = r.getContract().getId();
            Contract managedContract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
            r.setContract(managedContract);
        } else {
            r.setContract(null);
        }
        if (r.getCounterparty() != null && r.getCounterparty().getId() != null) {
            Long counterpartyId = r.getCounterparty().getId();
            Counterparty managedCounterparty = counterpartyRepository.findById(counterpartyId)
                    .orElseThrow(() -> new IllegalArgumentException("Counterparty not found: " + counterpartyId));
            r.setCounterparty(managedCounterparty);
        }
        if (r.getProcurementMethod() != null && r.getProcurementMethod().getId() != null) {
            Long methodId = r.getProcurementMethod().getId();
            ProcurementMethod managedMethod = procurementMethodRepository.findById(methodId)
                    .orElseThrow(() -> new IllegalArgumentException("Procurement method not found: " + methodId));
            r.setProcurementMethod(managedMethod);
        } else {
            r.setProcurementMethod(null);
        }
        if (r.getZgd() != null && r.getZgd().getId() != null) {
            r.setZgd(zgdRepository.getReferenceById(r.getZgd().getId()));
        }
        ContractAmount contractAmount = null;
        if (r.getRequest() != null && r.getContract() != null) {
            Long requestId = r.getRequest().getId();
            Long contractId = r.getContract().getId();
            contractAmount = contractAmountRepository.findByRequestIdAndContractId(requestId, contractId)
                    .orElseGet(() -> {
                        ContractAmount amount = new ContractAmount();
                        amount.setRequest(r.getRequest());
                        amount.setContract(r.getContract());
                        amount.setAmount(BigDecimal.ZERO);
                        return contractAmountRepository.save(amount);
                    });
        }
        r.setContractAmount(contractAmount);

        RequestPosition saved = requestPositionRepository.save(r);

        Long newRequestId = saved.getRequest() != null ? saved.getRequest().getId() : null;
        Long newContractId = saved.getContract() != null ? saved.getContract().getId() : null;

        if (previousRequestId != null && previousContractId != null &&
                (!Objects.equals(previousRequestId, newRequestId) || !Objects.equals(previousContractId, newContractId))) {
            recalculateContractAmount(previousRequestId, previousContractId);
        }

        if (newRequestId != null && newContractId != null) {
            recalculateContractAmount(newRequestId, newContractId);
        }

        return saved;
    }

    public void deleteById(Long id) {
        requestPositionRepository.findById(id).ifPresent(position -> {
            Long requestId = position.getRequest() != null ? position.getRequest().getId() : null;
            Long contractId = position.getContract() != null ? position.getContract().getId() : null;
            requestPositionRepository.delete(position);
            if (requestId != null && contractId != null) {
                recalculateContractAmount(requestId, contractId);
            }
        });
    }

    public List<Bo> findBoByBdz(Long bdzId) {
        return boRepository.findByBdzId(bdzId);
    }

    private void initializeRelations(List<RequestPosition> positions) {
        positions.forEach(this::initializeRelations);
    }

    private void initializeRelations(RequestPosition r) {
        if (r.getRequest() != null) {
            Hibernate.initialize(r.getRequest());
            if (r.getRequest().getCfo() != null) {
                Hibernate.initialize(r.getRequest().getCfo());
            }
        }
        if (r.getBdz() != null) {
            Hibernate.initialize(r.getBdz());
            if (r.getBdz().getParent() != null) {
                Hibernate.initialize(r.getBdz().getParent());
            }
        }
        if (r.getBo() != null) {
            Hibernate.initialize(r.getBo());
        }
        if (r.getCfo2() != null) {
            Hibernate.initialize(r.getCfo2());
        }
        if (r.getMvz() != null) {
            Hibernate.initialize(r.getMvz());
        }
        if (r.getContract() != null) {
            Hibernate.initialize(r.getContract());
        }
        if (r.getContractAmount() != null) {
            Hibernate.initialize(r.getContractAmount());
        }
        if (r.getCounterparty() != null) {
            Hibernate.initialize(r.getCounterparty());
        }
        if (r.getProcurementMethod() != null) {
            Hibernate.initialize(r.getProcurementMethod());
        }
        if (r.getZgd() != null) {
            Hibernate.initialize(r.getZgd());
        }
    }

    private void recalculateContractAmount(Long requestId, Long contractId) {
        if (requestId == null || contractId == null) {
            return;
        }

        long positionsCount = requestPositionRepository.countByRequestIdAndContractId(requestId, contractId);
        if (positionsCount == 0) {
            contractAmountRepository.findByRequestIdAndContractId(requestId, contractId)
                    .ifPresent(contractAmountRepository::delete);
            return;
        }

        BigDecimal total = requestPositionRepository
                .sumAmountNoVatByRequestIdAndContractId(requestId, contractId);
        if (total == null) {
            total = BigDecimal.ZERO;
        }

        ContractAmount contractAmount = contractAmountRepository
                .findByRequestIdAndContractId(requestId, contractId)
                .orElseGet(() -> {
                    ContractAmount amount = new ContractAmount();
                    amount.setRequest(requestRepository.getReferenceById(requestId));
                    amount.setContract(contractRepository.getReferenceById(contractId));
                    return amount;
                });
        contractAmount.setAmount(total);
        contractAmountRepository.save(contractAmount);
    }
}
