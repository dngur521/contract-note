package com.contractnote.ai;

import java.util.List;
import java.util.Map;

/** Gemini에 넘길 응답 형식. ExtractionResult, AnswerResult와 같은 모양이어야 한다. */
final class GeminiSchemas {

    private GeminiSchemas() {}

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static Map<String, Object> bool(String description) {
        return Map.of("type", "boolean", "description", description);
    }

    private static Map<String, Object> array(Map<String, Object> items, String description) {
        return Map.of("type", "array", "items", items, "description", description);
    }

    private static Map<String, Object> object(Map<String, Object> properties) {
        return Map.of("type", "object", "properties", properties, "required", List.copyOf(properties.keySet()));
    }

    private static final Map<String, Object> EVIDENCE = object(Map.of(
            "field", string("clientName, projectName, contractDate, deliveryDate, totalAmount, revisionLimit, copyrightOwner, withholding 중 하나"),
            "documentNumber", integer("문서 번호(1부터)"),
            "quote", string("원문 문장을 그대로")));

    private static final Map<String, Object> PAYMENT = object(Map.of(
            "label", string("계약금, 잔금 등"),
            "amount", integer("금액(원). 없으면 0"),
            "ruleType", Map.of("type", "string",
                    "enum", List.of("FIXED_DATE", "DAYS_AFTER_DELIVERY", "DAYS_AFTER_CONTRACT", "DAYS_AFTER_ACCEPTANCE",
                            "DELIVERY_MONTH_DAY", "NEXT_MONTH_DAY", "UNKNOWN"),
                    "description", "FIXED_DATE: 날짜 지정, DAYS_AFTER_DELIVERY: 납품 후 N일, DAYS_AFTER_CONTRACT: 계약 후 N일(계약 시 지급은 0일), DAYS_AFTER_ACCEPTANCE: 검수 후 N일, DELIVERY_MONTH_DAY: 납품한 달의 N일, NEXT_MONTH_DAY: 납품한 달 다음 달의 N일, UNKNOWN: 기록 없음"),
            "fixedDate", string("FIXED_DATE일 때 YYYY-MM-DD, 아니면 빈 문자열"),
            "offsetDays", integer("DAYS_AFTER_*일 때 일수, *_DAY일 때 그 달의 날짜(말일은 31), 아니면 0. 날짜를 직접 계산하지 말 것"),
            "documentNumber", integer("문서 번호(1부터)"),
            "quote", string("원문 문장을 그대로")));

    private static final Map<String, Object> TRANSCRIPT = object(Map.of(
            "documentNumber", integer("문서 번호(1부터)"),
            "text", string("문서의 글자 전체를 바꾸지 말고 그대로 옮긴다. 문장이나 항목마다 줄바꿈(\\n)으로 나눈다. 카카오톡과 메일은 메시지의 모든 줄 앞에 '[YYYY-MM-DD] 보낸사람: ' 을 붙인다")));

    static final Map<String, Object> EXTRACTION = object(Map.ofEntries(
            Map.entry("clientName", string("고객(의뢰인) 이름. 없으면 빈 문자열")),
            Map.entry("projectName", string("프로젝트 이름. 없으면 빈 문자열")),
            Map.entry("contractDate", string("계약일 YYYY-MM-DD. 계약서 날짜를 쓰고, 없으면 양측이 조건에 최종 합의한 메시지의 날짜를 쓴다. 둘 다 없으면 빈 문자열")),
            Map.entry("deliveryDate", string("최종 납품일 YYYY-MM-DD. 기록에 명시되지 않았으면 빈 문자열")),
            Map.entry("acceptanceDate", string("검수일 YYYY-MM-DD. 기록에 명시되지 않았으면 빈 문자열")),
            Map.entry("totalAmount", integer("총 계약 금액(원). 없으면 0")),
            Map.entry("revisionLimit", integer("합의된 수정 횟수. 없으면 -1")),
            Map.entry("copyrightOwner", string("저작권 귀속 내용. 없으면 빈 문자열")),
            Map.entry("withholding", bool("이 계약의 보수에서 3.3%(사업소득세)를 뗀다고 정해져 있으면 true. 양식의 일반 조항처럼 \"~인 경우 공제한다\"는 조건부 문장만 있으면 false")),
            Map.entry("evidences", array(EVIDENCE, "위 각 항목의 근거. 항목별로 최종 조건을 정한 원문 문장을 하나씩. 기록에 값이 없는 항목은 넣지 않는다")),
            Map.entry("payments", array(PAYMENT, "계약금, 중도금, 잔금 등 지급 조건 목록")),
            Map.entry("conflicts", array(Map.of("type", "string"), "기록끼리 또는 한 기록 안에서 앞뒤로 조건이 바뀐 부분을 사용자에게 확인 요청하는 문장 목록")),
            Map.entry("transcripts", array(TRANSCRIPT, "문서마다 읽은 글자 전체"))));

    static final Map<String, Object> ANSWER = object(Map.of(
            "found", bool("제공된 근거 안에서 답을 찾았으면 true"),
            "answer", string("한국어 답변. 찾지 못했으면 빈 문자열"),
            "usedSources", array(Map.of("type", "integer"), "답에 실제로 사용한 근거 번호들")));
}
