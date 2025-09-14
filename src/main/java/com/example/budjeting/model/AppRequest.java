package com.example.budjeting.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class AppRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @OneToOne
    @JoinColumn(name = "budget_article_id")
    private BudgetArticle budgetArticle;

    @OneToOne
    @JoinColumn(name = "cfo_id")
    private CFO cfo;

    @OneToOne
    @JoinColumn(name = "mvz_id")
    private MVZ mvz;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @OneToOne
    @JoinColumn(name = "bo_article_id")
    private BOArticle boArticle;

    private String vgo;

    private BigDecimal amount;

    private BigDecimal amountWithoutVat;

    private String subject;

    private String period;

    private boolean inputObject;

    private String procurementMethod;

    @OneToOne
    @JoinColumn(name = "supervisor_id")
    private Supervisor supervisor;

    @PrePersist
    public void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString();
        }
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public BudgetArticle getBudgetArticle() {
        return budgetArticle;
    }

    public void setBudgetArticle(BudgetArticle budgetArticle) {
        this.budgetArticle = budgetArticle;
    }

    public CFO getCfo() {
        return cfo;
    }

    public void setCfo(CFO cfo) {
        this.cfo = cfo;
    }

    public MVZ getMvz() {
        return mvz;
    }

    public void setMvz(MVZ mvz) {
        this.mvz = mvz;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public BOArticle getBoArticle() {
        return boArticle;
    }

    public void setBoArticle(BOArticle boArticle) {
        this.boArticle = boArticle;
    }

    public String getVgo() {
        return vgo;
    }

    public void setVgo(String vgo) {
        this.vgo = vgo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountWithoutVat() {
        return amountWithoutVat;
    }

    public void setAmountWithoutVat(BigDecimal amountWithoutVat) {
        this.amountWithoutVat = amountWithoutVat;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public boolean isInputObject() {
        return inputObject;
    }

    public void setInputObject(boolean inputObject) {
        this.inputObject = inputObject;
    }

    public String getProcurementMethod() {
        return procurementMethod;
    }

    public void setProcurementMethod(String procurementMethod) {
        this.procurementMethod = procurementMethod;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Supervisor supervisor) {
        this.supervisor = supervisor;
    }
}
