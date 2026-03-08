package com.example.backend.ai.service;

import com.example.backend.ai.dto.AiChatResponse;
import com.example.backend.ai.dto.ToolCall;
import com.example.backend.ai.prompt.AiPromptBuilder;
import com.example.backend.ai.provider.AiProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProvider provider;
    private final ToolExecutionService toolExecutionService;
    private final ObjectMapper objectMapper;
    private final AiPromptBuilder promptBuilder;

    public AiChatResponse chat(String role, Long enterpriseId, String message) {
        Instant start = Instant.now();
        log.info("[AI] incoming message role={} enterprise={} question={} ", role, enterpriseId, message);
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt(role, enterpriseId);
            String modelResponse = provider.chat(systemPrompt, message);
            ToolCall call = parseToolCall(modelResponse);

            if (call != null && call.tool() != null) {
                Object toolResult = toolExecutionService.execute(role, enterpriseId, call);
                String finalAnswer = provider.chat(promptBuilder.buildFollowUpPrompt(role, enterpriseId), buildFollowUp(message, toolResult));
                log.info("[AI] tool executed role={} enterprise={} tool={} durationMs={}", role, enterpriseId, call.tool(), Duration.between(start, Instant.now()).toMillis());
                return new AiChatResponse(finalAnswer, toolResult);
            }
            log.info("[AI] no tool used role={} enterprise={} durationMs={}", role, enterpriseId, Duration.between(start, Instant.now()).toMillis());
            return new AiChatResponse(modelResponse, null);
        } catch (Exception ex) {
            log.error("[AI] failure role={} enterprise={} error={}", role, enterpriseId, ex.getMessage(), ex);
            return new AiChatResponse("Le copilote est momentanément indisponible.", null);
        }
    }

    private ToolCall parseToolCall(String raw) {
        try {
            return objectMapper.readValue(raw, ToolCall.class);
        } catch (Exception e) {
            log.warn("[AI] Unable to parse tool call: {}", e.getMessage());
            return null;
        }
    }

    private String buildFollowUp(String userMessage, Object toolResult) {
        return "User message: " + userMessage + "\n" +
            "tool_result: " + toJson(toolResult) + "\n" +
            "Use only tool_result; no new tools.";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
