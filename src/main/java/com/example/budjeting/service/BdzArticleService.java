package com.example.budjeting.service;

import com.example.budjeting.domain.BdzArticle;
import com.example.budjeting.repository.BdzArticleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BdzArticleService {
    private final BdzArticleRepository repository;

    public BdzArticleService(BdzArticleRepository repository) {
        this.repository = repository;
    }

    public List<BdzArticle> findAll() {
        return repository.findAll();
    }

    public BdzArticle save(BdzArticle article) {
        return repository.save(article);
    }

    public void delete(BdzArticle article) {
        repository.delete(article);
    }
}
