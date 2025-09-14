package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String number;

    @ManyToOne
    private BdzItem bdzItem;

    @ManyToOne
    private Cfo cfo;

    @ManyToOne
    private Mvz mvz;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @ManyToOne
    private BoArticle boArticle;

    private String vgo;
    private Double amount;
    private Double amountWithoutVat;
    private String subject;
    private String period;
    private Boolean inputObject;
    private String procurementMethod;

    @ManyToOne
    private Supervisor supervisor;

    @PrePersist
    public void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString();
        }
        if (bdzItem != null && supervisor == null) {
            supervisor = bdzItem.getSupervisor();
        }
    }
}
