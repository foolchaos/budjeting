package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Entity
@Table(name = "request")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;

    @ManyToOne
    @JoinColumn(name = "bdz_id")
    private Bdz bdz;

    @ManyToOne
    @JoinColumn(name = "cfo_id")
    private Cfo cfo;

    @ManyToOne
    @JoinColumn(name = "mvz_id")
    private Mvz mvz;

    @ManyToOne
    @JoinColumn(name = "bo_id")
    private Bo bo;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    private String vgo;

    private BigDecimal amount;

    private BigDecimal amountWithoutVat;

    private String subject;

    private String period;

    private boolean introductory;

    private String procurementMethod;

    @ManyToOne
    @JoinColumn(name = "curator_id")
    private Curator curator;

    @PrePersist
    public void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString();
        }
        if (bdz != null && bdz.getCurator() != null) {
            curator = bdz.getCurator();
        }
    }
}
