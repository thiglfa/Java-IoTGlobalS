package com.wellwork.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Duration timeout;

    public AIService(@Value("${groq.api.key}") String apiKey,
                     @Value("${groq.base-url:https://api.groq.com}") String baseUrl,
                     @Value("${groq.model:llama-3.1-8b-instant}") String model,
                     @Value("${groq.timeout-seconds:30}") long timeoutSeconds,
                     ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }


    public Result generateMessage(String prompt) {
        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "max_tokens", 200,
                    "temperature", 0.2
            );

            Mono<String> respMono = webClient.post()
                    .uri("/openai/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(
                                            new RuntimeException("Erro da Groq: " + errorBody)
                                    ))
                    )
                    .bodyToMono(String.class)
                    .timeout(timeout);

            String respBody = respMono.block(timeout);

            JsonNode root = objectMapper.readTree(respBody);

            String messageText = "";
            Double confidence = null;

            if (root.has("choices") && root.get("choices").isArray()) {
                JsonNode first = root.get("choices").get(0);

                if (first.has("message") && first.get("message").has("content")) {
                    messageText = first.get("message").get("content").asText();
                }

                if (first.has("confidence")) {
                    try {
                        confidence = first.get("confidence").asDouble();
                    } catch (Exception ignored) {}
                }
            }

            // ❌ Removido o acréscimo da confiança no texto
            return new Result(messageText.trim(), Optional.ofNullable(confidence));

        } catch (Exception ex) {
            ex.printStackTrace();
            return new Result("", Optional.empty());
        }
    }



    public static record Result(String message, Optional<Double> confidence) { }
}
