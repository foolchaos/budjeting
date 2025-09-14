package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String internalNumber;
    private String externalNumber;
    private LocalDate contractDate;
    private String responsible;

    // getters and setters omitted for brevity
}
