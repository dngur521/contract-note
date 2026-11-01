package com.contractnote.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void 빈_글은_묶음이_없다() {
        assertTrue(TextChunker.split("  \n\n ").isEmpty());
    }

    @Test
    void 줄마다_나누고_빈_줄은_버린다() {
        assertEquals(List.of("첫 줄", "둘째 줄"), TextChunker.split("첫 줄\n\n둘째 줄"));
    }

    @Test
    void 줄바꿈이_없어도_문장으로_나눈다() {
        assertEquals(List.of("수정은 2번까지로 합니다.", "괜찮을까요?", "금액 3,000,000원"),
                TextChunker.split("수정은 2번까지로 합니다. 괜찮을까요? 금액 3,000,000원"));
    }

    @Test
    void 나눈_문장마다_날짜를_유지한다() {
        String line = "[2026-10-05] 박담당: 네 좋아요. 잔금은 7일 내에 드릴게요.";
        assertEquals(List.of("[2026-10-05] 박담당: 네 좋아요.", "[2026-10-05] 박담당: 잔금은 7일 내에 드릴게요."), TextChunker.split(line));
        assertEquals("네 좋아요.", TextChunker.withoutHeader("[2026-10-05] 박담당: 네 좋아요."));
    }

    @Test
    void 아주_긴_줄도_내용을_잃지_않는다() {
        String line = "가".repeat(700);
        List<String> chunks = TextChunker.split(line);
        assertEquals(700, chunks.stream().mapToInt(String::length).sum());
        assertTrue(chunks.stream().allMatch(c -> c.length() <= 300));
    }
}
