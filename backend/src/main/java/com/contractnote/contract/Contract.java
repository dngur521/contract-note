package com.contractnote.contract;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientName;
    private String projectName;
    private LocalDate contractDate;
    private LocalDate deliveryDate;
    private LocalDate acceptanceDate;
    private Long totalAmount;
    private Integer revisionLimit;
    private String copyrightOwner;
    private boolean withholding;

    @Enumerated(EnumType.STRING)
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(columnDefinition = "text")
    private String notes;

    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ContractDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Evidence> evidences = new ArrayList<>();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Payment> payments = new ArrayList<>();

    public void addDocument(ContractDocument document) {
        document.setContract(this);
        documents.add(document);
    }

    public void addEvidence(Evidence evidence) {
        evidence.setContract(this);
        evidences.add(evidence);
    }

    public void addPayment(Payment payment) {
        payment.setContract(this);
        payments.add(payment);
    }

    public Long getId() { return id; }
    public String getClientName() { return clientName; }
    public void setClientName(String v) { clientName = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { projectName = v; }
    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate v) { contractDate = v; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate v) { deliveryDate = v; }
    public LocalDate getAcceptanceDate() { return acceptanceDate; }
    public void setAcceptanceDate(LocalDate v) { acceptanceDate = v; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long v) { totalAmount = v; }
    public Integer getRevisionLimit() { return revisionLimit; }
    public void setRevisionLimit(Integer v) { revisionLimit = v; }
    public String getCopyrightOwner() { return copyrightOwner; }
    public void setCopyrightOwner(String v) { copyrightOwner = v; }
    public boolean isWithholding() { return withholding; }
    public void setWithholding(boolean v) { withholding = v; }
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus v) { status = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ContractDocument> getDocuments() { return documents; }
    public List<Evidence> getEvidences() { return evidences; }
    public List<Payment> getPayments() { return payments; }
}
