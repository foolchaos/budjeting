package com.example.budget.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Cfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String code;

    @NotBlank
    private String name;

    @OneToMany(mappedBy = "cfo")
    private List<Mvz> mvzList = new ArrayList<>();

    @OneToMany(mappedBy = "cfo")
    private List<Bdz> bdzList = new ArrayList<>();

    @OneToMany(mappedBy = "cfo")
    private List<Request> requests = new ArrayList<>();

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Mvz> getMvzList() { return mvzList; }
    public List<Bdz> getBdzList() { return bdzList; }
    public List<Request> getRequests() { return requests; }
}
