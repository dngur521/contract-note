package com.contractnote.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.contractnote.contract.PaymentCalculator.BaseDates;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PaymentCalculatorTest {

    private static final LocalDate CONTRACT = LocalDate.of(2026, 10, 5);
    private static final LocalDate DELIVERY = LocalDate.of(2026, 10, 20);
    private static final LocalDate ACCEPTANCE = LocalDate.of(2026, 10, 27);
    private static final BaseDates BASE = new BaseDates(CONTRACT, DELIVERY, ACCEPTANCE);

    @Test
    void 납품_후_7일은_납품일에_7일을_더한다() {
        assertEquals(LocalDate.of(2026, 10, 27),
                PaymentCalculator.dueDate(PaymentRule.DAYS_AFTER_DELIVERY, null, 7, BASE));
    }

    @Test
    void 달이_바뀌어도_올바르게_계산한다() {
        assertEquals(LocalDate.of(2026, 11, 3),
                PaymentCalculator.dueDate(PaymentRule.DAYS_AFTER_DELIVERY, null, 14, BASE));
    }

    @Test
    void 계약_시_지급은_계약일이다() {
        assertEquals(CONTRACT, PaymentCalculator.dueDate(PaymentRule.DAYS_AFTER_CONTRACT, null, 0, BASE));
    }

    @Test
    void 지정_날짜는_그대로_쓴다() {
        LocalDate fixed = LocalDate.of(2026, 12, 1);
        assertEquals(fixed, PaymentCalculator.dueDate(PaymentRule.FIXED_DATE, fixed, null, BASE));
    }

    @Test
    void 납품일이_없으면_기한도_정할_수_없다() {
        assertNull(PaymentCalculator.dueDate(PaymentRule.DAYS_AFTER_DELIVERY, null, 7, new BaseDates(CONTRACT, null, null)));
    }

    @Test
    void 검수_후_N일은_검수일에서_센다() {
        assertEquals(LocalDate.of(2026, 11, 10), PaymentCalculator.dueDate(PaymentRule.DAYS_AFTER_ACCEPTANCE, null, 14, BASE));
    }

    @Test
    void 검수일이_없으면_기한도_정할_수_없다() {
        assertNull(PaymentCalculator.dueDate(PaymentRule.DAYS_AFTER_ACCEPTANCE, null, 14, new BaseDates(CONTRACT, DELIVERY, null)));
    }

    @Test
    void 납품_다음_달_10일() {
        assertEquals(LocalDate.of(2026, 11, 10), PaymentCalculator.dueDate(PaymentRule.NEXT_MONTH_DAY, null, 10, BASE));
    }

    @Test
    void 납품한_달_말일() {
        assertEquals(LocalDate.of(2026, 10, 31), PaymentCalculator.dueDate(PaymentRule.DELIVERY_MONTH_DAY, null, 31, BASE));
    }

    @Test
    void 다음_달_말일은_달_길이에_맞추고_해를_넘긴다() {
        BaseDates december = new BaseDates(null, LocalDate.of(2026, 12, 15), null);
        assertEquals(LocalDate.of(2027, 1, 31), PaymentCalculator.dueDate(PaymentRule.NEXT_MONTH_DAY, null, 31, december));
        BaseDates january = new BaseDates(null, LocalDate.of(2027, 1, 15), null);
        assertEquals(LocalDate.of(2027, 2, 28), PaymentCalculator.dueDate(PaymentRule.NEXT_MONTH_DAY, null, 31, january));
    }

    @Test
    void 지급_합계가_총액과_다르면_알려_준다() {
        assertEquals(2_000_000L, PaymentCalculator.mismatchedSum(2_400_000L, java.util.List.of(600_000L, 1_400_000L)));
        assertNull(PaymentCalculator.mismatchedSum(2_000_000L, java.util.List.of(600_000L, 1_400_000L)));
    }

    @Test
    void 금액을_모르면_합계를_비교하지_않는다() {
        assertNull(PaymentCalculator.mismatchedSum(null, java.util.List.of(600_000L)));
        assertNull(PaymentCalculator.mismatchedSum(2_000_000L, java.util.Arrays.asList(600_000L, null)));
        assertNull(PaymentCalculator.mismatchedSum(2_000_000L, java.util.List.of()));
    }

    @Test
    void 원천징수_후_금액을_계산한다() {
        assertEquals(2_030_700, PaymentCalculator.afterWithholding(2_100_000));
        assertEquals(870_300, PaymentCalculator.afterWithholding(900_000));
    }

    @Test
    void 원_미만은_버린다() {
        // 소득세 1,234 * 3% = 37(37.02), 지방소득세 37 * 10% = 3(3.7)
        assertEquals(1_194, PaymentCalculator.afterWithholding(1_234));
    }
}
