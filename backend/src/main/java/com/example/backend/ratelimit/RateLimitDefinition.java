package com.example.backend.ratelimit;

import java.time.Duration;

public record RateLimitDefinition(int capacity, Duration refillPeriod) {
}
