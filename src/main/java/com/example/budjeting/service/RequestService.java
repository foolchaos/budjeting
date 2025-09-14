package com.example.budjeting.service;

import com.example.budjeting.entity.Request;
import com.example.budjeting.repository.RequestRepository;
import org.springframework.stereotype.Service;

@Service
public class RequestService {
    private final RequestRepository requestRepository;

    public RequestService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public Request save(Request request) {
        if (request.getBudgetItem() != null) {
            request.setZgd(request.getBudgetItem().getZgd());
        }
        return requestRepository.save(request);
    }
}
