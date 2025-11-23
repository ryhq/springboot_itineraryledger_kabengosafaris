package com.itineraryledger.kabengosafaris.Security;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;

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
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecuritySettingsGetterServices securitySettingsGetterServices;

    private String JWT_SECRET_KEY = "";

    @Value("${security.jwt.expiration.time.minutes:180}")
    private long jwtExpirationMinutes;

    @Value("${security.jwt.refresh.expiration.time.minutes:1440}")
    private long jwtRefreshExpirationMinutes;

    private long jwtExpirationTimeMillis;

    private long jwtRefreshExpirationTimeMillis;

    @Autowired
    public JwtTokenProvider(SecuritySettingsGetterServices securitySettingsGetterServices) {
        this.securitySettingsGetterServices = securitySettingsGetterServices;

        // Generate or load JWT secret key
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey secKey = keyGenerator.generateKey();
            JWT_SECRET_KEY = Base64.getEncoder().encodeToString(secKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("JwtTokenProvider: Error generating JWT secret key", e);
        }

        initialize(this.securitySettingsGetterServices);
    }

    public void reloadConfig(SecuritySettingsGetterServices securitySettingsGetterServices) {
        initialize(securitySettingsGetterServices);
    }

    private void initialize(SecuritySettingsGetterServices securitySettingsGetterServices) {
        try {
            // Try to load JWT expiration time SecuritySettingsServices
            jwtExpirationMinutes = securitySettingsGetterServices.getJwtExpirationMinutes();
            jwtRefreshExpirationMinutes = securitySettingsGetterServices.getJwtRefreshExpirationMinutes();

            this.jwtExpirationTimeMillis = jwtExpirationMinutes * 60 * 1000;
            this.jwtRefreshExpirationTimeMillis = jwtRefreshExpirationMinutes * 60 * 1000;

            log.info("JwtTokenProvider: Loaded JWT expiration time from SecuritySettingsServices: {} milliseconds ({} minutes)",
                    jwtExpirationTimeMillis, jwtExpirationMinutes);
            log.info("JwtTokenProvider: Loaded JWT refresh token expiration time from SecuritySettingsServices: {} milliseconds ({} minutes)",
                    jwtRefreshExpirationTimeMillis, jwtRefreshExpirationMinutes);
            
        } catch (Exception e) {
            log.error("JwtTokenProvider: Error during initialization", e);
            // Fallback to application.properties value
            log.warn("JwtTokenProvider: Using fallback JWT expiration time from application.properties: {} milliseconds ({} minutes)",
                    jwtExpirationTimeMillis, jwtExpirationMinutes);
            this.jwtRefreshExpirationTimeMillis = jwtRefreshExpirationMinutes * 60 * 1000;
            log.warn("JwtTokenProvider: Using fallback JWT refresh token expiration time from application.properties: {} milliseconds ({} minutes)",
                    jwtRefreshExpirationTimeMillis, jwtRefreshExpirationMinutes);
        }
    }

    // @PostConstruct
    // private void postConstruct() {
    //     initialize(securitySettingsGetterServices);
    // }


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
