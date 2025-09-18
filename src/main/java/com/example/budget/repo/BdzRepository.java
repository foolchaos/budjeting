package com.example.budget.repo;
import com.example.budget.domain.Bdz;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BdzRepository extends JpaRepository<Bdz, Long> {
    java.util.List<Bdz> findByParentId(Long parentId);
    java.util.List<Bdz> findByParentIsNull();
    java.util.List<Bdz> findByCfoId(Long cfoId);
}
