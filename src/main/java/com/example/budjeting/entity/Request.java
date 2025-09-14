package com.example.budjeting.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String number;

    @OneToOne
    private BdzArticle bdzArticle;

    @OneToOne
    private Cfo cfo;

    @OneToOne
    private Mvz mvz;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @OneToOne
    private BoArticle boArticle;

    private String vgo;
    private BigDecimal amount;
    private BigDecimal amountWithoutVat;
    private String contractSubject;
    private String period;
    private boolean introductoryObject;
    private String procurementMethod;

    @ManyToOne
    private Curator curator;

    @PrePersist
    private void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString();
        }
    }

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
    public String getContractSubject() { return contractSubject; }
    public void setContractSubject(String contractSubject) { this.contractSubject = contractSubject; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public boolean isIntroductoryObject() { return introductoryObject; }
    public void setIntroductoryObject(boolean introductoryObject) { this.introductoryObject = introductoryObject; }
    public String getProcurementMethod() { return procurementMethod; }
    public void setProcurementMethod(String procurementMethod) { this.procurementMethod = procurementMethod; }
    public Curator getCurator() { return curator; }
    public void setCurator(Curator curator) { this.curator = curator; }
}
