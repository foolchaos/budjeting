package com.example.budget.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "app_request")
public class Request {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String number; // уникальный номер (генерируется случайно)

    @ManyToOne
    @JoinColumn(name = "bdz_id")
    private Bdz bdz;

    @ManyToOne
    @JoinColumn(name = "cfo_id")
    private Cfo cfo;

    @ManyToOne
    @JoinColumn(name = "mvz_id")
    private Mvz mvz;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contract_id", unique = true)
    private Contract contract;

    @ManyToOne
    @JoinColumn(name = "bo_id")
    private Bo bo;

    private String vgo; // ВГО
    @NotNull
    private BigDecimal amount; // Сумма (млн руб.)
    @NotNull
    private BigDecimal amountNoVat; // Сумма без НДС (млн руб.)
    private String subject; // Предмет договора
    private String period; // Период (месяц)
    private boolean inputObject; // Вводный объект
    private String procurementMethod; // Способ закупки

    @ManyToOne
    @JoinColumn(name = "zgd_id")
    private Zgd zgd; // автоподстановка из БДЗ

    @PrePersist
    public void prePersist() {
        if (number == null) {
            number = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public Bdz getBdz() { return bdz; }
    public void setBdz(Bdz bdz) { this.bdz = bdz; }
    public Cfo getCfo() { return cfo; }
    public void setCfo(Cfo cfo) { this.cfo = cfo; }
    public Mvz getMvz() { return mvz; }
    public void setMvz(Mvz mvz) { this.mvz = mvz; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public Bo getBo() { return bo; }
    public void setBo(Bo bo) { this.bo = bo; }
    public String getVgo() { return vgo; }
    public void setVgo(String vgo) { this.vgo = vgo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAmountNoVat() { return amountNoVat; }
    public void setAmountNoVat(BigDecimal amountNoVat) { this.amountNoVat = amountNoVat; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public boolean isInputObject() { return inputObject; }
    public void setInputObject(boolean inputObject) { this.inputObject = inputObject; }
    public String getProcurementMethod() { return procurementMethod; }
    public void setProcurementMethod(String procurementMethod) { this.procurementMethod = procurementMethod; }
    public Zgd getZgd() { return zgd; }
    public void setZgd(Zgd zgd) { this.zgd = zgd; }
}
