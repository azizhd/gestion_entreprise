package com.example.backend.ai.controller;

import com.example.backend.ai.dto.AiChatRequest;
import com.example.backend.ai.dto.AiChatResponse;
import com.example.backend.ai.security.AiRequestContext;
import com.example.backend.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiService aiService;
    private final AiRequestContext context;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        String role = context.currentRole().orElseThrow(() -> new AccessDeniedException("No role"));
        Long enterpriseId = context.currentEnterpriseId().orElseThrow(() -> new AccessDeniedException("Enterprise required"));
        AiChatResponse response = aiService.chat(role, enterpriseId, request.message());
        return ResponseEntity.ok(response);
    }
}
