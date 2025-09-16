package com.example.budget.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Bdz {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String code;

    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cfo_id")
    private Cfo cfo;

    @ManyToOne(fetch = FetchType.LAZY)
    private Bdz parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bdz> children = new ArrayList<>();

    @OneToMany(mappedBy = "bdz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bo> boItems = new ArrayList<>();

    @OneToOne(mappedBy = "bdz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Zgd zgd;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Cfo getCfo() { return cfo; }
    public void setCfo(Cfo cfo) { this.cfo = cfo; }
    public Bdz getParent() { return parent; }
    public void setParent(Bdz parent) { this.parent = parent; }
    public List<Bdz> getChildren() { return children; }
    public List<Bo> getBoItems() { return boItems; }
    public Zgd getZgd() { return zgd; }
    public void setZgd(Zgd zgd) { this.zgd = zgd; }
}
