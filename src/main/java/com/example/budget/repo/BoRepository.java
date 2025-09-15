package com.example.budget.repo;
import com.example.budget.domain.Bo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BoRepository extends JpaRepository<Bo, Long> {
    List<Bo> findByBdzId(Long bdzId);
}
