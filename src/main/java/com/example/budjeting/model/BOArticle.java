package com.example.budjeting.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bo_articles")
public class BOArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_article_id")
    private BudgetArticle budgetArticle;

    @OneToOne(mappedBy = "boArticle")
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

    public BudgetArticle getBudgetArticle() {
        return budgetArticle;
    }

    public void setBudgetArticle(BudgetArticle budgetArticle) {
        this.budgetArticle = budgetArticle;
    }

    public AppRequest getApplication() {
        return application;
    }

    public void setApplication(AppRequest application) {
        this.application = application;
    }
}
