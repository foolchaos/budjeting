package com.example.budget.service;

import com.example.budget.domain.Contract;
import com.example.budget.domain.Counterparty;
import com.example.budget.domain.Request;
import com.example.budget.repo.ContractRepository;
import com.example.budget.repo.CounterpartyRepository;
import com.example.budget.repo.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CounterpartyService {

    private final CounterpartyRepository counterpartyRepository;
    private final ContractRepository contractRepository;
    private final RequestRepository requestRepository;

    public CounterpartyService(CounterpartyRepository counterpartyRepository,
                               ContractRepository contractRepository,
                               RequestRepository requestRepository) {
        this.counterpartyRepository = counterpartyRepository;
        this.contractRepository = contractRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<Counterparty> findAll() {
        return counterpartyRepository.findAll();
    }

    public Counterparty save(Counterparty counterparty) {
        return counterpartyRepository.save(counterparty);
    }

    @Transactional
    public void delete(Counterparty counterparty) {
        if (counterparty == null || counterparty.getId() == null) {
            return;
        }
        deleteById(counterparty.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        Counterparty managed = counterpartyRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }

        List<Contract> contracts = contractRepository.findByCounterpartyId(id);
        if (!contracts.isEmpty()) {
            contracts.forEach(contract -> contract.setCounterparty(null));
            contractRepository.saveAll(contracts);
        }

        List<Request> requests = requestRepository.findByCounterpartyId(id);
        if (!requests.isEmpty()) {
            requests.forEach(request -> request.setCounterparty(null));
            requestRepository.saveAll(requests);
        }

        counterpartyRepository.delete(managed);
    }
}
