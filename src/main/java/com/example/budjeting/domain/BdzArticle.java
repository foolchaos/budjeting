package com.example.budjeting.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class BdzArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private BdzArticle parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BdzArticle> children = new ArrayList<>();

    @OneToMany(mappedBy = "bdzArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoArticle> boArticles = new ArrayList<>();

    @OneToOne(mappedBy = "bdzArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Zgd zgd;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BdzArticle getParent() { return parent; }
    public void setParent(BdzArticle parent) { this.parent = parent; }
    public List<BdzArticle> getChildren() { return children; }
    public void setChildren(List<BdzArticle> children) { this.children = children; }
    public List<BoArticle> getBoArticles() { return boArticles; }
    public void setBoArticles(List<BoArticle> boArticles) { this.boArticles = boArticles; }
    public Zgd getZgd() { return zgd; }
    public void setZgd(Zgd zgd) { this.zgd = zgd; }
}
