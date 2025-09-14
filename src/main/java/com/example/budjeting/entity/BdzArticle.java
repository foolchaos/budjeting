package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

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
    private Set<BdzArticle> children = new HashSet<>();

    @OneToMany(mappedBy = "bdzArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BoArticle> boArticles = new HashSet<>();

    @OneToOne(mappedBy = "bdzArticle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Zgd zgd;

    // getters and setters omitted for brevity
}
