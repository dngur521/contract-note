package com.contractnote.contract;

/** AI가 낸 근거 문장이 실제 원문에 있는지 확인한다. 띄어쓰기와 문장 부호 차이는 무시한다. */
public final class EvidenceChecker {

    private EvidenceChecker() {}

    public static String normalize(String text) {
        return text == null ? "" : text.replaceAll("[\\s\\p{Punct}·“”‘’]+", "");
    }

    public static boolean isIn(String quote, String normalizedText) {
        String q = normalize(quote);
        return !q.isEmpty() && normalizedText.contains(q);
    }
}
