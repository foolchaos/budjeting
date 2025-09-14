package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String number;

    @OneToOne
    private BudgetArticle budgetArticle;

    @OneToOne
    private Cfo cfo;

    @OneToOne
    private Mvz mvz;

    @OneToOne(cascade = CascadeType.ALL)
    private Contract contract;

    @OneToOne
    private BoArticle boArticle;

    private String vgo;
    private BigDecimal amount;
    private BigDecimal amountWithoutVat;
    private String subject;
    private String period;
    private boolean inputObject;
    private String purchaseMethod;

    @OneToOne
    private Curator curator;

    @PrePersist
    public void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString().substring(0, 8);
        }
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public BudgetArticle getBudgetArticle() { return budgetArticle; }
    public void setBudgetArticle(BudgetArticle budgetArticle) { this.budgetArticle = budgetArticle; }
    public Cfo getCfo() { return cfo; }
    public void setCfo(Cfo cfo) { this.cfo = cfo; }
    public Mvz getMvz() { return mvz; }
    public void setMvz(Mvz mvz) { this.mvz = mvz; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public BoArticle getBoArticle() { return boArticle; }
    public void setBoArticle(BoArticle boArticle) { this.boArticle = boArticle; }
    public String getVgo() { return vgo; }
    public void setVgo(String vgo) { this.vgo = vgo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAmountWithoutVat() { return amountWithoutVat; }
    public void setAmountWithoutVat(BigDecimal amountWithoutVat) { this.amountWithoutVat = amountWithoutVat; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public boolean isInputObject() { return inputObject; }
    public void setInputObject(boolean inputObject) { this.inputObject = inputObject; }
    public String getPurchaseMethod() { return purchaseMethod; }
    public void setPurchaseMethod(String purchaseMethod) { this.purchaseMethod = purchaseMethod; }
    public Curator getCurator() { return curator; }
    public void setCurator(Curator curator) { this.curator = curator; }
}
