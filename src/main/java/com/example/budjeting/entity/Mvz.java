package com.example.budjeting.entity;

import jakarta.persistence.*;

@Entity
public class Mvz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cfo cfo;

    @OneToOne(mappedBy = "mvz")
    private Request request;

    public Mvz() {
    }

    public Mvz(String code, String name) {
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
    public Cfo getCfo() { return cfo; }
    public void setCfo(Cfo cfo) { this.cfo = cfo; }
    public Request getRequest() { return request; }
    public void setRequest(Request request) { this.request = request; }
}
