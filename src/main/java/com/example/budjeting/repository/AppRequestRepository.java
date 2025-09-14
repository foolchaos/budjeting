package com.example.budjeting.repository;

import com.example.budjeting.model.AppRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRequestRepository extends JpaRepository<AppRequest, Long> {
}
