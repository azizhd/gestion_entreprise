package com.example.backend.ratelimit;

import com.example.backend.entitie.enumuration.TypeRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitPolicyTest {

    private final RateLimitPolicy policy = new RateLimitPolicy();

    @Test
    void resolvesAdminStandardLimit() {
        RateLimitDefinition definition = policy.resolve(TypeRole.ROLE_ADMIN, EndpointType.STANDARD);

        assertEquals(100, definition.capacity());
    }

    @Test
    void resolvesEmployeeAiLimit() {
        RateLimitDefinition definition = policy.resolve(TypeRole.ROLE_EMPLOYEE, EndpointType.AI_CHAT);

        assertEquals(5, definition.capacity());
    }
}
