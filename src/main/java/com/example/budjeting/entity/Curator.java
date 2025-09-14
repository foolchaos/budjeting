package com.example.budjeting.entity;

import jakarta.persistence.*;

@Entity
public class Curator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String department;

    @OneToOne
    private BudgetArticle budgetArticle;

    public Curator() {
    }

    public Curator(String fullName, String department) {
        this.fullName = fullName;
        this.department = department;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public BudgetArticle getBudgetArticle() { return budgetArticle; }
    public void setBudgetArticle(BudgetArticle budgetArticle) { this.budgetArticle = budgetArticle; }
}
