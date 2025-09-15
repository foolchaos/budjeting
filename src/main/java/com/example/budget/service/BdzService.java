package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.domain.Request;
import com.example.budget.repo.BdzRepository;
import com.example.budget.repo.RequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BdzService {
    private final BdzRepository bdzRepository;
    private final RequestRepository requestRepository;

    public BdzService(BdzRepository bdzRepository, RequestRepository requestRepository) {
        this.bdzRepository = bdzRepository;
        this.requestRepository = requestRepository;
    }

    public List<Bdz> findAll() { return bdzRepository.findAll(); }

    public java.util.List<Bdz> findRoots() { return bdzRepository.findByParentIsNull(); }
    public java.util.List<Bdz> findChildren(Long parentId) { return bdzRepository.findByParentId(parentId); }

    public Bdz save(Bdz bdz) { return bdzRepository.save(bdz); }

    @Transactional
    public void deleteById(Long id) {
        Bdz bdz = bdzRepository.findById(id).orElse(null);
        if (bdz == null) return;
        // Отвязать заявки (bdz -> null)
        List<Request> requests = requestRepository.findAll();
        for (Request r : requests) {
            if (r.getBdz() != null && r.getBdz().getId().equals(id)) {
                r.setBdz(null);
                r.setZgd(null);
                requestRepository.save(r);
            }
        }
        // Каскад: потомки/БО/ЗГД настроены на уровне сущностей
        bdzRepository.delete(bdz);
    }
}
