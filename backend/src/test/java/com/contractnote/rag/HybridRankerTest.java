package com.contractnote.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.contractnote.ai.SourceChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class HybridRankerTest {

    @Test
    void 기록이_적으면_전부_넘긴다() {
        List<SourceChunk> chunks = new java.util.ArrayList<>();
        for (long i = 0; i < 12; i++) {
            chunks.add(chunk(i, "A사", "문장 " + i, 0.5));
        }
        assertEquals(12, HybridRanker.rank("점심", chunks, 3).size());
    }

    @Test
    void 긴_기록에서도_문서마다_최소_문장을_넣는다() {
        List<SourceChunk> chunks = new java.util.ArrayList<>();
        for (long i = 0; i < 300; i++) {
            chunks.add(new SourceChunk(i, 1, 1, "A사", "계약서", "조항 " + i, 0.9));
        }
        SourceChunk mail = new SourceChunk(100, 1, 2, "A사", "메일", "짧은 메일", 0.1);
        chunks.add(mail);
        List<SourceChunk> ranked = HybridRanker.rank("수수료", chunks, 5);
        assertTrue(ranked.contains(new SourceChunk(100, 1, 2, "A사", "메일", "짧은 메일", 0.1)));
        assertEquals(6, ranked.size());
    }

    private static SourceChunk chunk(long id, String client, String text, double score) {
        return new SourceChunk(id, 1, 1, client, "기록", text, score);
    }

    @Test
    void 낱말이_들어_있는_문장을_앞으로_올린다() {
        var money = chunk(1, "A사", "총 금액 3,000,000원", 0.55);
        var revision = chunk(2, "A사", "수정은 2번까지로 하고", 0.35);
        List<SourceChunk> ranked = HybridRanker.rank("수정 몇 번까지야?", List.of(money, revision), 2);
        assertEquals(2, ranked.get(0).id());
    }

    @Test
    void 조사가_붙어도_찾는다() {
        assertEquals(1.0, HybridRanker.keywordRatio(List.of("잔금은"), "잔금 2,100,000원"));
    }

    @Test
    void 질문에_고객_이름이_있으면_그_고객만_남긴다() {
        var a = chunk(1, "A사", "수정 3회", 0.3);
        var b = chunk(2, "B사", "수정 5회", 0.9);
        List<SourceChunk> ranked = HybridRanker.rank("A사 수정 몇 번이야?", List.of(a, b), 5);
        assertEquals(List.of(1L), ranked.stream().map(SourceChunk::id).toList());
    }

    @Test
    void 고객_이름은_낱말_점수에_넣지_않는다() {
        var nameOnly = chunk(1, "A사", "A사와 미팅", 0.2);
        var revision = chunk(2, "A사", "수정 3회", 0.2);
        assertEquals(2, HybridRanker.rank("A사 수정", List.of(nameOnly, revision), 1).get(0).id());
    }

    @Test
    void 관련_낱말이_없으면_벡터_점수_순서를_지킨다() {
        var first = chunk(1, "A사", "가나다", 0.4);
        var second = chunk(2, "A사", "라마바", 0.3);
        List<SourceChunk> ranked = HybridRanker.rank("점심 메뉴", List.of(second, first), 2);
        assertEquals(1, ranked.get(0).id());
        assertTrue(ranked.get(0).score() < 0.5);
    }
}
