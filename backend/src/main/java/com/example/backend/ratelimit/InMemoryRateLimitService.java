package com.example.backend.ratelimit;

import com.example.backend.entitie.enumuration.TypeRole;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InMemoryRateLimitService implements RateLimitService {

    private final RateLimitPolicy policy;
    private final ConcurrentMap<RateLimitKey, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(Long userId, TypeRole role, EndpointType endpointType) {
        if (userId == null) {
            return true;
        }

        RateLimitDefinition definition = policy.resolve(role, endpointType);
        RateLimitKey key = new RateLimitKey(userId, endpointType, role);

        Bucket bucket = buckets.computeIfAbsent(key, ignored -> Bucket.builder()
                .addLimit(Bandwidth.simple(definition.capacity(), definition.refillPeriod()))
                .build());

        return bucket.tryConsume(1);
    }
}
