package com.example.budjeting.service.impl;

import com.example.budjeting.entity.ApplicationRequest;
import com.example.budjeting.repository.ApplicationRequestRepository;
import com.example.budjeting.service.ApplicationRequestService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationRequestServiceImpl implements ApplicationRequestService {

    private final ApplicationRequestRepository repository;

    public ApplicationRequestServiceImpl(ApplicationRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public ApplicationRequest save(ApplicationRequest request) {
        return repository.save(request);
    }

    @Override
    public List<ApplicationRequest> findAll() {
        return repository.findAll();
    }
}
