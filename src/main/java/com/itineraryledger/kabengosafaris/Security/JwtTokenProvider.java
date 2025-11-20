package com.itineraryledger.kabengosafaris.Security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.Authentication;

import lombok.extern.slf4j.Slf4j;

/**
 * JWT Token Provider Component
 * Generates, validates, and extracts information from JWT tokens.
 * Token expiration time is now configurable via database settings.
 *
 * Configurable Settings:
 * - jwt.expiration.time.minutes: JWT token expiration time in minutes
 * - jwt.refresh.expiration.time.minutes: Refresh token expiration time
 * - jwt.issuer: JWT token issuer claim
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private String JWT_SECRET_KEY = "";
    private final SecuritySettingsService securitySettingsService;
    private long jwtExpirationTimeMillis;
    private long jwtRefreshExpirationTimeMillis;

    // Fallback value from application.properties
    @Value("${security.jwt.expiration.time.minutes:180}")
    private long defaultJwtExpirationMinutes;

    @Value("${security.jwt.refresh.expiration.time.minutes:1440}")
    private long defaultJwtRefreshExpirationMinutes;

    public JwtTokenProvider(SecuritySettingsService securitySettingsService) {
        this.securitySettingsService = securitySettingsService;

        try {
            // Generate or load JWT secret key
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey secKey = keyGenerator.generateKey();
            JWT_SECRET_KEY = Base64.getEncoder().encodeToString(secKey.getEncoded());

            // Try to load JWT expiration time from database settings
            // If database is not initialized yet, this will fail and we'll use fallback
            try {
                this.jwtExpirationTimeMillis = securitySettingsService.getJwtExpirationTimeMillis();
                log.info("JwtTokenProvider: Using database settings - expiration time: {} milliseconds ({} minutes)",
                        jwtExpirationTimeMillis, jwtExpirationTimeMillis / (60 * 1000));
            } catch (Exception dbException) {
                // Database settings not available yet (normal during startup)
                log.debug("JwtTokenProvider: Database settings not available during initialization, will use fallback");
                this.jwtExpirationTimeMillis = defaultJwtExpirationMinutes * 60 * 1000;
                log.info("JwtTokenProvider: Using fallback JWT expiration time from application.properties: {} milliseconds ({} minutes)",
                        jwtExpirationTimeMillis, defaultJwtExpirationMinutes);
            }

            // Try to load JWT refresh token expiration time from database settings
            try {
                this.jwtRefreshExpirationTimeMillis = securitySettingsService.getJwtRefreshExpirationTimeMillis();
                log.info("JwtTokenProvider: Using database settings - refresh token expiration time: {} milliseconds ({} minutes)",
                        jwtRefreshExpirationTimeMillis, jwtRefreshExpirationTimeMillis / (60 * 1000));
            } catch (Exception dbException) {
                // Database settings not available yet (normal during startup)
                log.debug("JwtTokenProvider: Database settings not available during initialization, will use fallback for refresh token");
                this.jwtRefreshExpirationTimeMillis = defaultJwtRefreshExpirationMinutes * 60 * 1000;
                log.info("JwtTokenProvider: Using fallback JWT refresh token expiration time from application.properties: {} milliseconds ({} minutes)",
                        jwtRefreshExpirationTimeMillis, defaultJwtRefreshExpirationMinutes);
            }

        } catch (Exception e) {
            log.error("JwtTokenProvider: Error during initialization", e);
            // Fallback to application.properties value
            this.jwtExpirationTimeMillis = defaultJwtExpirationMinutes * 60 * 1000;
            log.warn("JwtTokenProvider: Using fallback JWT expiration time from application.properties: {} milliseconds ({} minutes)",
                    jwtExpirationTimeMillis, defaultJwtExpirationMinutes);
            this.jwtRefreshExpirationTimeMillis = defaultJwtRefreshExpirationMinutes * 60 * 1000;
            log.warn("JwtTokenProvider: Using fallback JWT refresh token expiration time from application.properties: {} milliseconds ({} minutes)",
                    jwtRefreshExpirationTimeMillis, defaultJwtRefreshExpirationMinutes);
        }
    }

    /**
     * Refresh JWT expiration time from database settings.
     * Call this method if you want to pick up changes made to the database
     * without restarting the application.
     */
    public void refreshExpirationTime() {
        try {
            this.jwtExpirationTimeMillis = securitySettingsService.getJwtExpirationTimeMillis();
            log.info("JWT expiration time refreshed from database: {} milliseconds", jwtExpirationTimeMillis);
        } catch (Exception e) {
            log.error("Failed to refresh JWT expiration time from database", e);
        }
    }

    /**
     * Refresh JWT refresh token expiration time from database settings.
     * Call this method if you want to pick up changes made to the database
     * without restarting the application.
     */
    public void refreshRefreshTokenExpirationTime() {
        try {
            this.jwtRefreshExpirationTimeMillis = securitySettingsService.getJwtRefreshExpirationTimeMillis();
            log.info("JWT refresh token expiration time refreshed from database: {} milliseconds", jwtRefreshExpirationTimeMillis);
        } catch (Exception e) {
            log.error("Failed to refresh JWT refresh token expiration time from database", e);
        }
    }

    public String generateToken(Authentication authentication) {
        String name = authentication.getName();
        return generateTokenFromUsername(name);
    }

    public String generateRefreshToken(Authentication authentication) {
        String name = authentication.getName();
        return generateRefreshTokenFromUsername(name);
    }

    public String generateTokenFromUsername(String name) {
        return Jwts.builder()
                .subject(name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationTimeMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshTokenFromUsername(String name) {
        return Jwts.builder()
                .subject(name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationTimeMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
