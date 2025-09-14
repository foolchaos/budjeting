package com.example.budjeting.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cfos")
public class CFO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "cfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MVZ> mvzs = new ArrayList<>();

    @OneToOne(mappedBy = "cfo")
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

    public List<MVZ> getMvzs() {
        return mvzs;
    }

    public AppRequest getApplication() {
        return application;
    }

    public void setApplication(AppRequest application) {
        this.application = application;
    }
}
