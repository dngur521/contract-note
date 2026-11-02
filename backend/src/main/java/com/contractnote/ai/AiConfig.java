package com.contractnote.ai;

import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ContractAi contractAi(@Value("${app.gemini.api-key}") String geminiKey, @Value("${app.gemini.model}") String geminiModel,
                          @Value("${app.gemini.fallback-model:}") String geminiFallback,
                          @Value("${app.claude.api-key}") String claudeKey, @Value("${app.claude.model}") String claudeModel) {
        if (!geminiKey.isBlank()) {
            return new GeminiContractAi(geminiKey, Stream.of(geminiModel, geminiFallback).filter(m -> !m.isBlank()).toList());
        }
        return claudeKey.isBlank() ? new SampleContractAi() : new ClaudeContractAi(claudeKey, claudeModel);
    }
}
