package com.contractnote.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeminiContractAi implements ContractAi {

    private static final int TIMEOUT_MILLIS = 120_000;

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Client client;
    private final String model;

    public GeminiContractAi(String apiKey, String model) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder().timeout(TIMEOUT_MILLIS).build())
                .build();
        this.model = model;
    }

    @Override
    public String mode() {
        return "gemini";
    }

    @Override
    public ExtractionResult extract(List<UploadedFile> files) {
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            UploadedFile file = files.get(i);
            parts.add(Part.fromText("문서 " + (i + 1) + ": " + file.fileName()));
            parts.add(file.mediaType().startsWith("text/")
                    ? Part.fromText(new String(file.bytes(), StandardCharsets.UTF_8))
                    : Part.fromBytes(file.bytes(), file.mediaType()));
        }
        parts.add(Part.fromText(AiPrompts.EXTRACT_REQUEST));
        return call(AiPrompts.EXTRACT_SYSTEM, parts, GeminiSchemas.EXTRACTION, ExtractionResult.class);
    }

    @Override
    public AnswerResult answer(String question, List<SourceChunk> sources) {
        return call(AiPrompts.ANSWER_SYSTEM, List.of(Part.fromText(AiPrompts.answerRequest(question, sources))),
                GeminiSchemas.ANSWER, AnswerResult.class);
    }

    private <T> T call(String system, List<Part> parts, Map<String, Object> schema, Class<T> type) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(system)))
                .responseMimeType("application/json")
                .responseJsonSchema(schema)
                .maxOutputTokens(32000)
                .build();
        GenerateContentResponse response = client.models.generateContent(model, Content.fromParts(parts.toArray(Part[]::new)), config);
        String text = response.text();
        if (text == null || text.isBlank()) {
            throw new AiFailedException("AI가 응답을 만들지 못했습니다. 다시 시도해 주세요.");
        }
        try {
            return JSON.readValue(text, type);
        } catch (JsonProcessingException e) {
            throw new AiFailedException("AI 응답을 읽지 못했습니다. 기록이 길면 나눠서 올려 주세요.");
        }
    }
}
