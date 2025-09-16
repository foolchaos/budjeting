package com.example.budget.repo;
import com.example.budget.domain.Bdz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface BdzRepository extends JpaRepository<Bdz, Long> {
    List<Bdz> findByParentId(Long parentId);
    List<Bdz> findByParentIsNull();
    List<Bdz> findByCodeIn(Collection<String> codes);
}
