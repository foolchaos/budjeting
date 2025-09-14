package com.example.budjeting.repository;

import com.example.budjeting.entity.Bdz;
import com.example.budjeting.entity.Bo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoRepository extends JpaRepository<Bo, Long> {
    List<Bo> findByBdz(Bdz bdz);
}
