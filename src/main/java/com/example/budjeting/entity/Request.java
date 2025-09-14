package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String number;

    @OneToOne
    @JoinColumn(name = "budget_item_id")
    private BudgetItem budgetItem;

    @OneToOne
    @JoinColumn(name = "cfo_id")
    private Cfo cfo;

    @OneToOne
    @JoinColumn(name = "mvz_id")
    private Mvz mvz;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @OneToOne
    @JoinColumn(name = "bo_article_id")
    private BoArticle boArticle;

    private String vgo;
    private BigDecimal amount;
    private BigDecimal amountWithoutVat;
    private String subject;
    private String period;
    private boolean introductoryObject;
    private String procurementMethod;

    @ManyToOne
    @JoinColumn(name = "zgd_id")
    private Zgd zgd;

    @PrePersist
    public void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString();
        }
    }
}
