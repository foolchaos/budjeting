package com.example.budget.service;

import com.example.budget.domain.*;
import com.example.budget.repo.*;
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

    public RequestService(RequestRepository requestRepository, BoRepository boRepository, BdzRepository bdzRepository,
                          CfoRepository cfoRepository, MvzRepository mvzRepository) {
        this.requestRepository = requestRepository;
        this.boRepository = boRepository;
        this.bdzRepository = bdzRepository;
        this.cfoRepository = cfoRepository;
        this.mvzRepository = mvzRepository;
    }

    public List<Request> findAll() { return requestRepository.findAll(); }
    public Request save(Request r) { return requestRepository.save(r); }
    public void deleteById(Long id) { requestRepository.deleteById(id); }

    public List<Bo> findBoByBdz(Long bdzId) { return boRepository.findByBdzId(bdzId); }
    public List<Mvz> findMvzByCfo(Long cfoId) { return mvzRepository.findByCfoId(cfoId); }
}
