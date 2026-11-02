package com.contractnote.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.genai.errors.ClientException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiContractAiTest {

    private static final List<String> MODELS = List.of("first", "second");

    @Test
    void usesFirstModelWhenAvailable() {
        List<String> tried = new ArrayList<>();
        String result = GeminiContractAi.firstAvailable(MODELS, model -> {
            tried.add(model);
            return model;
        });
        assertEquals("first", result);
        assertEquals(List.of("first"), tried);
    }

    @Test
    void movesToNextModelOnLimit() {
        String result = GeminiContractAi.firstAvailable(MODELS, model -> {
            if (model.equals("first")) {
                throw new ClientException(429, "RESOURCE_EXHAUSTED", "limit");
            }
            return model;
        });
        assertEquals("second", result);
    }

    @Test
    void doesNotTryNextModelOnOtherErrors() {
        List<String> tried = new ArrayList<>();
        assertThrows(ClientException.class, () -> GeminiContractAi.firstAvailable(MODELS, model -> {
            tried.add(model);
            throw new ClientException(400, "INVALID_ARGUMENT", "bad file");
        }));
        assertEquals(List.of("first"), tried);
    }

    @Test
    void throwsLimitErrorWhenAllModelsAreLimited() {
        ClientException e = assertThrows(ClientException.class, () -> GeminiContractAi.firstAvailable(MODELS, model -> {
            throw new ClientException(429, "RESOURCE_EXHAUSTED", model);
        }));
        assertEquals(429, e.code());
    }
}
