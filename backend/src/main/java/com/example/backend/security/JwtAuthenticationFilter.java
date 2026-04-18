package com.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            log.debug("[JWT] Incoming request: {} {}", request.getMethod(), request.getRequestURI());
            String jwt = getJwtFromRequest(request);
            boolean hasAuthHeader = StringUtils.hasText(request.getHeader("Authorization"));
            log.debug("[JWT] Authorization header present: {}", hasAuthHeader);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                log.debug("[JWT] Token validated successfully");
                String email = tokenProvider.getEmailFromToken(jwt);
                String roleClaim = tokenProvider.getRoleFromToken(jwt);
                log.debug("[JWT] Extracted email: {}, role: {}", email, roleClaim);

                var userDetails = customUserDetailsService.loadUserByUsername(email);
                log.debug("[JWT] Loaded user: {} with {} authorities", userDetails.getUsername(), userDetails.getAuthorities().size());
                List<GrantedAuthority> authorities = resolveAuthorities(roleClaim, userDetails.getAuthorities());
                log.debug("[JWT] Resolved authorities: {}", authorities);

                var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                log.debug("[JWT] Authentication object: {}", authentication);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("[JWT] SecurityContext populated for {}", email);
            } else {
                log.debug("[JWT] Token missing or failed validation");
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private List<GrantedAuthority> resolveAuthorities(String roleClaim, Collection<? extends GrantedAuthority> userAuthorities) {
        if (StringUtils.hasText(roleClaim)) {
            return Collections.singletonList(new SimpleGrantedAuthority(roleClaim));
        }
        return List.copyOf(userAuthorities);
    }
}
