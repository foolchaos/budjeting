package com.example.budget.service;

import com.example.budget.domain.Cfo;
import com.example.budget.domain.Bdz;
import com.example.budget.domain.Mvz;
import com.example.budget.repo.BdzRepository;
import com.example.budget.repo.CfoRepository;
import com.example.budget.repo.MvzRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CfoService {
    private final CfoRepository cfoRepository;
    private final BdzRepository bdzRepository;
    private final MvzRepository mvzRepository;

    public CfoService(CfoRepository cfoRepository, BdzRepository bdzRepository, MvzRepository mvzRepository) {
        this.cfoRepository = cfoRepository;
        this.bdzRepository = bdzRepository;
        this.mvzRepository = mvzRepository;
    }

    public List<Cfo> findAll() { return cfoRepository.findAll(); }
    public Cfo save(Cfo cfo) { return cfoRepository.save(cfo); }

    @Transactional
    public void deleteById(Long id) {
        Cfo cfo = cfoRepository.findById(id).orElse(null);
        if (cfo == null) return;
        // Отвязать связанные объекты
        for (Mvz mvz : mvzRepository.findAll()) {
            if (mvz.getCfo() != null && mvz.getCfo().getId().equals(id)) {
                mvz.setCfo(null);
                mvzRepository.save(mvz);
            }
        }
        for (Bdz bdz : bdzRepository.findAll()) {
            if (bdz.getCfo() != null && bdz.getCfo().getId().equals(id)) {
                bdz.setCfo(null);
                bdzRepository.save(bdz);
            }
        }
        cfoRepository.delete(cfo);
    }
}
