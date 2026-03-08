package com.example.backend.ai.dto;

import com.example.backend.ai.tools.AiTool;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record ToolCall(
	@JsonProperty("tool") AiTool tool,
	@JsonProperty("args") @JsonAlias("arguments") Map<String, Object> args
) {
}
