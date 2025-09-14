package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Cfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;

    @OneToMany(mappedBy = "cfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Mvz> mvzs = new HashSet<>();

    // getters and setters omitted for brevity
}
