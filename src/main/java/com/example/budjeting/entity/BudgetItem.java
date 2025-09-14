package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class BudgetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private BudgetItem parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetItem> children = new ArrayList<>();

    @OneToMany(mappedBy = "budgetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoArticle> boArticles = new ArrayList<>();

    @OneToOne(mappedBy = "budgetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Zgd zgd;

    @OneToOne(mappedBy = "budgetItem")
    private Request request;

    @PreRemove
    private void preRemove() {
        if (request != null) {
            request.setBudgetItem(null);
        }
    }
}
