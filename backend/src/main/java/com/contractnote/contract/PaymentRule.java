package com.contractnote.contract;

public enum PaymentRule {
    /** 날짜 지정 */
    FIXED_DATE,
    /** 납품 후 N일 */
    DAYS_AFTER_DELIVERY,
    /** 계약 후 N일 */
    DAYS_AFTER_CONTRACT,
    /** 검수 후 N일 */
    DAYS_AFTER_ACCEPTANCE,
    /** 납품한 달의 N일. 말일은 31로 적는다 */
    DELIVERY_MONTH_DAY,
    /** 납품한 달 다음 달의 N일. 말일은 31로 적는다 */
    NEXT_MONTH_DAY,
    /** 기록 없음 */
    UNKNOWN;

    public static PaymentRule parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return UNKNOWN;
        }
    }
}
