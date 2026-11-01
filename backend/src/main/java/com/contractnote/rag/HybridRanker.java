package com.contractnote.rag;

import com.contractnote.ai.SourceChunk;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 벡터 검색 결과를 한 번 더 정렬한다.
 * 뜻이 비슷한 문장만 찾으면 "수정 몇 번"에 금액 문장이 먼저 나오는 일이 있어, 낱말이 실제로 들어 있는 문장에 점수를 더한다.
 * 질문에 고객 이름이 있으면 그 고객의 기록만 남긴다.
 */
public final class HybridRanker {

    private static final double KEYWORD_WEIGHT = 0.5;
    /** 기록이 이 정도로 적으면 고르지 않고 전부 AI에 넘긴다. 빠뜨릴 위험이 더 크기 때문이다. */
    private static final int READ_ALL_LIMIT = 200;
    /** 긴 계약서에 짧은 메일이 묻히지 않도록 문서마다 이만큼은 넣는다. */
    private static final int MIN_PER_DOCUMENT = 2;

    private HybridRanker() {}

    public static List<SourceChunk> rank(String question, List<SourceChunk> candidates, int topK) {
        String remaining = question;
        List<String> mentioned = new ArrayList<>();
        for (SourceChunk chunk : candidates) {
            String client = chunk.clientName();
            if (client != null && !client.isBlank() && question.contains(client) && !mentioned.contains(client)) {
                mentioned.add(client);
                remaining = remaining.replace(client, " ");
            }
        }
        List<String> tokens = tokens(remaining);

        List<SourceChunk> ranked = candidates.stream()
                .filter(c -> mentioned.isEmpty() || mentioned.contains(c.clientName()))
                .map(c -> withScore(c, c.score() + KEYWORD_WEIGHT * keywordRatio(tokens, TextChunker.withoutHeader(c.text()))))
                .sorted(Comparator.comparingDouble(SourceChunk::score).reversed())
                .toList();
        if (ranked.size() <= READ_ALL_LIMIT) {
            return ranked;
        }
        Map<Long, Integer> perDocument = new HashMap<>();
        List<SourceChunk> picked = new ArrayList<>();
        for (SourceChunk chunk : ranked) {
            int count = perDocument.merge(chunk.documentId(), 1, Integer::sum);
            if (picked.size() < topK || count <= MIN_PER_DOCUMENT) {
                picked.add(chunk);
            }
        }
        return picked;
    }

    /** 공백으로 나눈 낱말 중 두 글자 이상만 쓴다. */
    static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        for (String word : text.split("[\\s?!.,]+")) {
            if (word.length() >= 2) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    static double keywordRatio(List<String> tokens, String text) {
        if (tokens.isEmpty()) {
            return 0;
        }
        long matched = tokens.stream().filter(token -> containsStem(token, text)).count();
        return (double) matched / tokens.size();
    }

    /** 조사를 떼기 위해 뒤에서 한 글자씩 줄여 가며 비교한다. "잔금은"은 "잔금"으로도 찾는다. */
    private static boolean containsStem(String token, String text) {
        for (int end = token.length(); end >= 2; end--) {
            if (text.contains(token.substring(0, end))) {
                return true;
            }
        }
        return false;
    }

    private static SourceChunk withScore(SourceChunk c, double score) {
        return new SourceChunk(c.id(), c.contractId(), c.documentId(), c.clientName(), c.fileName(), c.text(), score);
    }
}
