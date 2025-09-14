package com.example.budjeting.model;

import jakarta.persistence.*;

@Entity
@Table(name = "supervisors")
public class Supervisor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String department;

    @OneToOne
    @JoinColumn(name = "budget_article_id")
    private BudgetArticle budgetArticle;

    @OneToOne(mappedBy = "supervisor")
    private AppRequest application;

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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
