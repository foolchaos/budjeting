package com.example.budjeting.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "bdz")
public class Bdz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Bdz parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Bdz> children = new ArrayList<>();

    @OneToMany(mappedBy = "bdz", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Bo> bos = new ArrayList<>();

    @OneToOne(mappedBy = "bdz", cascade = CascadeType.ALL, orphanRemoval = true)
    private Curator curator;
}
