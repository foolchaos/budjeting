package com.example.budget.repo;
import com.example.budget.domain.Mvz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MvzRepository extends JpaRepository<Mvz, Long> {
    java.util.List<Mvz> findByCfoId(Long cfoId);
}
