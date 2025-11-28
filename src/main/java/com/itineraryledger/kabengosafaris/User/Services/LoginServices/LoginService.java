package com.itineraryledger.kabengosafaris.User.Services.LoginServices;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Security.JwtTokenProvider;
import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.LoginRequest;
import com.itineraryledger.kabengosafaris.User.DTOs.LoginResponse;
import com.itineraryledger.kabengosafaris.User.Services.MFAServices.MFARequiredException;
import com.itineraryledger.kabengosafaris.User.Services.MFAServices.MFATempTokenProvider;

@Service
public class LoginService {
    @Autowired
    private LoginRateLimitingService loginRateLimitingService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecuritySettingsGetterServices securitySettingsGetterServices;

    @Autowired
    private MFATempTokenProvider mfaTempTokenProvider;

    public LoginResponse login (LoginRequest loginRequest) {
        String identifier = loginRequest.getIdentifier();
        String password = loginRequest.getPassword();

        if (identifier ==  null ||  identifier.isEmpty()) {
            throw new LoginException("Email or Username must be provided");
        }

        if (password == null || password.isEmpty()) {
            throw new LoginException("Password must be provided");
        }

        // Rate limiting: Prevent brute-force attacks (if enabled)
        try {
            if (securitySettingsGetterServices.getLoginRateLimitEnabled()) {
                if (!loginRateLimitingService.isAllowed(identifier)) {
                    throw new LoginException("Too many login attempts. Please try again later.");
                }
            }
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }

        User user;

        if (identifier.contains("@")) { // Email-based login
            user = userRepository.findByEmail(identifier).orElseThrow(() -> new LoginException("Invalid email or password"));
        } else { // Username-based login
            user = userRepository.findByUsername(identifier).orElseThrow(() -> new LoginException("Invalid username or password"));
        }

        try {

            if (!user.isEnabled()) {
                throw new LoginException("Your account is disabled or your email is not verified. Please contact support or request a new verification email.");
            }

            // Check if failed attempt counter should be reset based on counterResetHours
            resetCounterIfExpired(user);

            // Check if the account is locked (if account lockout policy is enabled)
            if (securitySettingsGetterServices.getAccountLockoutEnabled() && !user.isAccountNonLocked()) {
                if (unlockWhenTimeExpired(user)) {
                    // Account was locked but is now unlocked
                    resetFailedAttempts(user);
                } else {
                    throw new LoginException("Your account is locked. Please contact support to unlock it.");
                }
            }

            if (user.getPasswordExpiryDate() != null && user.getPasswordExpiryDate().isBefore(LocalDateTime.now())) {
                LocalDateTime passwordExpiryDate = user.getPasswordExpiryDate();
                throw new LoginException("Your password has expired, since " + passwordExpiryDate.toString() + ". Please change or reset your password.");
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), password)
            );

