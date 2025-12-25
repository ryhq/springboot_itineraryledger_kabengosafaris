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

    @Value("${security.mfa.jwt.expiration.time.seconds:180}")
    private long mfaJwtExpirationTimeSeconds;

    @Value("${security.registration.jwt.expiration.time.minutes:60}")
    private long registrationJwtExpirationMinutes;

    private long jwtExpirationTimeMillis;

    private long jwtRefreshExpirationTimeMillis;

    private long mfaJwtExpirationTimeMillis;

    private long registrationJwtExpirationTimeMillis;

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
            mfaJwtExpirationTimeSeconds = securitySettingsGetterServices.getMFAJwtExpirationMinutes();
            registrationJwtExpirationMinutes = securitySettingsGetterServices.getRegistrationJwtExpirationMinutes();
            jwtRefreshExpirationMinutes = securitySettingsGetterServices.getJwtRefreshExpirationMinutes();

            this.jwtExpirationTimeMillis = jwtExpirationMinutes * 60 * 1000;
            this.mfaJwtExpirationTimeMillis = mfaJwtExpirationTimeSeconds * 1000;
            this.registrationJwtExpirationTimeMillis = registrationJwtExpirationMinutes * 60 * 1000;
            this.jwtRefreshExpirationTimeMillis = jwtRefreshExpirationMinutes * 60 * 1000;

            log.info("JwtTokenProvider: Loaded JWT expiration time from SecuritySettingsServices: {} milliseconds ({} minutes)",
                    jwtExpirationTimeMillis, jwtExpirationMinutes);
            log.info("JwtTokenProvider: Loaded MFA JWT expiration time from SecuritySettingsServices: {} milliseconds ({} seconds)",
                    mfaJwtExpirationTimeMillis, mfaJwtExpirationTimeSeconds);
            log.info("JwtTokenProvider: Loaded Registration JWT expiration time from SecuritySettingsServices: {} milliseconds ({} minutes)",
                    registrationJwtExpirationTimeMillis, registrationJwtExpirationMinutes);
            log.info("JwtTokenProvider: Loaded JWT refresh token expiration time from SecuritySettingsServices: {} milliseconds ({} minutes)",
                    jwtRefreshExpirationTimeMillis, jwtRefreshExpirationMinutes);
            
        } catch (Exception e) {
            log.error("JwtTokenProvider: Error during initialization", e);
            // Fallback to application.properties value
            this.jwtExpirationTimeMillis = jwtExpirationMinutes * 60 * 1000;
            this.mfaJwtExpirationTimeMillis = mfaJwtExpirationTimeSeconds * 1000;
            this.registrationJwtExpirationTimeMillis = registrationJwtExpirationMinutes * 60 * 1000;
            this.jwtRefreshExpirationTimeMillis = jwtRefreshExpirationMinutes * 60 * 1000;
            log.info("JwtTokenProvider: Fallback to application.properties JWT expiration time: {} milliseconds ({} minutes)",
                    jwtExpirationTimeMillis, jwtExpirationMinutes);
            log.info("JwtTokenProvider: Fallback to application.properties MFA JWT expiration time: {} milliseconds ({} seconds)",
                    mfaJwtExpirationTimeMillis, mfaJwtExpirationTimeSeconds);
            log.info("JwtTokenProvider: Fallback to application.properties Registration JWT expiration time: {} milliseconds ({} minutes)",
                    registrationJwtExpirationTimeMillis, registrationJwtExpirationMinutes);
            log.info("JwtTokenProvider: Fallback to application.properties JWT refresh token expiration time: {} milliseconds ({} minutes)",
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

    public String generateMFATokenFromUsername(String name) {
        return Jwts.builder()
                .subject(name)
                .claim("type", TokenType.MFA.getType())
                .claim("aud", "mfa")
                .claim("purpose", "mfa_verify")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + mfaJwtExpirationTimeMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRegistrationTokenFromUsername(String name) {
        return Jwts.builder()
                .subject(name)
                .claim("type", TokenType.REGISTRATION.getType())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + registrationJwtExpirationTimeMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenFromUsername(String name) {
        return Jwts.builder()
                .subject(name)
                .claim("type", TokenType.ACCESS.getType())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationTimeMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshTokenFromUsername(String name) {
        return Jwts.builder()
                .subject(name)
                .claim("type", TokenType.REFRESH.getType())
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

    public TokenType getTokenType(String token) {
        try {
            Object typeClaim = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("type");

            if (typeClaim != null) {
                return TokenType.fromString(typeClaim.toString());
            }
            // Default to ACCESS for backward compatibility
            return TokenType.ACCESS;
        } catch (Exception e) {
            log.error("Error extracting token type: {}", e.getMessage());
            return null;
        }
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
