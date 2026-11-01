package com.contractnote.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.TextBlockParam;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class ClaudeContractAi implements ContractAi {

    private final AnthropicClient client;
    private final String model;

    public ClaudeContractAi(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public String mode() {
        return "claude";
    }

    @Override
    public ExtractionResult extract(List<UploadedFile> files) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            UploadedFile file = files.get(i);
            blocks.add(text("문서 " + (i + 1) + ": " + file.fileName()));
            blocks.add(toBlock(file));
        }
        blocks.add(text(AiPrompts.EXTRACT_REQUEST));

        var params = baseParams(AiPrompts.EXTRACT_SYSTEM)
                .addUserMessageOfContentBlockParams(blocks)
                .outputConfig(ExtractionResult.class)
                .build();
        return firstResult(client.messages().create(params));
    }

    @Override
    public AnswerResult answer(String question, List<SourceChunk> sources) {
        var params = baseParams(AiPrompts.ANSWER_SYSTEM)
                .addUserMessage(AiPrompts.answerRequest(question, sources))
                .outputConfig(AnswerResult.class)
                .build();
        return firstResult(client.messages().create(params));
    }

    private MessageCreateParams.Builder baseParams(String system) {
        return MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                .system(system)
                // 안전 분류기가 거절하면 다른 모델로 넘겨 다시 시도한다.
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
    }

    private static <T> T firstResult(StructuredMessage<T> response) {
        response.stopReason().ifPresent(reason -> {
            if (StopReason.REFUSAL.equals(reason)) {
                throw new AiFailedException("AI가 이 요청을 처리하지 않았습니다.");
            }
            if (StopReason.MAX_TOKENS.equals(reason)) {
                throw new AiFailedException("기록이 너무 길어 AI 응답이 잘렸습니다. 파일을 나눠서 올려 주세요.");
            }
        });
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(block -> block.text())
                .findFirst()
                .orElseThrow(() -> new AiFailedException("AI 응답이 비어 있습니다."));
    }

    private static ContentBlockParam toBlock(UploadedFile file) {
        String base64 = Base64.getEncoder().encodeToString(file.bytes());
        return switch (file.mediaType()) {
            case "application/pdf" -> ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                    .source(Base64PdfSource.builder().data(base64).build())
                    .title(file.fileName())
                    .build());
            case "image/png", "image/jpeg" -> ContentBlockParam.ofImage(ImageBlockParam.builder()
                    .source(Base64ImageSource.builder()
                            .data(base64)
                            .mediaType(file.mediaType().equals("image/png")
                                    ? Base64ImageSource.MediaType.IMAGE_PNG
                                    : Base64ImageSource.MediaType.IMAGE_JPEG)
                            .build())
                    .build());
            default -> text(new String(file.bytes(), StandardCharsets.UTF_8));
        };
    }

    private static ContentBlockParam text(String value) {
        return ContentBlockParam.ofText(TextBlockParam.builder().text(value).build());
    }
}
