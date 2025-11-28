package com.itineraryledger.kabengosafaris.User.Controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.TokenType;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller for token refresh operations
 * Handles refresh token validation and new access token generation
 */
@RestController
@RequestMapping("/api/auth/token")
@Slf4j
public class TokenController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    /**
     * Refresh access token using a refresh token
     * Only REFRESH tokens are accepted by this endpoint
     *
     * @param refreshToken The refresh token (from Authorization header)
     * @return New access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "Missing or invalid authorization header", "MISSING_TOKEN")
                );
            }

            String refreshToken = authHeader.substring(7);

            // Validate the refresh token
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "Invalid or expired refresh token", "INVALID_REFRESH_TOKEN")
                );
            }

            // Validate token type is REFRESH
            TokenType tokenType = jwtTokenProvider.getTokenType(refreshToken);
            if (tokenType != TokenType.REFRESH) {
                log.warn("Attempt to refresh token with wrong token type: {}", tokenType);
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401,
                        "Invalid token type. Only REFRESH tokens can be used for token refresh",
                        "INVALID_TOKEN_TYPE"
                    )
                );
            }

            // Extract username and generate new access token
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                return ResponseEntity.status(401).body(
                    ApiResponse.error(401, "User not found", "USER_NOT_FOUND")
                );
            }

            String newAccessToken = jwtTokenProvider.generateTokenFromUsername(username);

            return ResponseEntity.ok(
                ApiResponse.success(200, "Access token refreshed successfully",
                    new TokenResponse(newAccessToken, "Bearer", TokenType.ACCESS.getType())
                )
            );

        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage());
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Token refresh failed", "TOKEN_REFRESH_FAILED")
            );
        }
    }

    /**
     * Response class for token refresh
     */
    public static class TokenResponse {
        private String accessToken;
        private String tokenType;
        private String type;

        public TokenResponse(String accessToken, String tokenType, String type) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.type = type;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public String getType() {
            return type;
        }
    }
}
