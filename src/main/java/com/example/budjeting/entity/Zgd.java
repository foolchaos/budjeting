package com.example.budjeting.entity;

import jakarta.persistence.*;

@Entity
public class Zgd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String department;

    @OneToOne
    private BdzArticle bdzArticle;

    // getters and setters omitted for brevity
}
