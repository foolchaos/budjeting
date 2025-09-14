package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class Cfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "cfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mvz> mvzs = new ArrayList<>();

    @OneToOne(mappedBy = "cfo")
    private Request request;

    @PreRemove
    private void preRemove() {
        if (request != null) {
            request.setCfo(null);
        }
    }
}
