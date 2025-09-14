package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String internalNumber;
    private String externalNumber;
    private LocalDate date;
    private String responsible;

    @OneToOne(mappedBy = "contract")
    private Request request;
}
