package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class BdzItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;

    @ManyToOne
    private BdzItem parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BdzItem> children = new HashSet<>();

    @OneToOne(mappedBy = "bdzItem", cascade = CascadeType.ALL)
    private Supervisor supervisor;

    @OneToMany(mappedBy = "bdzItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BoArticle> boArticles = new HashSet<>();
}
