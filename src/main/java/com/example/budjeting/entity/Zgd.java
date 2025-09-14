package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Zgd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String department;

    @OneToOne
    @JoinColumn(name = "budget_item_id")
    private BudgetItem budgetItem;
}
