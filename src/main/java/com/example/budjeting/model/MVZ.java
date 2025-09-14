package com.example.budjeting.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mvzs")
public class MVZ {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cfo_id")
    private CFO cfo;

    @OneToOne(mappedBy = "mvz")
    private AppRequest application;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CFO getCfo() {
        return cfo;
    }

    public void setCfo(CFO cfo) {
        this.cfo = cfo;
    }

    public AppRequest getApplication() {
        return application;
    }

    public void setApplication(AppRequest application) {
        this.application = application;
    }
}
