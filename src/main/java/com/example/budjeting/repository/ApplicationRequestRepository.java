package com.example.budjeting.repository;

import com.example.budjeting.entity.ApplicationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRequestRepository extends JpaRepository<ApplicationRequest, Long> {
}
