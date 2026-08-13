package com.itineraryledger.kabengosafaris.User.Services.UserAdminServices;

import java.util.HashMap;
import java.util.HashSet;
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
import com.itineraryledger.kabengosafaris.Security.StrongPasswordGenerator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.CreateUserDTO;
import com.itineraryledger.kabengosafaris.User.DTOs.RegistrationRequest;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationException;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Giving somebody an account.
 *
 * Deliberately built on RegistrationServices rather than beside it, so an account an
 * administrator creates is subject to the same password policy, the same uniqueness
 * checks and the same expiry rules as one somebody creates for themselves. Two
 * creation paths that drift apart is how a system ends up with accounts that could
 * not be created today.
 *
 * The password is generated and thrown away. What the new colleague receives is an
 * activation link, so they choose their own — nobody has to read a temporary
 * password down a phone line, and no administrator ever knows a colleague's password.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminCreateService {

    /** Long enough that the generated value is never worth guessing at. */
    private static final int GENERATED_PASSWORD_LENGTH = 24;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RegistrationServices registrationServices;
    private final UserAdminGetService getService;
    private final IdObfuscator idObfuscator;

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(CreateUserDTO request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No details supplied", "NO_BODY"));
            }

            /*
             * A supplied password is honoured — occasionally an account is set up with
             * somebody sitting there — but the normal path generates one nobody sees.
             */
            String password = blankToNull(request.getPassword());

            // RegistrationRequest is all-args only: email, username, password, first, last, phone
            RegistrationRequest registration = new RegistrationRequest(
                trim(request.getEmail()),
                trim(request.getUsername()),
                password != null ? password : StrongPasswordGenerator.generateStrongPassword(GENERATED_PASSWORD_LENGTH),
                trim(request.getFirstName()),
                trim(request.getLastName()),
                blankToNull(request.getPhoneNumber()));

            boolean sendInvite = !Boolean.FALSE.equals(request.getSendInvite());
            User created = registrationServices.registerUser(registration, sendInvite);

            // Roles are granted after creation: registration knows nothing about them.
            Set<String> unknownRoles = new HashSet<>();
            if (!request.allRoleIds().isEmpty()) {
                Set<Role> roles = new HashSet<>();
                for (String obfuscated : request.allRoleIds()) {
                    Role role = readRole(obfuscated);
                    if (role == null) unknownRoles.add(obfuscated);
                    else roles.add(role);
                }
                created.setRoles(roles);
                created = userRepository.save(created);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("user", getService.toDTO(created));
            data.put("inviteSent", sendInvite);
            /*
             * Say so rather than swallow it. An account created with two of its three
             * roles looks finished, and the missing one surfaces later as a colleague
             * who cannot open a page they were told they could.
             */
            if (!unknownRoles.isEmpty()) data.put("unknownRoleIds", unknownRoles);

            String message = sendInvite
                ? "Account created and an activation email sent to " + created.getEmail()
                : "Account created. No activation email was sent";

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, message, data));

        } catch (RegistrationException e) {
            // policy and uniqueness failures: the message is written for a person
            log.info("Rejected account creation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, e.getMessage(), "USER_CREATE_REJECTED"));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create the account", "USER_CREATE_FAILED"));
        }
    }

    private Role readRole(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return roleRepository.findById(idObfuscator.decodeId(obfuscated)).orElse(null);
        } catch (Exception e) {
            log.warn("Unreadable role id on account creation: {}", obfuscated);
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