            if (authentication.isAuthenticated()) {
                // Reset failed attempts after successful login
                if (user.getFailedAttempt() > 0) {
                    resetFailedAttempts(user);
                }

                // Check if MFA is enabled and confirmed
                if (user.isMfaEnabled() && user.getMfaConfirmed()) {
                    // MFA is required - generate temporary token and throw exception
                    String tempToken = mfaTempTokenProvider.generateMFATempToken(user);
                    throw new MFARequiredException("MFA verification required", tempToken);
                }

                LoginResponse loginResponse = new LoginResponse();
                loginResponse.setAccessToken(jwtTokenProvider.generateToken(authentication));
                loginResponse.setRefreshToken(jwtTokenProvider.generateRefreshToken(authentication));
                try {
                    loginResponse.setRefreshTokenExpiresIn(securitySettingsGetterServices.getJwtRefreshExpirationTimeMillis());
                    loginResponse.setAccessTokenExpiresIn(securitySettingsGetterServices.getJwtExpirationTimeMillis());
                    String accessTokenExpiryDateTime = LocalDateTime.now().plusSeconds(securitySettingsGetterServices.getJwtExpirationTimeMillis() / 1000).toString();
                    String refreshTokenExpiryDateTime = LocalDateTime.now().plusSeconds(securitySettingsGetterServices.getJwtRefreshExpirationTimeMillis() / 1000).toString();
                    loginResponse.setAccessTokenExpiresAt(accessTokenExpiryDateTime);
                    loginResponse.setRefreshTokenExpiresAt(refreshTokenExpiryDateTime);
                } catch (Exception e) {
                    // ignore
                }
                return loginResponse;
            }
        } catch (LockedException ex) {
            throw new LoginException("Your account is currently locked for security reasons. Please contact support to unlock it.");
        } catch (DisabledException ex) {
            throw new LoginException("Your account is disabled or your email is not verified. Please contact support or request a new verification email.");
        } catch (CredentialsExpiredException ex) {
            throw new LoginException("Your password has expired. Please reset your password.");
        } catch (AuthenticationException ex) {
            // Handle failed login attempts for database users (if account lockout is enabled)
            if (securitySettingsGetterServices.getAccountLockoutEnabled()) {
                int maxFailedAttempts = securitySettingsGetterServices.getMaxFailedAttempts();
                if (user.getFailedAttempt() >= maxFailedAttempts - 1) {
                    lock(user);
                } else {
                    increaseFailedAttempts(user);
                }
            }
            throw new LoginException("Invalid username or password");
        } catch (MFARequiredException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LoginException("An error occurred during login: " + ex.getMessage());
        }
        throw new LoginException("Login failed due to unknown reasons");
    }
    
    public LoginResponse loginResponseMFA(User user) {
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken(jwtTokenProvider.generateTokenFromUsername(user.getUsername()));
        loginResponse.setRefreshToken(jwtTokenProvider.generateRefreshTokenFromUsername(user.getUsername()));
        try {
            loginResponse.setRefreshTokenExpiresIn(securitySettingsGetterServices.getJwtRefreshExpirationTimeMillis());
            loginResponse.setAccessTokenExpiresIn(securitySettingsGetterServices.getJwtExpirationTimeMillis());
            String accessTokenExpiryDateTime = LocalDateTime.now().plusSeconds(securitySettingsGetterServices.getJwtExpirationTimeMillis() / 1000).toString();
            String refreshTokenExpiryDateTime = LocalDateTime.now().plusSeconds(securitySettingsGetterServices.getJwtRefreshExpirationTimeMillis() / 1000).toString();
            loginResponse.setAccessTokenExpiresAt(accessTokenExpiryDateTime);
            loginResponse.setRefreshTokenExpiresAt(refreshTokenExpiryDateTime);
        } catch (Exception e) {
            // ignore
        }
        return loginResponse;
    }
    /**
     * Resets the failed attempt counter if the counterResetHours has expired.
     * This allows users to get fresh login attempts after being inactive for the configured period.
     *
     * @param user the user to check and potentially reset
     */
    private void resetCounterIfExpired(User user) {
        if (user.getLastFailedAttemptTime() == null || user.getFailedAttempt() == 0) {
            return;
        }

        try {
            int counterResetHours = securitySettingsGetterServices.getLockoutCounterResetHours();
            if (counterResetHours == 0) {
                return; // 0 means never reset
            }

            LocalDateTime resetDeadline = user.getLastFailedAttemptTime().plusHours(counterResetHours);

            if (LocalDateTime.now().isAfter(resetDeadline)) {
                resetFailedAttempts(user);
            }
        } catch (Exception e) {
            // Log error but don't fail - proceed with login attempt
            org.slf4j.LoggerFactory.getLogger(LoginService.class).error("Error checking counterResetHours for user: {}", user.getUsername(), e);
        }
    }
     
    /**
     * Determines whether a user's account lock period has expired and, if so, unlocks the account.
     *
     * <p>The method checks the user's accountLockedTime and computes an unlock deadline based on
     * the database-driven accountLockout.lockoutDurationMinutes setting. If the lock period has
     * expired, the method:
     * - sets accountLocked to false,
     * - clears accountLockedTime (sets it to null),
     * - resets failedAttempt to 0,
     * - persists the updated user via userRepository.save(user),
     * and returns true.</p>
     *
     * <p>If accountLockedTime is null or the lock period has not yet expired, no changes are made
     * and the method returns false.</p>
     *
     * <p>Note: this method mutates and persists the provided User instance and relies on the
     * system clock (LocalDateTime) for time-based decisions.</p>
     *
     * @param user the User whose lock expiration should be evaluated and potentially cleared; must not be null
     * @return true if the account was unlocked and the user was persisted; false if the account was not unlocked
     * @throws NullPointerException if the provided user is null
     */
    public boolean unlockWhenTimeExpired(User user) {
        LocalDateTime lockTime = user.getAccountLockedTime();
        if (lockTime == null) {
            return false;
        }

        try {
            int lockoutDurationMinutes = securitySettingsGetterServices.getLockoutDurationMinutes();
            LocalDateTime unlockTime = lockTime.plusMinutes(lockoutDurationMinutes);

            if (LocalDateTime.now().isAfter(unlockTime)) {
                user.setAccountLocked(false);
                user.setAccountLockedTime(null);
                user.setFailedAttempt(0);
                userRepository.save(user);
                return true;
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(LoginService.class).error("Error checking lockout duration for user: {}", user.getUsername(), e);
        }
        return false;
    }
     
    private void resetFailedAttempts(User user) {
        user.setFailedAttempt(0);
        user.setLastFailedAttemptTime(null);
        userRepository.save(user);
    }
     
    private void lock(User user) {
        user.setAccountLocked(true);
        user.setAccountLockedTime(LocalDateTime.now());
        userRepository.save(user);
    }
    
     
    private void increaseFailedAttempts(User user) {
        user.setLastFailedAttemptTime(LocalDateTime.now());
        user.setFailedAttempt(user.getFailedAttempt() + 1);
        userRepository.save(user);
    }
    
}
