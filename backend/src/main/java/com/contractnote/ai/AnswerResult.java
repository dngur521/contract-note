package com.contractnote.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

/** 질문 답변 결과. */
public record AnswerResult(
        @JsonPropertyDescription("제공된 근거 안에서 답을 찾았으면 true") boolean found,
        @JsonPropertyDescription("한국어 답변. 찾지 못했으면 빈 문자열") String answer,
        @JsonPropertyDescription("답에 실제로 사용한 근거 번호들") List<Integer> usedSources) {}
