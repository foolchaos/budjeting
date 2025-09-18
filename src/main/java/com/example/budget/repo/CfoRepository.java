package com.example.budget.repo;
import com.example.budget.domain.Cfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CfoRepository extends JpaRepository<Cfo, Long> {
    List<Cfo> findByRequestIsNull();
}
