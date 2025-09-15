package com.example.budget.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Zgd {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String fullName;

    @NotBlank
    private String department;

    @OneToOne
    @JoinColumn(name = "bdz_id", unique = true)
    private Bdz bdz;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Bdz getBdz() { return bdz; }
    public void setBdz(Bdz bdz) { this.bdz = bdz; }
}
