package com.example.budget.service;

import com.example.budget.domain.Request;
import com.example.budget.domain.RequestPosition;
import com.example.budget.repo.RequestRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequestService {
    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<Request> findAll() {
        List<Request> requests = requestRepository.findAll();
        requests.forEach(this::initializePositions);
        return requests;
    }

    @Transactional(readOnly = true)
    public Request findById(Long id) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
        initializePositions(request);
        return request;
    }

    @Transactional
    public Request save(Request request) {
        return requestRepository.save(request);
    }

    @Transactional
    public void deleteById(Long id) {
        requestRepository.deleteById(id);
    }

    private void initializePositions(Request request) {
        Hibernate.initialize(request.getPositions());
        Hibernate.initialize(request.getCfo());
        for (RequestPosition position : request.getPositions()) {
            Hibernate.initialize(position);
        }
    }
}
