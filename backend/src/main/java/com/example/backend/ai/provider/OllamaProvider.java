package com.example.backend.ai.provider;

import com.example.backend.ai.provider.dto.OllamaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaProvider implements AiProvider {

    @Value("${ai.ollama.host:http://localhost:11434}")
    private String host;

    @Value("${ai.ollama.model:llama3.2}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        var body = Map.of(
            "model", model,
            "system", systemPrompt,
            "prompt", userPrompt,
            "stream", false
        );
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var req = new HttpEntity<>(body, headers);
        log.debug("[AI][OLLAMA] sending prompt to {} with model {}", host, model);
        String raw = restTemplate.postForObject(host + "/api/generate", req, String.class);
        try {
            var parsed = objectMapper.readValue(raw, OllamaResponse.class);
            return parsed != null ? parsed.response() : "";
        } catch (Exception ex) {
            log.error("[AI][OLLAMA] failed to parse response: {}", ex.getMessage(), ex);
            return "";
        }
    }
}
