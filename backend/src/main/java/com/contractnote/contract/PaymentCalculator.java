package com.contractnote.contract;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/** 날짜·금액 계산. 틀리면 안 되는 숫자이므로 AI에 맡기지 않고 코드로 계산한다. */
public final class PaymentCalculator {

    private PaymentCalculator() {}

    /** 계약의 기준 날짜들. */
    public record BaseDates(LocalDate contract, LocalDate delivery, LocalDate acceptance) {}

    /** 지급 기한. 기준 날짜가 아직 없으면 null. */
    public static LocalDate dueDate(PaymentRule rule, LocalDate fixedDate, Integer offsetDays, BaseDates base) {
        int days = offsetDays == null ? 0 : offsetDays;
        return switch (rule) {
            case FIXED_DATE -> fixedDate;
            case DAYS_AFTER_DELIVERY -> base.delivery() == null ? null : base.delivery().plusDays(days);
            case DAYS_AFTER_CONTRACT -> base.contract() == null ? null : base.contract().plusDays(days);
            case DAYS_AFTER_ACCEPTANCE -> base.acceptance() == null ? null : base.acceptance().plusDays(days);
            case DELIVERY_MONTH_DAY -> base.delivery() == null ? null : dayOfMonth(YearMonth.from(base.delivery()), days);
            case NEXT_MONTH_DAY -> base.delivery() == null ? null : dayOfMonth(YearMonth.from(base.delivery()).plusMonths(1), days);
            case UNKNOWN -> null;
        };
    }

    /** 그 달에 없는 날(예: 2월 31일)은 말일로 맞춘다. */
    private static LocalDate dayOfMonth(YearMonth month, int day) {
        return month.atDay(Math.clamp(day, 1, month.lengthOfMonth()));
    }

    /** 지급 항목 합계가 총 금액과 다르면 그 합계를, 같거나 비교할 수 없으면 null을 돌려준다. */
    public static Long mismatchedSum(Long total, List<Long> amounts) {
        if (total == null || amounts.isEmpty() || amounts.stream().anyMatch(Objects::isNull)) {
            return null;
        }
        long sum = amounts.stream().mapToLong(Long::longValue).sum();
        return sum == total ? null : sum;
    }

    /** 3.3% 원천징수(소득세 3% + 지방소득세는 소득세의 10%, 원 미만 절사) 후 실수령액. */
    public static long afterWithholding(long amount) {
        long incomeTax = amount * 3 / 100;
        long localTax = incomeTax / 10;
        return amount - incomeTax - localTax;
    }
}
