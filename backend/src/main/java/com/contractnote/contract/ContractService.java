package com.contractnote.contract;

import com.contractnote.ai.ContractAi;
import com.contractnote.ai.ExtractionResult;
import com.contractnote.ai.UploadedFile;
import com.contractnote.common.NotFoundException;
import com.contractnote.contract.ContractDtos.*;
import com.contractnote.rag.ChunkRepository;
import com.contractnote.rag.EmbeddingService;
import com.contractnote.rag.TextChunker;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ContractService {

    private final ContractRepository contracts;
    private final PaymentRepository payments;
    private final ContractAi ai;
    private final EmbeddingService embeddings;
    private final ChunkRepository chunks;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ContractService(ContractRepository contracts, PaymentRepository payments, ContractAi ai,
                           EmbeddingService embeddings, ChunkRepository chunks, TransactionTemplate tx, Clock clock) {
        this.contracts = contracts;
        this.payments = payments;
        this.ai = ai;
        this.embeddings = embeddings;
        this.chunks = chunks;
        this.tx = tx;
        this.clock = clock;
    }

    /** 기록을 AI로 정리해 확인 대기(DRAFT) 계약을 만든다. 오래 걸리는 AI 호출은 트랜잭션 밖에서 한다. */
    public ContractView extract(List<UploadedFile> files) {
        ExtractionResult result = ai.extract(files);
        Map<Integer, String> transcripts = new HashMap<>();
        result.transcripts().forEach(t -> transcripts.put(t.documentNumber(), t.text()));

        // 문서 순서별로 (문장 묶음, 벡터)를 미리 계산한다.
        List<List<String>> chunkTexts = new ArrayList<>();
        List<List<float[]>> chunkVectors = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            List<String> texts = TextChunker.split(transcripts.getOrDefault(i + 1, ""));
            chunkTexts.add(texts);
            chunkVectors.add(texts.stream().map(t -> embeddings.embed(TextChunker.withoutHeader(t))).toList());
        }

        return tx.execute(status -> {
            Contract contract = new Contract();
            contract.setClientName(blankToNull(result.clientName()));
            contract.setProjectName(blankToNull(result.projectName()));
            contract.setContractDate(parseDate(result.contractDate()));
            contract.setDeliveryDate(parseDate(result.deliveryDate()));
            contract.setAcceptanceDate(parseDate(result.acceptanceDate()));
            contract.setTotalAmount(result.totalAmount() > 0 ? result.totalAmount() : null);
            contract.setRevisionLimit(result.revisionLimit() >= 0 ? result.revisionLimit() : null);
            contract.setCopyrightOwner(blankToNull(result.copyrightOwner()));
            List<String> notes = new ArrayList<>(result.conflicts());
            contract.setWithholding(decideWithholding(result, notes, transcripts.values()));
            contract.setNotes(String.join("\n", notes));

            List<ContractDocument> documents = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                UploadedFile file = files.get(i);
                ContractDocument document = new ContractDocument(file.fileName(), file.mediaType(), file.bytes());
                document.setTranscript(transcripts.getOrDefault(i + 1, ""));
                contract.addDocument(document);
                documents.add(document);
            }
            // AI가 낸 근거 문장이 읽은 글자에 실제로 있는지 코드로 확인하고, 없으면 근거로 보여 주지 않는다.
            String allText = EvidenceChecker.normalize(String.join("\n", transcripts.values()));
            for (var e : result.evidences()) {
                if (EvidenceChecker.isIn(e.quote(), allText)) {
                    contract.addEvidence(new Evidence(documentAt(documents, e.documentNumber()), e.field(), e.quote()));
                }
            }
            for (var p : result.payments()) {
                String quote = EvidenceChecker.isIn(p.quote(), allText) ? p.quote() : null;
                contract.addPayment(new Payment(documentAt(documents, p.documentNumber()), p.label(),
                        p.amount() > 0 ? p.amount() : null, PaymentRule.parse(p.ruleType()),
                        parseDate(p.fixedDate()), p.offsetDays(), quote));
            }
            contracts.saveAndFlush(contract);

            for (int i = 0; i < documents.size(); i++) {
                for (int j = 0; j < chunkTexts.get(i).size(); j++) {
                    chunks.insert(contract.getId(), documents.get(i).getId(), chunkTexts.get(i).get(j), chunkVectors.get(i).get(j));
                }
            }
            return toView(contract);
        });
    }

    @Transactional(readOnly = true)
    public List<ContractView> list() {
        return contracts.findAllByOrderByCreatedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ContractView get(long id) {
        return toView(find(id));
    }

    /** 사용자가 고친 내용을 저장한다. confirm이면 일정에 등록된다. */
    @Transactional
    public ContractView update(long id, ContractUpdate request) {
        Contract contract = find(id);
        contract.setClientName(request.clientName());
        contract.setProjectName(request.projectName());
        contract.setContractDate(request.contractDate());
        contract.setDeliveryDate(request.deliveryDate());
        contract.setAcceptanceDate(request.acceptanceDate());
        contract.setTotalAmount(request.totalAmount());
        contract.setRevisionLimit(request.revisionLimit());
        contract.setCopyrightOwner(request.copyrightOwner());
        contract.setWithholding(request.withholding());
        if (request.confirm()) {
            contract.setStatus(ContractStatus.CONFIRMED);
        }
        if (request.payments() != null) {
            applyPayments(contract, request.payments());
        }
        return toView(contract);
    }

    private void applyPayments(Contract contract, List<PaymentUpdate> updates) {
        Map<Long, Payment> existing = new HashMap<>();
        contract.getPayments().forEach(p -> existing.put(p.getId(), p));
        List<Payment> kept = new ArrayList<>();
        for (PaymentUpdate u : updates) {
            PaymentRule rule = PaymentRule.parse(u.ruleType());
            Payment payment = u.id() == null ? null : existing.get(u.id());
            if (payment == null) {
                payment = new Payment(null, u.label(), u.amount(), rule, u.fixedDate(), u.offsetDays(), null);
                contract.addPayment(payment);
            } else {
                payment.update(u.label(), u.amount(), rule, u.fixedDate(), u.offsetDays());
            }
            kept.add(payment);
        }
        contract.getPayments().retainAll(kept);
    }

    @Transactional
    public void delete(long id) {
        contracts.delete(find(id));
    }

    @Transactional
    public void setPaid(long paymentId, boolean paid) {
        payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("지급 항목을 찾을 수 없습니다."))
                .setPaid(paid);
    }

    /** 확인된 계약의 일정. 안 받은 돈 중 기한이 지난 것부터 보여 준다. */
    @Transactional(readOnly = true)
    public List<ScheduleItem> schedule() {
        LocalDate today = LocalDate.now(clock);
        List<ScheduleItem> items = new ArrayList<>();
        for (Contract c : contracts.findByStatus(ContractStatus.CONFIRMED)) {
            if (c.getDeliveryDate() != null && !c.getDeliveryDate().isBefore(today)) {
                items.add(new ScheduleItem(c.getId(), c.getClientName(), c.getProjectName(), "DELIVERY", "납품",
                        c.getDeliveryDate(), ChronoUnit.DAYS.between(today, c.getDeliveryDate()), false,
                        null, null, null, false));
            }
            for (Payment p : c.getPayments()) {
                LocalDate due = dueDate(c, p);
                // 날짜를 아직 모르는 돈도 일정에서 사라지지 않게 그대로 보여 준다.
                Long daysLeft = due == null ? null : ChronoUnit.DAYS.between(today, due);
                boolean overdue = !p.isPaid() && daysLeft != null && daysLeft < 0;
                items.add(new ScheduleItem(c.getId(), c.getClientName(), c.getProjectName(), "PAYMENT", p.getLabel(),
                        due, daysLeft, overdue, p.getAmount(), netAmount(c, p), p.getId(), p.isPaid()));
            }
        }
        items.sort(Comparator.comparing(ScheduleItem::paid)
                .thenComparing(ScheduleItem::date, Comparator.nullsFirst(Comparator.naturalOrder())));
        return items;
    }

    /** 질문 답변에 넘길 최종 조건 요약. 지급일은 코드로 계산한 값이다. */
    @Transactional(readOnly = true)
    public ContractCard card(long id) {
        Contract c = find(id);
        StringBuilder text = new StringBuilder(c.getStatus() == ContractStatus.CONFIRMED ? "사용자가 확인한 조건\n" : "AI가 정리한 조건(사용자 확인 전)\n");
        text.append("고객: ").append(orUnknown(c.getClientName())).append('\n');
        text.append("프로젝트: ").append(orUnknown(c.getProjectName())).append('\n');
        text.append("총 금액: ").append(c.getTotalAmount() == null ? "기록 없음" : c.getTotalAmount() + "원").append('\n');
        text.append("계약일: ").append(orUnknown(c.getContractDate())).append('\n');
        text.append("납품일: ").append(orUnknown(c.getDeliveryDate())).append('\n');
        text.append("검수일: ").append(orUnknown(c.getAcceptanceDate())).append('\n');
        text.append("수정 횟수: ").append(c.getRevisionLimit() == null ? "기록 없음" : c.getRevisionLimit() + "회").append('\n');
        text.append("저작권: ").append(orUnknown(c.getCopyrightOwner())).append('\n');
        text.append("3.3% 원천징수: ").append(c.isWithholding() ? "함" : "기록 없음").append('\n');
        for (Payment p : c.getPayments()) {
            text.append("지급 - ").append(p.getLabel()).append(": ")
                    .append(p.getAmount() == null ? "금액 기록 없음" : p.getAmount() + "원").append(", 기한 ")
                    .append(orUnknown(dueDate(c, p))).append(p.isPaid() ? ", 받음" : "").append('\n');
        }
        if (c.getNotes() != null && !c.getNotes().isBlank()) {
            text.append("바뀐 내용:\n").append(c.getNotes());
        }
        return new ContractCard(c.getId(), c.getClientName(), text.toString());
    }

    private static String orUnknown(Object value) {
        return value == null ? "기록 없음" : value.toString();
    }

    private Contract find(long id) {
        return contracts.findById(id).orElseThrow(() -> new NotFoundException("계약을 찾을 수 없습니다."));
    }

    private ContractView toView(Contract c) {
        List<String> notes = new ArrayList<>(c.getNotes() == null || c.getNotes().isBlank()
                ? List.of() : Arrays.asList(c.getNotes().split("\n")));
        // AI가 총액만 바꾸고 지급 항목을 그대로 두는 실수를 코드로 잡는다. 수정하면 다시 계산된다.
        Long sum = PaymentCalculator.mismatchedSum(c.getTotalAmount(), c.getPayments().stream().map(Payment::getAmount).toList());
        if (sum != null) {
            notes.add("지급 항목 합계 %,d원이 총 금액 %,d원과 다릅니다. 금액을 확인해 주세요.".formatted(sum, c.getTotalAmount()));
        }
        return new ContractView(c.getId(), c.getClientName(), c.getProjectName(), c.getContractDate(),
                c.getDeliveryDate(), c.getAcceptanceDate(), c.getTotalAmount(), c.getRevisionLimit(), c.getCopyrightOwner(),
                c.isWithholding(), c.getStatus().name(), notes,
                c.getDocuments().stream()
                        .map(d -> new DocumentView(d.getId(), d.getFileName(), d.getMediaType())).toList(),
                c.getEvidences().stream()
                        .map(e -> new EvidenceView(e.getField(), e.getQuote(), documentId(e.getDocument()), fileName(e.getDocument())))
                        .toList(),
                c.getPayments().stream()
                        .map(p -> new PaymentView(p.getId(), p.getLabel(), p.getAmount(), netAmount(c, p),
                                p.getRuleType().name(), p.getFixedDate(), p.getOffsetDays(), dueDate(c, p), p.isPaid(),
                                p.getQuote(), fileName(p.getDocument())))
                        .toList());
    }

    private static LocalDate dueDate(Contract c, Payment p) {
        return PaymentCalculator.dueDate(p.getRuleType(), p.getFixedDate(), p.getOffsetDays(),
                new PaymentCalculator.BaseDates(c.getContractDate(), c.getDeliveryDate(), c.getAcceptanceDate()));
    }

    private static Long netAmount(Contract c, Payment p) {
        if (p.getAmount() == null) {
            return null;
        }
        return c.isWithholding() ? PaymentCalculator.afterWithholding(p.getAmount()) : p.getAmount();
    }

    /**
     * 표준계약서에는 "사업소득자인 경우 공제한다"는 일반 조항이 있다. 그것만으로 이 계약에 적용한다고 단정하지 않고
     * 사용자에게 확인을 요청한다. AI가 근거 문장을 빠뜨린 경우에는 읽은 글자에 3.3%나 원천징수가 실제로 있는지 본다.
     */
    private static boolean decideWithholding(ExtractionResult result, List<String> notes, Collection<String> texts) {
        if (!result.withholding()) {
            return false;
        }
        List<String> quotes = result.evidences().stream()
                .filter(e -> "withholding".equals(e.field()))
                .map(ExtractionResult.FieldEvidence::quote)
                .toList();
        boolean mentioned = texts.stream().anyMatch(t -> t.contains("3.3") || t.contains("원천"));
        boolean conditionalOnly = !quotes.isEmpty() && quotes.stream().allMatch(q -> q.contains("경우"));
        if (conditionalOnly || (quotes.isEmpty() && !mentioned)) {
            notes.add("원천징수가 조건부 일반 조항으로만 적혀 있어 '안 함'으로 두었습니다. 3.3%를 떼고 받는다면 체크해 주세요.");
            return false;
        }
        return true;
    }

    private static ContractDocument documentAt(List<ContractDocument> documents, int number) {
        return number >= 1 && number <= documents.size() ? documents.get(number - 1) : null;
    }

    private static Long documentId(ContractDocument document) {
        return document == null ? null : document.getId();
    }

    private static String fileName(ContractDocument document) {
        return document == null ? null : document.getFileName();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
