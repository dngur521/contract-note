package com.contractnote.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;

public final class ContractDtos {

    private ContractDtos() {}

    public record DocumentView(long id, String fileName, String mediaType) {}

    public record EvidenceView(String field, String quote, Long documentId, String fileName) {}

    public record PaymentView(long id, String label, Long amount, Long netAmount, String ruleType, LocalDate fixedDate,
                              Integer offsetDays, LocalDate dueDate, boolean paid, String quote, String fileName) {}

    public record ContractView(long id, String clientName, String projectName, LocalDate contractDate,
                               LocalDate deliveryDate, LocalDate acceptanceDate, Long totalAmount, Integer revisionLimit, String copyrightOwner,
                               boolean withholding, String status, List<String> notes, List<DocumentView> documents,
                               List<EvidenceView> evidences, List<PaymentView> payments) {}

    public record PaymentUpdate(Long id, @NotBlank String label, Long amount, @NotBlank String ruleType,
                                LocalDate fixedDate, Integer offsetDays) {}

    public record ContractUpdate(@NotBlank String clientName, String projectName, LocalDate contractDate,
                                 LocalDate deliveryDate, LocalDate acceptanceDate, Long totalAmount, Integer revisionLimit, String copyrightOwner,
                                 boolean withholding, boolean confirm, @Valid List<PaymentUpdate> payments) {}

    public record PaidUpdate(boolean paid) {}

    public record ContractCard(long id, String clientName, String text) {}

    public record ScheduleItem(long contractId, String clientName, String projectName, String type, String label,
                               LocalDate date, Long daysLeft, boolean overdue, Long amount, Long netAmount,
                               Long paymentId, boolean paid) {}
}
