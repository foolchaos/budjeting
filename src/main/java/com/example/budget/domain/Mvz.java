package com.example.budget.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Mvz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String code;

    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    private Cfo cfo;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Cfo getCfo() { return cfo; }
    public void setCfo(Cfo cfo) { this.cfo = cfo; }
}
