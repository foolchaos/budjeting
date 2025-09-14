package com.example.budjeting.domain;

import jakarta.persistence.*;

@Entity
public class Zgd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fio;
    private String department;

    @OneToOne
    private BdzArticle bdzArticle;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFio() { return fio; }
    public void setFio(String fio) { this.fio = fio; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public BdzArticle getBdzArticle() { return bdzArticle; }
    public void setBdzArticle(BdzArticle bdzArticle) { this.bdzArticle = bdzArticle; }
}
