package com.example.budget.service;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequestService {
    private final RequestRepository requestRepository;
    private final BoRepository boRepository;
    private final BdzRepository bdzRepository;
    private final CfoRepository cfoRepository;
    private final MvzRepository mvzRepository;
    private final ContractRepository contractRepository;
    private final ZgdRepository zgdRepository;

    public RequestService(RequestRepository requestRepository, BoRepository boRepository, BdzRepository bdzRepository,
                          CfoRepository cfoRepository, MvzRepository mvzRepository, ContractRepository contractRepository,
                          ZgdRepository zgdRepository) {
        this.requestRepository = requestRepository;
        this.boRepository = boRepository;
        this.bdzRepository = bdzRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
        this.contractRepository = contractRepository;
        this.zgdRepository = zgdRepository;
    }

    @Transactional(readOnly = true)
    public List<Request> findAll() {
        List<Request> list = requestRepository.findAll();
        list.forEach(r -> {
            if (r.getBdz() != null) Hibernate.initialize(r.getBdz());
            if (r.getBo() != null) Hibernate.initialize(r.getBo());
            if (r.getCfo() != null) Hibernate.initialize(r.getCfo());
            if (r.getMvz() != null) Hibernate.initialize(r.getMvz());
            if (r.getContract() != null) Hibernate.initialize(r.getContract());
            if (r.getZgd() != null) Hibernate.initialize(r.getZgd());
        });
        return list;
    }
    @Transactional
    public Request save(Request r) {
        if (r.getBdz() != null && r.getBdz().getId() != null) {
            r.setBdz(bdzRepository.getReferenceById(r.getBdz().getId()));
        }
        if (r.getBo() != null && r.getBo().getId() != null) {
            r.setBo(boRepository.getReferenceById(r.getBo().getId()));
        }
        if (r.getCfo() != null && r.getCfo().getId() != null) {
            r.setCfo(cfoRepository.getReferenceById(r.getCfo().getId()));
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
        if (r.getZgd() != null && r.getZgd().getId() != null) {
            r.setZgd(zgdRepository.getReferenceById(r.getZgd().getId()));
        }
        return requestRepository.save(r);
    }
    public void deleteById(Long id) { requestRepository.deleteById(id); }

    public List<Bo> findBoByBdz(Long bdzId) { return boRepository.findByBdzId(bdzId); }
    public List<Mvz> findMvzByCfo(Long cfoId) { return mvzRepository.findByCfoId(cfoId); }
}
