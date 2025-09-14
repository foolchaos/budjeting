package com.example.budjeting.service;

import com.example.budjeting.domain.Request;
import com.example.budjeting.repository.RequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RequestService {
    private final RequestRepository repository;

    public RequestService(RequestRepository repository) {
        this.repository = repository;
    }

    public List<Request> findAll() {
        return repository.findAll();
    }

    public Request save(Request request) {
        if (request.getNumber() == null || request.getNumber().isEmpty()) {
            request.setNumber(UUID.randomUUID().toString());
        }
        return repository.save(request);
    }

    public void delete(Request request) {
        repository.delete(request);
    }
}
