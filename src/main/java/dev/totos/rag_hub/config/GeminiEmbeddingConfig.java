package dev.totos.rag_hub.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class GeminiEmbeddingConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new GeminiEmbeddingModel(apiKey);
    }

    public static class GeminiEmbeddingModel extends AbstractEmbeddingModel {

        private static final String MODEL = "gemini-embedding-001";
        // gemini-embedding-001 defaults to 3072 dims; set this to whatever
        // your vector store column expects. 768 keeps parity with the old
        // text-embedding-004 dimension if you don't want to migrate the store.
        private static final int OUTPUT_DIMENSIONALITY = 768;

        private final String apiKey;
        private final RestClient restClient;

        public GeminiEmbeddingModel(String apiKey) {
            this.apiKey = apiKey;
            this.restClient = RestClient.create();
        }

        @Override
        public float[] embed(Document document) {
            return this.embed(document.getFormattedContent());
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            List<String> instructions = request.getInstructions();

            for (int i = 0; i < instructions.size(); i++) {
                String text = instructions.get(i);

                Map<String, Object> body = Map.of(
                        "model", "models/" + MODEL,
                        "content", Map.of("parts", List.of(Map.of("text", text))),
                        "outputDimensionality", OUTPUT_DIMENSIONALITY
                );

                Map<?, ?> response = restClient.post()
                        .uri("https://generativelanguage.googleapis.com/v1beta/models/"
                                + MODEL + ":embedContent?key=" + apiKey)
                        .header("Content-Type", "application/json")
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                if (response != null && response.containsKey("embedding")) {
                    Map<?, ?> embeddingMap = (Map<?, ?>) response.get("embedding");
                    List<Double> values = (List<Double>) embeddingMap.get("values");
                    float[] floatValues = new float[values.size()];
                    for (int j = 0; j < values.size(); j++) {
                        floatValues[j] = values.get(j).floatValue();
                    }
                    embeddings.add(new Embedding(floatValues, i));
                }
            }

            return new EmbeddingResponse(embeddings);
        }
    }
}