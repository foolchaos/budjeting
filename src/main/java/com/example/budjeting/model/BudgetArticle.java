package com.example.budjeting.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budget_articles")
public class BudgetArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BudgetArticle parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetArticle> children = new ArrayList<>();

    @OneToMany(mappedBy = "budgetArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BOArticle> boArticles = new ArrayList<>();

    @OneToOne(mappedBy = "budgetArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Supervisor supervisor;

    @OneToOne(mappedBy = "budgetArticle")
    private AppRequest application;

    public Long getId() {
        return id;
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

    public List<BOArticle> getBoArticles() {
        return boArticles;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public AppRequest getApplication() {
        return application;
    }

    public void setApplication(AppRequest application) {
        this.application = application;
    }
}
