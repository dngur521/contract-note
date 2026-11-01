package com.contractnote.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 글을 줄과 문장 단위로 나눈다. 답변 근거로 정확한 문장을 보여 주기 위해서다.
 * 줄 앞에 붙은 "[날짜] 보낸사람: "은 어느 합의가 나중인지 알 수 있도록 나눈 문장마다 유지한다.
 */
public final class TextChunker {

    private static final int MAX_CHARS = 300;
    private static final Pattern HEADER = Pattern.compile("^\\[\\d{4}-\\d{2}-\\d{2}\\][^:]{0,40}:\\s*");

    private TextChunker() {}

    public static List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            Matcher matcher = HEADER.matcher(line);
            String header = matcher.find() ? matcher.group() : "";
            for (String part : line.substring(header.length()).split("(?<=[.?!])\\s+")) {
                String sentence = part.strip();
                while (sentence.length() > MAX_CHARS) {
                    chunks.add(header + sentence.substring(0, MAX_CHARS));
                    sentence = sentence.substring(MAX_CHARS);
                }
                if (!sentence.isEmpty()) {
                    chunks.add(header + sentence);
                }
            }
        }
        return chunks;
    }

    /** 뜻 비교에는 날짜와 보낸사람을 빼고 쓴다. */
    public static String withoutHeader(String chunk) {
        return HEADER.matcher(chunk).replaceFirst("");
    }
}
