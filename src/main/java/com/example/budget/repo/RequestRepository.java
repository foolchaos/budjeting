package com.example.budget.repo;
import com.example.budget.domain.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByBoId(Long boId);
}
