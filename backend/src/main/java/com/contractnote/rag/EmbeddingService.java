package com.contractnote.rag;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 문장을 384차원 벡터로 바꾼다. 한국어를 지원하는 공개 모델을 이 컴퓨터에서 직접 돌리므로 API 비용이 없다.
 * 모델 파일은 처음 쓸 때 한 번 내려받는다.
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final String BASE = "https://huggingface.co/Xenova/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/";
    private static final Path DIR = Path.of(System.getProperty("user.home"), ".cache", "contract-note", "embedding");

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    public float[] embed(String text) {
        try {
            ensureLoaded();
            Encoding encoding = tokenizer.encode(text);
            long[] mask = encoding.getAttentionMask();
            long[][] shape = {encoding.getIds()};
            long[][] maskShape = {mask};
            long[][] typeShape = {encoding.getTypeIds()};

            Map<String, OnnxTensor> inputs = new HashMap<>();
            try (OnnxTensor ids = OnnxTensor.createTensor(env, shape);
                 OnnxTensor attention = OnnxTensor.createTensor(env, maskShape);
                 OnnxTensor types = OnnxTensor.createTensor(env, typeShape)) {
                inputs.put("input_ids", ids);
                inputs.put("attention_mask", attention);
                if (session.getInputNames().contains("token_type_ids")) {
                    inputs.put("token_type_ids", types);
                }
                try (OrtSession.Result result = session.run(inputs)) {
                    float[][] tokens = ((float[][][]) result.get(0).getValue())[0];
                    return meanPoolAndNormalize(tokens, mask);
                }
            }
        } catch (IOException | OrtException e) {
            throw new IllegalStateException("임베딩 계산에 실패했습니다.", e);
        }
    }

    static float[] meanPoolAndNormalize(float[][] tokens, long[] mask) {
        float[] sum = new float[tokens[0].length];
        int count = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (mask[i] == 0) {
                continue;
            }
            count++;
            for (int d = 0; d < sum.length; d++) {
                sum[d] += tokens[i][d];
            }
        }
        double norm = 0;
        for (int d = 0; d < sum.length; d++) {
            sum[d] /= Math.max(count, 1);
            norm += sum[d] * sum[d];
        }
        norm = Math.sqrt(norm);
        for (int d = 0; d < sum.length; d++) {
            sum[d] /= (float) Math.max(norm, 1e-9);
        }
        return sum;
    }

    private synchronized void ensureLoaded() throws IOException, OrtException {
        if (session != null) {
            return;
        }
        Path model = download("onnx/model_quantized.onnx", "model.onnx");
        Path tokenizerFile = download("tokenizer.json", "tokenizer.json");
        env = OrtEnvironment.getEnvironment();
        session = env.createSession(model.toString(), new OrtSession.SessionOptions());
        tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile, Map.of("maxLength", "256", "truncation", "true"));
    }

    private static Path download(String remote, String local) throws IOException {
        Path target = DIR.resolve(local);
        if (Files.exists(target)) {
            return target;
        }
        Files.createDirectories(DIR);
        log.info("임베딩 모델 파일을 내려받는 중: {}", remote);
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + remote)).build();
        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("모델 다운로드 실패: HTTP " + response.statusCode());
            }
            Path temp = Files.createTempFile(DIR, local, ".part");
            try (InputStream in = response.body()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("모델 다운로드가 중단되었습니다.", e);
        }
    }
}
