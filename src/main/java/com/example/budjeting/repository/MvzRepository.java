package com.example.budjeting.repository;

import com.example.budjeting.entity.Cfo;
import com.example.budjeting.entity.Mvz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MvzRepository extends JpaRepository<Mvz, Long> {
    List<Mvz> findByCfo(Cfo cfo);
}
