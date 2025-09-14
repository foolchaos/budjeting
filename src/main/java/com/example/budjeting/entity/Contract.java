package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String internalNumber;
    private String externalNumber;
    private LocalDate contractDate;
    private String responsible;

    @OneToOne(mappedBy = "contract")
    private Application application;
}
