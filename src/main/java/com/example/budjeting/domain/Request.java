package com.example.budjeting.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String number;
    @OneToOne
    private BdzArticle bdzArticle;
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
    private boolean introObject;
    private String procurementMethod;
    @OneToOne
    private Zgd zgd;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public BdzArticle getBdzArticle() { return bdzArticle; }
    public void setBdzArticle(BdzArticle bdzArticle) { this.bdzArticle = bdzArticle; }
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
    public boolean isIntroObject() { return introObject; }
    public void setIntroObject(boolean introObject) { this.introObject = introObject; }
    public String getProcurementMethod() { return procurementMethod; }
    public void setProcurementMethod(String procurementMethod) { this.procurementMethod = procurementMethod; }
    public Zgd getZgd() { return zgd; }
    public void setZgd(Zgd zgd) { this.zgd = zgd; }
}
