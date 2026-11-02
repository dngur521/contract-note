package com.contractnote.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeminiContractAi implements ContractAi {

    private static final Logger log = LoggerFactory.getLogger(GeminiContractAi.class);
    private static final int TIMEOUT_MILLIS = 120_000;
    private static final int TOO_MANY_REQUESTS = 429;

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Client client;
    private final List<String> models;

    /** models: 앞에서부터 쓰고, 무료 한도(429)에 걸리면 다음 모델로 넘어간다. */
    public GeminiContractAi(String apiKey, List<String> models) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder().timeout(TIMEOUT_MILLIS).build())
                .build();
        this.models = List.copyOf(models);
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
        Content content = Content.fromParts(parts.toArray(Part[]::new));
        GenerateContentResponse response = firstAvailable(models, model -> client.models.generateContent(model, content, config));
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

    static <R> R firstAvailable(List<String> models, Function<String, R> call) {
        ApiException limited = null;
        for (String model : models) {
            try {
                return call.apply(model);
            } catch (ApiException e) {
                if (e.code() != TOO_MANY_REQUESTS) {
                    throw e;
                }
                log.warn("{} 모델이 한도에 걸려 다음 모델을 시도합니다.", model);
                limited = e;
            }
        }
        if (limited == null) {
            throw new IllegalStateException("Gemini 모델이 설정되지 않았습니다.");
        }
        throw limited;
    }
}
