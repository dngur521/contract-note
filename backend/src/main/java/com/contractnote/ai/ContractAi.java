package com.contractnote.ai;

import java.util.List;

/** AI가 맡는 일: 문서 읽기·조건 추출·근거 안에서 답하기. 날짜·금액 계산은 맡기지 않는다. */
public interface ContractAi {
    /** 화면에 표시할 동작 방식. claude 또는 sample */
    String mode();

    ExtractionResult extract(List<UploadedFile> files);

    AnswerResult answer(String question, List<SourceChunk> sources);
}
