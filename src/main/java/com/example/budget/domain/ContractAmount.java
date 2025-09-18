package com.example.budget.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract_amount",
        uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "contract_id"}))
public class ContractAmount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @OneToMany(mappedBy = "contractAmount")
    private List<RequestPosition> requestPositions = new ArrayList<>();

    @Column(nullable = false, precision = 19, scale = 3)
    private BigDecimal amount = BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public List<RequestPosition> getRequestPositions() {
        return requestPositions;
    }

    public void setRequestPositions(List<RequestPosition> requestPositions) {
        this.requestPositions = requestPositions;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
