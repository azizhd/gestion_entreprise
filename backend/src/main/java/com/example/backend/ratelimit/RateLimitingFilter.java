package com.example.backend.ratelimit;

import com.example.backend.security.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String ERROR_RESPONSE = "{\"error\":\"Too many requests. Please try again later.\"}";

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserPrincipal userPrincipal)) {
            filterChain.doFilter(request, response);
            return;
        }

        EndpointType endpointType = resolveEndpointType(request.getRequestURI());
        boolean allowed = rateLimitService.tryConsume(userPrincipal.getId(), userPrincipal.getRole(), endpointType);
        if (!allowed) {
            log.warn("User {} with role {} exceeded rate limit on {}", userPrincipal.getId(), userPrincipal.getRole(), request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(ERROR_RESPONSE);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private EndpointType resolveEndpointType(String path) {
        if (path == null) {
            return EndpointType.STANDARD;
        }
        if (path.startsWith("/api/chat") || path.startsWith("/api/ai/chat")) {
            return EndpointType.AI_CHAT;
        }
        return EndpointType.STANDARD;
    }
}
