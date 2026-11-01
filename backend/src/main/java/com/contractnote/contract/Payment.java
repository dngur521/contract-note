package com.contractnote.contract;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    private ContractDocument document;

    private String label;
    private Long amount;

    @Enumerated(EnumType.STRING)
    private PaymentRule ruleType;

    private LocalDate fixedDate;
    private Integer offsetDays;
    private boolean paid;

    @Column(columnDefinition = "text")
    private String quote;

    protected Payment() {}

    public Payment(ContractDocument document, String label, Long amount, PaymentRule ruleType,
                   LocalDate fixedDate, Integer offsetDays, String quote) {
        this.document = document;
        this.label = label;
        this.amount = amount;
        this.ruleType = ruleType;
        this.fixedDate = fixedDate;
        this.offsetDays = offsetDays;
        this.quote = quote;
    }

    void setContract(Contract contract) { this.contract = contract; }
    public void update(String label, Long amount, PaymentRule ruleType, LocalDate fixedDate, Integer offsetDays) {
        this.label = label;
        this.amount = amount;
        this.ruleType = ruleType;
        this.fixedDate = fixedDate;
        this.offsetDays = offsetDays;
    }
    public void setPaid(boolean paid) { this.paid = paid; }
    public Long getId() { return id; }
    public Contract getContract() { return contract; }
    public ContractDocument getDocument() { return document; }
    public String getLabel() { return label; }
    public Long getAmount() { return amount; }
    public PaymentRule getRuleType() { return ruleType; }
    public LocalDate getFixedDate() { return fixedDate; }
    public Integer getOffsetDays() { return offsetDays; }
    public boolean isPaid() { return paid; }
    public String getQuote() { return quote; }
}
