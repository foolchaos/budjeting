package com.example.budget.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_request_header")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotNull
    @Column(name = "request_year", nullable = false)
    private Integer year;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cfo_id", unique = true)
    private Cfo cfo;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestPosition> positions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Cfo getCfo() {
        return cfo;
    }

    public void setCfo(Cfo cfo) {
        this.cfo = cfo;
    }

    public List<RequestPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<RequestPosition> positions) {
        this.positions = positions;
    }
}
