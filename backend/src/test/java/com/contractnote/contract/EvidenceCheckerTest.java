package com.contractnote.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EvidenceCheckerTest {

    private static final String TEXT = EvidenceChecker.normalize("네 좋아요. 수정은 2번까지로 하고\n잔금은 납품 후 7일 내에 드릴게요.");

    @Test
    void 띄어쓰기가_달라도_원문에_있으면_통과한다() {
        assertTrue(EvidenceChecker.isIn("수정은 2번까지로  하고 잔금은", TEXT));
    }

    @Test
    void 원문에_없는_설명은_걸러낸다() {
        assertFalse(EvidenceChecker.isIn("수정 횟수에 대한 조항이 없습니다.", TEXT));
    }

    @Test
    void 빈_근거는_인정하지_않는다() {
        assertFalse(EvidenceChecker.isIn("  ", TEXT));
        assertFalse(EvidenceChecker.isIn(null, TEXT));
    }
}
