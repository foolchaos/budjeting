package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class BudgetArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private BudgetArticle parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetArticle> children = new ArrayList<>();

    @OneToMany(mappedBy = "budgetArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoArticle> boArticles = new ArrayList<>();

    @OneToOne(mappedBy = "budgetArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Curator curator;

    public BudgetArticle() {
    }

    public BudgetArticle(String code, String name) {
        this.code = code;
        this.name = name;
    }

    // getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BudgetArticle getParent() {
        return parent;
    }

    public void setParent(BudgetArticle parent) {
        this.parent = parent;
    }

    public List<BudgetArticle> getChildren() {
        return children;
    }

    public void setChildren(List<BudgetArticle> children) {
        this.children = children;
    }

    public List<BoArticle> getBoArticles() {
        return boArticles;
    }

    public void setBoArticles(List<BoArticle> boArticles) {
        this.boArticles = boArticles;
    }

    public Curator getCurator() {
        return curator;
    }

    public void setCurator(Curator curator) {
        this.curator = curator;
    }
}
