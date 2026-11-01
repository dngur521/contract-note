package com.contractnote.contract;

import jakarta.persistence.*;

/** 계약 항목의 근거 문장. */
@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    private ContractDocument document;

    private String field;

    @Column(columnDefinition = "text")
    private String quote;

    protected Evidence() {}

    public Evidence(ContractDocument document, String field, String quote) {
        this.document = document;
        this.field = field;
        this.quote = quote;
    }

    void setContract(Contract contract) { this.contract = contract; }
    public ContractDocument getDocument() { return document; }
    public String getField() { return field; }
    public String getQuote() { return quote; }
}
