package com.contractnote.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/** AI가 계약 기록에서 뽑아낸 결과. 이 형식 그대로 AI 응답의 JSON 스키마가 된다. */
public record ExtractionResult(
        @JsonPropertyDescription("고객(의뢰인) 이름. 없으면 빈 문자열") String clientName,
        @JsonPropertyDescription("프로젝트 이름. 없으면 빈 문자열") String projectName,
        @JsonPropertyDescription("계약일 YYYY-MM-DD. 계약서 날짜를 쓰고, 없으면 양측이 조건에 최종 합의한 메시지의 날짜를 쓴다. 둘 다 없으면 빈 문자열") String contractDate,
        @JsonPropertyDescription("최종 납품일 YYYY-MM-DD. 기록에 명시되지 않았으면 빈 문자열") String deliveryDate,
        @JsonPropertyDescription("검수일 YYYY-MM-DD. 기록에 명시되지 않았으면 빈 문자열") String acceptanceDate,
        @JsonPropertyDescription("총 계약 금액(원). 없으면 0") long totalAmount,
        @JsonPropertyDescription("합의된 수정 횟수. 없으면 -1") int revisionLimit,
        @JsonPropertyDescription("저작권 귀속 내용. 없으면 빈 문자열") String copyrightOwner,
        @JsonPropertyDescription("이 계약의 보수에서 3.3%(사업소득세)를 뗀다고 정해져 있으면 true. 양식의 일반 조항처럼 \"~인 경우 공제한다\"는 조건부 문장만 있으면 false") boolean withholding,
        @JsonPropertyDescription("위 각 항목의 근거. 항목별로 최종 조건을 정한 원문 문장을 하나씩. 기록에 값이 없는 항목은 넣지 않는다") List<FieldEvidence> evidences,
        @JsonPropertyDescription("계약금, 중도금, 잔금 등 지급 조건 목록") List<PaymentTerm> payments,
        @JsonPropertyDescription("기록끼리 또는 한 기록 안에서 앞뒤로 조건이 바뀐 부분을 사용자에게 확인 요청하는 문장 목록") List<String> conflicts,
        @JsonPropertyDescription("문서마다 읽은 글자 전체") List<Transcript> transcripts) {

    public record FieldEvidence(
            @JsonPropertyDescription("clientName, projectName, contractDate, deliveryDate, totalAmount, revisionLimit, copyrightOwner, withholding 중 하나")
            String field,
            @JsonPropertyDescription("문서 번호(1부터)") int documentNumber,
            @JsonPropertyDescription("원문 문장을 그대로") String quote) {}

    public record PaymentTerm(
            @JsonPropertyDescription("계약금, 잔금 등") String label,
            @JsonPropertyDescription("금액(원). 없으면 0") long amount,
            @JsonPropertyDescription("FIXED_DATE(날짜 지정), DAYS_AFTER_DELIVERY(납품 후 N일), DAYS_AFTER_CONTRACT(계약 후 N일), DAYS_AFTER_ACCEPTANCE(검수 후 N일), DELIVERY_MONTH_DAY(납품한 달의 N일), NEXT_MONTH_DAY(납품한 달 다음 달의 N일), UNKNOWN(기록 없음) 중 하나")
            String ruleType,
            @JsonPropertyDescription("FIXED_DATE일 때 YYYY-MM-DD, 아니면 빈 문자열") String fixedDate,
            @JsonPropertyDescription("DAYS_AFTER_*일 때 일수, *_DAY일 때 그 달의 날짜(말일은 31), 아니면 0. 날짜를 직접 계산하지 말 것") int offsetDays,
            @JsonPropertyDescription("문서 번호(1부터)") int documentNumber,
            @JsonPropertyDescription("원문 문장을 그대로") String quote) {}

    public record Transcript(
            @JsonPropertyDescription("문서 번호(1부터)") int documentNumber,
            @JsonPropertyDescription("문서의 글자 전체를 바꾸지 말고 그대로 옮긴다. 문장이나 항목마다 줄바꿈(\\n)으로 나눈다. 카카오톡과 메일은 메시지의 모든 줄 앞에 '[YYYY-MM-DD] 보낸사람: ' 을 붙인다") String text) {}
}
