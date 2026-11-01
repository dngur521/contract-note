package com.contractnote.ai;

/** 검색으로 찾은 문장 묶음. */
public record SourceChunk(long id, long contractId, long documentId, String clientName, String fileName, String text, double score) {}
