package com.itineraryledger.kabengosafaris.User.Services.MFAServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.TokenType;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MFATempTokenProvider {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    /**
     * Generate a temporary token for MFA verification
     * Token is valid for 5 minutes
     */
    public String generateMFATempToken(User user) {
        try {
            // Generate JWT token with user's username
            // Uses the standard JWT generation but the short expiration is handled
            // by the client - the token is for MFA verification only
            String token = jwtTokenProvider.generateMFATokenFromUsername(user.getUsername());
            return token;
        } catch (Exception e) {
            log.error("Error generating MFA temporary token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate MFA temporary token and retrieve user
     * Returns User if token is valid and has MFA token type, null otherwise
     */
    public User validateMFATempToken(String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                // Validate token type is MFA
                TokenType tokenType = jwtTokenProvider.getTokenType(token);
                if (tokenType != TokenType.MFA) {
                    log.warn("Attempt to use MFA endpoint with wrong token type: {}", tokenType);
                    return null;
                }

                String username = jwtTokenProvider.getUsernameFromToken(token);
                return userRepository.findByUsername(username).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error validating MFA temporary token: {}", e.getMessage());
        }
        return null;
    }
}
