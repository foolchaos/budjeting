package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Cfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;

    @OneToMany(mappedBy = "cfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mvz> mvzs = new ArrayList<>();

    public Cfo() {
    }

    public Cfo(String code, String name) {
        this.code = code;
        this.name = name;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Mvz> getMvzs() { return mvzs; }
    public void setMvzs(List<Mvz> mvzs) { this.mvzs = mvzs; }
}
