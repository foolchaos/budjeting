package com.example.budget.service;

import com.example.budget.domain.Bo;
import com.example.budget.domain.Request;
import com.example.budget.repo.BoRepository;
import com.example.budget.repo.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoService {

    private final BoRepository boRepository;
    private final RequestRepository requestRepository;

    public BoService(BoRepository boRepository, RequestRepository requestRepository) {
        this.boRepository = boRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<Bo> findAll() {
        return boRepository.findAll();
    }

    public Bo save(Bo bo) {
        return boRepository.save(bo);
    }

    @Transactional
    public void delete(Bo bo) {
        if (bo == null || bo.getId() == null) {
            return;
        }
        deleteById(bo.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        Bo managed = boRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }

        List<Request> requests = requestRepository.findByBoId(id);
        if (!requests.isEmpty()) {
            requests.forEach(r -> r.setBo(null));
            requestRepository.saveAll(requests);
        }

        boRepository.delete(managed);
    }
}
