package com.example.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:mySecretKeyForJWTTokenGenerationAndValidationPurposeOnly123456789}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long jwtRefreshExpirationMs;

    // Generate JWT Token
    public String generateToken(Authentication authentication) {
        org.springframework.security.core.userdetails.User userPrincipal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        return createToken(userPrincipal.getUsername(), userPrincipal.getAuthorities().iterator().next().getAuthority(), jwtExpirationMs);
    }

    // Generate Refresh Token
    public String generateRefreshToken(String email) {
        return createToken(email, "REFRESH", jwtRefreshExpirationMs);
    }

    // Create JWT Token
    private String createToken(String email, String role, long expirationMs) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // Get Email from Token
    public String getEmailFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            log.error("Error extracting email from token", e);
            return null;
        }
    }

    // Get Role from Token
    public String getRoleFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            return (String) Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("role");
        } catch (JwtException e) {
            log.error("Error extracting role from token", e);
            return null;
        }
    }

    // Create Access Token with specific email and role
    public String createAccessToken(String email, String role) {
        return createToken(email, role, jwtExpirationMs);
    }

    // Validate Token
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed", e);
            return false;
        }
    }
}
