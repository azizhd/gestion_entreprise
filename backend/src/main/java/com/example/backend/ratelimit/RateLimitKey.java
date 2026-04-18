package com.example.backend.ratelimit;

import com.example.backend.entitie.enumuration.TypeRole;

public record RateLimitKey(Long userId, EndpointType endpointType, TypeRole role) {
}
