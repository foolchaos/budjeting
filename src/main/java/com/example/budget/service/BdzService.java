package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.domain.Request;
import com.example.budget.repo.BdzRepository;
import com.example.budget.repo.RequestRepository;
import org.hibernate.Hibernate;
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

    @Transactional(readOnly = true)
    public List<Bdz> findAll() {
        List<Bdz> list = bdzRepository.findAll();
        list.forEach(b -> {
            Hibernate.initialize(b);
            if (b.getParent() != null) {
                Hibernate.initialize(b.getParent());
            }
            if (b.getCfo() != null) {
                Hibernate.initialize(b.getCfo());
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public java.util.List<Bdz> findRoots() {
        List<Bdz> list = bdzRepository.findByParentIsNull();
        list.forEach(b -> {
            Hibernate.initialize(b);
            if (b.getCfo() != null) {
                Hibernate.initialize(b.getCfo());
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public java.util.List<Bdz> findChildren(Long parentId) {
        List<Bdz> list = bdzRepository.findByParentId(parentId);
        list.forEach(b -> {
            Hibernate.initialize(b);
            if (b.getParent() != null) {
                Hibernate.initialize(b.getParent());
            }
            if (b.getCfo() != null) {
                Hibernate.initialize(b.getCfo());
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public Bdz findById(Long id) {
        Bdz bdz = bdzRepository.findById(id).orElse(null);
        if (bdz != null) {
            Hibernate.initialize(bdz);
            if (bdz.getParent() != null) {
                Hibernate.initialize(bdz.getParent());
            }
            if (bdz.getCfo() != null) {
                Hibernate.initialize(bdz.getCfo());
            }
        }
        return bdz;
    }

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
