package com.example.backend.ratelimit;

import com.example.backend.entitie.enumuration.TypeRole;

public interface RateLimitService {

    boolean tryConsume(Long userId, TypeRole role, EndpointType endpointType);
}
