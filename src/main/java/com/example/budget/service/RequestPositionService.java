package com.example.budget.service;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequestPositionService {
    private final RequestPositionRepository requestPositionRepository;
    private final RequestRepository requestRepository;
    private final BoRepository boRepository;
    private final BdzRepository bdzRepository;
    private final CfoTwoRepository cfoTwoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final ZgdRepository zgdRepository;

    public RequestPositionService(RequestPositionRepository requestPositionRepository, RequestRepository requestRepository,
                                  BoRepository boRepository, BdzRepository bdzRepository,
                                  CfoTwoRepository cfoTwoRepository, MvzRepository mvzRepository,
                                  ContractRepository contractRepository, CounterpartyRepository counterpartyRepository,
                                  ZgdRepository zgdRepository) {
        this.requestPositionRepository = requestPositionRepository;
        this.requestRepository = requestRepository;
        this.boRepository = boRepository;
        this.bdzRepository = bdzRepository;
        this.cfoTwoRepository = cfoTwoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;
        this.counterpartyRepository = counterpartyRepository;
        this.zgdRepository = zgdRepository;
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
        }
        if (r.getCounterparty() != null && r.getCounterparty().getId() != null) {
            Long counterpartyId = r.getCounterparty().getId();
            Counterparty managedCounterparty = counterpartyRepository.findById(counterpartyId)
                    .orElseThrow(() -> new IllegalArgumentException("Counterparty not found: " + counterpartyId));
            r.setCounterparty(managedCounterparty);
        }
        if (r.getZgd() != null && r.getZgd().getId() != null) {
            r.setZgd(zgdRepository.getReferenceById(r.getZgd().getId()));
        }
        return requestPositionRepository.save(r);
    }

    public void deleteById(Long id) {
        requestPositionRepository.deleteById(id);
    }

    public List<Bo> findBoByBdz(Long bdzId) {
        return boRepository.findByBdzId(bdzId);
    }

    private void initializeRelations(List<RequestPosition> positions) {
        positions.forEach(this::initializeRelations);
    }

    private void initializeRelations(RequestPosition r) {
        if (r.getRequest() != null) Hibernate.initialize(r.getRequest());
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
        if (r.getCounterparty() != null) {
            Hibernate.initialize(r.getCounterparty());
        }
        if (r.getZgd() != null) {
            Hibernate.initialize(r.getZgd());
        }
    }
}
