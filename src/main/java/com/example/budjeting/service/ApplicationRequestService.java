package com.example.budjeting.service;

import com.example.budjeting.entity.ApplicationRequest;
import java.util.List;

public interface ApplicationRequestService {
    ApplicationRequest save(ApplicationRequest request);
    List<ApplicationRequest> findAll();
}
