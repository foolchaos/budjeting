package com.example.budjeting.domain;

import jakarta.persistence.*;

@Entity
public class BoArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private BdzArticle bdzArticle;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BdzArticle getBdzArticle() { return bdzArticle; }
    public void setBdzArticle(BdzArticle bdzArticle) { this.bdzArticle = bdzArticle; }
}
