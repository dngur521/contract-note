package com.contractnote.rag;

import com.contractnote.ai.AnswerResult;
import com.contractnote.ai.ContractAi;
import com.contractnote.ai.SourceChunk;
import com.contractnote.contract.ContractService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** RAG: 질문을 벡터로 바꿔 내 기록을 검색하고, 찾은 문장 안에서만 AI가 답하게 한다. */
@RestController
public class AskController {

    public record AskRequest(@NotBlank String question, Long contractId) {}

    public record Source(Long documentId, String clientName, String fileName, String text, double score) {}

    public record AskResponse(boolean found, String answer, List<Source> sources) {}

    private static final String NOT_FOUND = "기록에서 찾을 수 없습니다.";
    private static final int CANDIDATES = 500;
    private static final int NEIGHBORS = 1;
    private static final int CARD_HITS = 3;

    private final EmbeddingService embeddings;
    private final ChunkRepository chunks;
    private final ContractAi ai;
    private final ContractService contracts;
    private final int topK;
    private final double minScore;

    public AskController(EmbeddingService embeddings, ChunkRepository chunks, ContractAi ai, ContractService contracts,
                         @Value("${app.rag.top-k}") int topK, @Value("${app.rag.min-score}") double minScore) {
        this.embeddings = embeddings;
        this.chunks = chunks;
        this.ai = ai;
        this.contracts = contracts;
        this.topK = topK;
        this.minScore = minScore;
    }

    @PostMapping("/api/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        List<SourceChunk> candidates = chunks.search(embeddings.embed(request.question()), request.contractId(), CANDIDATES);
        List<SourceChunk> ranked = HybridRanker.rank(request.question(), candidates, topK).stream()
                .filter(chunk -> chunk.score() >= minScore)
                .toList();
        if (ranked.isEmpty()) {
            return new AskResponse(false, NOT_FOUND, List.of());
        }
        List<SourceChunk> found = new ArrayList<>(cards(ranked));
        found.addAll(chunks.withNeighbors(ranked, NEIGHBORS));

        AnswerResult result = ai.answer(request.question(), found);
        if (!result.found() || result.answer().isBlank() || result.answer().contains("기록 없음")) {
            return new AskResponse(false, NOT_FOUND, List.of());
        }
        List<Source> sources = new ArrayList<>();
        for (int number : result.usedSources()) {
            if (number >= 1 && number <= found.size()) {
                SourceChunk s = found.get(number - 1);
                sources.add(new Source(s.documentId() < 0 ? null : s.documentId(), s.clientName(), s.fileName(), s.text(), s.score()));
            }
        }
        return new AskResponse(true, result.answer(), sources);
    }

    /** 가장 관련 높은 계약들의 최종 조건 요약을 근거 맨 앞에 넣는다. */
    private List<SourceChunk> cards(List<SourceChunk> ranked) {
        return ranked.stream().limit(CARD_HITS).map(SourceChunk::contractId).distinct().map(contracts::card)
                .map(card -> new SourceChunk(-card.id(), card.id(), -1, card.clientName(), "정리된 계약 조건", card.text(), 1))
                .toList();
    }
}
