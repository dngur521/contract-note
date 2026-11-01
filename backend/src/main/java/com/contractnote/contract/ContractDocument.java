package com.contractnote.contract;

import jakarta.persistence.*;

@Entity
@Table(name = "document")
public class ContractDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Contract contract;

    private String fileName;
    private String mediaType;

    private byte[] content;

    @Column(columnDefinition = "text")
    private String transcript;

    protected ContractDocument() {}

    public ContractDocument(String fileName, String mediaType, byte[] content) {
        this.fileName = fileName;
        this.mediaType = mediaType;
        this.content = content;
    }

    void setContract(Contract contract) { this.contract = contract; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getMediaType() { return mediaType; }
    public byte[] getContent() { return content; }
    public String getTranscript() { return transcript; }
}
