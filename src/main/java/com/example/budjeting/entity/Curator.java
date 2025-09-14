package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "curator")
public class Curator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fio;

    private String department;

    @OneToOne
    @JoinColumn(name = "bdz_id")
    private Bdz bdz;
}
