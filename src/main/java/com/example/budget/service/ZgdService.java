package com.example.budget.service;

import com.example.budget.domain.RequestPosition;
import com.example.budget.domain.Zgd;
import com.example.budget.repo.RequestPositionRepository;
import com.example.budget.repo.ZgdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZgdService {

    private final ZgdRepository zgdRepository;
    private final RequestPositionRepository requestRepository;

    public ZgdService(ZgdRepository zgdRepository, RequestPositionRepository requestRepository) {
        this.zgdRepository = zgdRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<Zgd> findAll() {
        return zgdRepository.findAll();
    }

    public Zgd save(Zgd zgd) {
        return zgdRepository.save(zgd);
    }

    @Transactional
    public void delete(Zgd zgd) {
        if (zgd == null || zgd.getId() == null) {
            return;
        }
        deleteById(zgd.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        Zgd managed = zgdRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }

        List<RequestPosition> requests = requestRepository.findByZgdId(id);
        if (!requests.isEmpty()) {
            requests.forEach(r -> r.setZgd(null));
            requestRepository.saveAll(requests);
        }

        zgdRepository.delete(managed);
    }
}
