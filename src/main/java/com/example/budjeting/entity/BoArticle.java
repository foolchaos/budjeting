package com.example.budjeting.entity;

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

    // getters and setters omitted for brevity
}
