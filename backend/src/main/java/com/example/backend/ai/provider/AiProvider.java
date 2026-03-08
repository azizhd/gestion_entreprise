package com.example.backend.ai.provider;

public interface AiProvider {
    String chat(String systemPrompt, String userPrompt);
}
