package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class ApplicationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;

    @OneToOne(cascade = CascadeType.ALL)
    private BdzArticle bdzArticle;

    @OneToOne(cascade = CascadeType.ALL)
    private Cfo cfo;

    @OneToOne(cascade = CascadeType.ALL)
    private Mvz mvz;

    @OneToOne(cascade = CascadeType.ALL)
    private Contract contract;

    @OneToOne(cascade = CascadeType.ALL)
    private BoArticle boArticle;

    private String vgo;
    private BigDecimal amount;
    private BigDecimal amountWithoutVat;
    private String subject;
    private String period;
    private boolean introductory;
    private String purchaseMethod;

    @OneToOne(cascade = CascadeType.ALL)
    private Zgd zgd;

    @PrePersist
    public void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString();
        }
    }

    // getters and setters omitted for brevity
}
