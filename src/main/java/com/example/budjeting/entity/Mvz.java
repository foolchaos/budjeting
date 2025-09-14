package com.example.budjeting.entity;

import jakarta.persistence.*;

@Entity
public class Mvz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cfo cfo;

    // getters and setters omitted for brevity
}
