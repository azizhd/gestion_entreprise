package com.example.backend.ai.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
public class OpenAiProvider implements AiProvider {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.openai.url:https://api.openai.com/v1/chat/completions}")
    private String url;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of(
            "model", model,
            "temperature", 0.2,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            )
        );
        var req = new HttpEntity<>(body, headers);
        log.debug("[AI][OPENAI] sending prompt to {}", url);
        Map<?, ?> response = restTemplate.postForObject(url, req, Map.class);
        if (response == null) {
            return "";
        }
        var choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        var message = (Map<String, Object>) choices.get(0).get("message");
        return message != null ? String.valueOf(message.get("content")) : "";
    }
}
