package com.itineraryledger.kabengosafaris.User.Services.UserAdminServices;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.UpdateUserDTO;
import com.itineraryledger.kabengosafaris.User.Services.PasswordResetServices.PasswordResetService;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.AccountVerificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Changing somebody else's account.
 *
 * Every method here is null-means-skip, so the record page can save one field at a
 * time without the rest of the form overwriting what a colleague changed a moment
 * ago.
 *
 * Two guards run through the whole class, and they exist because the mistakes they
 * prevent cannot be undone from inside the application:
 *
 *   · You cannot switch off, lock or strip the roles from your OWN account. The
 *     request would succeed and the next one would be refused — locked out by your
 *     own click, with no way back in to reverse it.
 *   · You cannot remove the last way in. If a change would leave no enabled account
 *     holding an active SUPERADMIN role, it is refused. Otherwise the only remedy is
 *     a hand-written UPDATE against the database, which is not a remedy an office has.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminUpdateService {

    private static final String SUPERADMIN = "SUPERADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserAdminGetService getService;
    private final PasswordResetService passwordResetService;
    private final AccountVerificationService accountVerificationService;
    private final IdObfuscator idObfuscator;

    @Transactional
    public ResponseEntity<ApiResponse<?>> update(String idObfuscated, UpdateUserDTO request, User actor) {
        try {
            User user = read(idObfuscated);
            if (user == null) return notFound();
            if (request == null) return ok(user, "Nothing to change");

            // Refusing to switch off your own account, before anything is written.
            if (Boolean.FALSE.equals(request.getEnabled())) {
                ResponseEntity<ApiResponse<?>> refusal = refuseSelfLockout(user, actor, "deactivate");
                if (refusal != null) return refusal;
            }

            if (request.getFirstName() != null) user.setFirstName(request.getFirstName().trim());
            if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
            if (request.getBio() != null) user.setBio(request.getBio());

            if (request.getEmail() != null) {
                String email = request.getEmail().trim().toLowerCase();
                if (!email.equalsIgnoreCase(user.getEmail())
                    && userRepository.findByEmail(email).isPresent()) {
                    return rejected("That email is already on another account");
                }
                user.setEmail(email);
            }

            if (request.getUsername() != null) {
                String username = request.getUsername().trim();
                if (!username.equalsIgnoreCase(user.getUsername())
                    && userRepository.findByUsername(username).isPresent()) {
                    return rejected("That username is already taken");
                }
                user.setUsername(username);
            }

            if (request.getPhoneNumber() != null) {
                String phone = request.getPhoneNumber().trim();
                if (phone.isEmpty()) {
                    user.setPhoneNumber(null);
                } else {
                    if (!phone.equals(user.getPhoneNumber())
                        && userRepository.findByPhoneNumber(phone).isPresent()) {
                        return rejected("That phone number is already on another account");
                    }
                    user.setPhoneNumber(phone);
                }
            }

            if (request.getEnabled() != null) user.setEnabled(request.getEnabled());

            Set<String> unknownRoles = new HashSet<>();
            if (request.getRoleIds() != null) {
                Set<Role> roles = resolveRoles(request.getRoleIds(), unknownRoles);
                ResponseEntity<ApiResponse<?>> refusal = refuseIfLosingOwnAccess(user, actor, roles);
                if (refusal != null) return refusal;
                user.setRoles(roles);
            }

            // The last-way-in check reads the pending state, so it must run before saving.
            ResponseEntity<ApiResponse<?>> stranded = refuseIfStrandingTheSystem(user);
            if (stranded != null) return stranded;

            User saved = userRepository.save(user);
            Map<String, Object> data = new HashMap<>();
            data.put("user", getService.toDTO(saved));
            if (!unknownRoles.isEmpty()) data.put("unknownRoleIds", unknownRoles);
            return ResponseEntity.ok(ApiResponse.success(200, "Account updated", data));

        } catch (Exception e) {
            log.error("Error updating user {}", idObfuscated, e);
            return failed("Failed to update the account", "USER_UPDATE_FAILED");
        }
    }

    /** Out of the pickers and out of the system, history kept. */
    @Transactional
    public ResponseEntity<ApiResponse<?>> setEnabled(String idObfuscated, boolean enabled, User actor) {
        try {
            User user = read(idObfuscated);
            if (user == null) return notFound();

            if (!enabled) {
                ResponseEntity<ApiResponse<?>> refusal = refuseSelfLockout(user, actor, "deactivate");
                if (refusal != null) return refusal;
            }

            user.setEnabled(enabled);

            ResponseEntity<ApiResponse<?>> stranded = refuseIfStrandingTheSystem(user);
            if (stranded != null) return stranded;

            User saved = userRepository.save(user);
            return ok(saved, enabled ? "Account reactivated" : "Account deactivated");
        } catch (Exception e) {
            log.error("Error changing enabled state for {}", idObfuscated, e);
            return failed("Failed to change the account state", "USER_STATE_FAILED");
        }
    }

    /**
     * Letting somebody back in after the lockout.
     *
     * Clearing the counter is the whole point: leaving it at its limit means the next
     * wrong password locks them straight out again, which reads as the unlock not
     * having worked.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> setLocked(String idObfuscated, boolean locked, User actor) {
        try {
            User user = read(idObfuscated);
            if (user == null) return notFound();

            if (locked) {
                ResponseEntity<ApiResponse<?>> refusal = refuseSelfLockout(user, actor, "lock");
                if (refusal != null) return refusal;
            }

            user.setAccountLocked(locked);
            if (locked) {
                user.setAccountLockedTime(java.time.LocalDateTime.now());
            } else {
                user.setAccountLockedTime(null);
                user.setFailedAttempt(0);
                user.setLastFailedAttemptTime(null);
            }

            User saved = userRepository.save(user);
            return ok(saved, locked ? "Account locked" : "Account unlocked and the failed-attempt count cleared");
        } catch (Exception e) {
            log.error("Error changing lock state for {}", idObfuscated, e);
            return failed("Failed to change the lock state", "USER_LOCK_FAILED");
        }
    }

    /**
     * Turning off a colleague's second factor.
     *
     * The real case is a lost or wiped phone: the authenticator is gone and its codes
     * are unrecoverable, so somebody has to clear the enrolment before they can set it
     * up again. The seed and the backup codes are both dropped — leaving either behind
     * would let the old phone, if it ever turns up, still sign in.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> resetMfa(String idObfuscated) {
        try {
            User user = read(idObfuscated);
            if (user == null) return notFound();

            user.setMfaEnabled(false);
            user.setMfaConfirmed(false);
            user.setMfaSecret(null);
            user.setMfaBackupCodes(null);
            user.setMfaEnabledAt(null);
            user.setLastMfaVerification(null);

            User saved = userRepository.save(user);
            log.info("MFA enrolment cleared for {}", saved.getUsername());
            return ok(saved, "Two-factor authentication cleared. They will be asked to set it up again");
        } catch (Exception e) {
            log.error("Error resetting MFA for {}", idObfuscated, e);
            return failed("Failed to clear two-factor authentication", "USER_MFA_RESET_FAILED");
        }
    }

    /**
     * Mailing a password-reset link.
     *
     * Not "set their password to this": an administrator who knows a colleague's
     * password can act as them, and every audit stamp would then name the wrong
     * person. The link goes to their mailbox and only they can finish it.
     */
    public ResponseEntity<ApiResponse<?>> sendPasswordReset(String idObfuscated) {
        try {
            User user = read(idObfuscated);
            if (user == null) return notFound();

            passwordResetService.requestPasswordReset(user.getEmail());
            return ResponseEntity.ok(ApiResponse.success(200,
                "A password reset link has been sent to " + user.getEmail(), Map.of()));
        } catch (Exception e) {
            log.error("Error sending password reset for {}", idObfuscated, e);
            return failed("Failed to send the reset link", "USER_RESET_SEND_FAILED");
        }
    }

    /** For the invite that was lost, or went out before the mailbox existed. */
    public ResponseEntity<ApiResponse<?>> resendInvite(String idObfuscated) {
        try {
            User user = read(idObfuscated);
            if (user == null) return notFound();

            if (Boolean.TRUE.equals(user.getEnabled())) {
                return rejected("That account is already active — send a password reset instead");
            }

            accountVerificationService.resendVerificationEmail(user.getEmail());
            return ResponseEntity.ok(ApiResponse.success(200,
                "A new activation email has been sent to " + user.getEmail(), Map.of()));
        } catch (Exception e) {
            log.error("Error resending invite for {}", idObfuscated, e);
            return failed("Failed to send the activation email", "USER_INVITE_SEND_FAILED");
        }
    }

    /* ------------------------------------------------------------------ guards */

    /**
     * Refuses a change that would lock the person making it out of the application.
     *
     * There is no undo for this from inside the app: the very next request is refused,
     * including the one that would put it back.
     */
    private ResponseEntity<ApiResponse<?>> refuseSelfLockout(User target, User actor, String verb) {
        if (actor == null || !actor.getId().equals(target.getId())) return null;
        return ResponseEntity.badRequest().body(ApiResponse.error(400,
            "You cannot " + verb + " your own account — ask another administrator to do it",
            "SELF_LOCKOUT_REFUSED"));
    }

    /** The same guard for roles: dropping your own administration rights. */
    private ResponseEntity<ApiResponse<?>> refuseIfLosingOwnAccess(User target, User actor, Set<Role> roles) {
        if (actor == null || !actor.getId().equals(target.getId())) return null;

        boolean keepsAdministration = roles.stream()
            .filter(role -> Boolean.TRUE.equals(role.getActive()))
            .anyMatch(role -> role.hasPermission("UPDATE_USER") || role.hasPermission("UPDATE_ROLE"));

        if (keepsAdministration) return null;

        return ResponseEntity.badRequest().body(ApiResponse.error(400,
            "That would remove your own access to user management — ask another administrator to change your roles",
            "SELF_LOCKOUT_REFUSED"));
    }

    /**
     * Refuses a change that would leave nobody able to administer the system.
     *
     * Reads the pending state of the account being changed, and the saved state of
     * everybody else, so it catches the last superadmin being deactivated and the last
     * superadmin having the role taken away — which are the same accident.
     */
    private ResponseEntity<ApiResponse<?>> refuseIfStrandingTheSystem(User pending) {
        boolean pendingIsAWayIn = isAWayIn(pending);
        if (pendingIsAWayIn) return null;

        long others = userRepository.findAll().stream()
            .filter(other -> !other.getId().equals(pending.getId()))
            .filter(this::isAWayIn)
            .count();

        if (others > 0) return null;

        return ResponseEntity.badRequest().body(ApiResponse.error(400,
            "That would leave no active " + SUPERADMIN + " account, and nobody could administer the system afterwards. "
                + "Grant the role to somebody else first",
            "LAST_SUPERADMIN_REFUSED"));
    }

    /** An enabled, unlocked account holding an active SUPERADMIN role. */
    private boolean isAWayIn(User user) {
        if (!Boolean.TRUE.equals(user.getEnabled())) return false;
        if (Boolean.TRUE.equals(user.getAccountLocked())) return false;
        if (user.getRoles() == null) return false;
        return user.getRoles().stream()
            .anyMatch(role -> Boolean.TRUE.equals(role.getActive())
                && SUPERADMIN.equalsIgnoreCase(role.getName()));
    }

    /* ------------------------------------------------------------------ plumbing */

    private Set<Role> resolveRoles(List<String> obfuscatedIds, Set<String> unknown) {
        Set<Role> roles = new HashSet<>();
        for (String obfuscated : obfuscatedIds) {
            if (obfuscated == null || obfuscated.isBlank()) continue;
            try {
                Role role = roleRepository.findById(idObfuscator.decodeId(obfuscated)).orElse(null);
                if (role == null) unknown.add(obfuscated);
                else roles.add(role);
            } catch (Exception e) {
                log.warn("Unreadable role id on a user update: {}", obfuscated);
                unknown.add(obfuscated);
            }
        }
        return roles;
    }

    private User read(String idObfuscated) {
        try {
            return userRepository.findById(idObfuscator.decodeId(idObfuscated)).orElse(null);
        } catch (Exception e) {
            log.warn("Unreadable user id: {}", idObfuscated);
            return null;
        }
    }

    private ResponseEntity<ApiResponse<?>> ok(User user, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("user", getService.toDTO(user));
        return ResponseEntity.ok(ApiResponse.success(200, message, data));
    }

    private ResponseEntity<ApiResponse<?>> notFound() {
        return ResponseEntity.status(404).body(
            ApiResponse.error(404, "User not found", "USER_NOT_FOUND"));
    }

    private ResponseEntity<ApiResponse<?>> rejected(String message) {
        return ResponseEntity.badRequest().body(
            ApiResponse.error(400, message, "USER_UPDATE_REJECTED"));
    }

    private ResponseEntity<ApiResponse<?>> failed(String message, String code) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ApiResponse.error(500, message, code));
    }
}
