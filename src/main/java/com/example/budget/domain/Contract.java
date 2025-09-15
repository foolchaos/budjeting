package com.example.budget.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
public class Contract {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name; // наименование

    private String internalNumber; // номер внутренний
    private String externalNumber; // номер внешний

    @NotNull
    private LocalDate contractDate; // дата договора

    @NotBlank
    private String responsible; // ФИО

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInternalNumber() { return internalNumber; }
    public void setInternalNumber(String internalNumber) { this.internalNumber = internalNumber; }
    public String getExternalNumber() { return externalNumber; }
    public void setExternalNumber(String externalNumber) { this.externalNumber = externalNumber; }
    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }
    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }
}
