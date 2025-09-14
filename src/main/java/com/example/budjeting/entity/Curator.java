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
    @JoinColumn(name = "bdz_article_id")
    private BdzArticle bdzArticle;

    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BdzArticle getBdzArticle() {
        return bdzArticle;
    }

    public void setBdzArticle(BdzArticle bdzArticle) {
        this.bdzArticle = bdzArticle;
    }
}
