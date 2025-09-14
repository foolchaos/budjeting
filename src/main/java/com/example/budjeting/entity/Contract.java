package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "contract")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String internalNumber;

    private String externalNumber;

    private LocalDate contractDate;

    private String responsible;
}
