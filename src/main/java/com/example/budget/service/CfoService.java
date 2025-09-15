package com.example.budget.service;

import com.example.budget.domain.Cfo;
import com.example.budget.domain.Request;
import com.example.budget.repo.CfoRepository;
import com.example.budget.repo.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CfoService {
    private final CfoRepository cfoRepository;
    private final RequestRepository requestRepository;

    public CfoService(CfoRepository cfoRepository, RequestRepository requestRepository) {
        this.cfoRepository = cfoRepository;
        this.requestRepository = requestRepository;
    }

    public List<Cfo> findAll() { return cfoRepository.findAll(); }
    public Cfo save(Cfo cfo) { return cfoRepository.save(cfo); }

    @Transactional
    public void deleteById(Long id) {
        Cfo cfo = cfoRepository.findById(id).orElse(null);
        if (cfo == null) return;
        // Отвязать заявки (cfo/mvz -> null)
        List<Request> requests = requestRepository.findAll();
        for (Request r : requests) {
            if (r.getCfo() != null && r.getCfo().getId().equals(id)) {
                r.setCfo(null);
                r.setMvz(null);
                requestRepository.save(r);
            }
        }
        cfoRepository.delete(cfo); // каскадом удалятся МВЗ
    }
}
