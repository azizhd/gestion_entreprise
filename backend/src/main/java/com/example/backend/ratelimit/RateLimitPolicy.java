package com.example.backend.ratelimit;

import com.example.backend.entitie.enumuration.TypeRole;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class RateLimitPolicy {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimitDefinition resolve(TypeRole role, EndpointType endpointType) {
        TypeRole resolvedRole = role != null ? role : TypeRole.ROLE_EMPLOYEE;
        boolean isAi = endpointType == EndpointType.AI_CHAT;

        return switch (resolvedRole) {
            case ROLE_ADMIN -> new RateLimitDefinition(isAi ? 20 : 100, WINDOW);
            case ROLE_SECRETAIRE -> new RateLimitDefinition(isAi ? 10 : 60, WINDOW);
            case ROLE_COMPTABLE -> new RateLimitDefinition(isAi ? 10 : 60, WINDOW);
            case ROLE_EMPLOYEE -> new RateLimitDefinition(isAi ? 5 : 30, WINDOW);
        };
    }
}
