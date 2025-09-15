package com.example.budget.service;

import com.example.budget.domain.Contract;
import com.example.budget.domain.Request;
import com.example.budget.repo.ContractRepository;
import com.example.budget.repo.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final RequestRepository requestRepository;

    public ContractService(ContractRepository contractRepository, RequestRepository requestRepository) {
        this.contractRepository = contractRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<Contract> findAll() {
        return contractRepository.findAll();
    }

    public Contract save(Contract contract) {
        return contractRepository.save(contract);
    }

    @Transactional
    public void delete(Contract contract) {
        if (contract == null || contract.getId() == null) {
            return;
        }
        deleteById(contract.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        Contract managed = contractRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }

        List<Request> requests = requestRepository.findByContractId(id);
        if (!requests.isEmpty()) {
            requests.forEach(r -> r.setContract(null));
            requestRepository.saveAll(requests);
        }

        contractRepository.delete(managed);
    }
}
