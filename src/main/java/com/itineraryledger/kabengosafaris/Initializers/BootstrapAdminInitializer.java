package com.itineraryledger.kabengosafaris.Initializers;

import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.User.DTOs.RegistrationRequest;
import com.itineraryledger.kabengosafaris.User.Services.RegistrationServices.RegistrationServices;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.Security.StrongPasswordGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The first account, on an empty installation.
 *
 * A fresh database has permissions, roles, parks and templates — and nobody who can sign in. Every
 * account in this system is created by somebody who is already signed in, so provisioning a new
 * company used to end with an INSERT written by hand, which is both easy to get wrong and impossible
 * to audit.
 *
 * Only ever runs when there are ZERO users: an installation with people in it is never touched, so a
 * stray property on a restart cannot mint a superadmin.
 *
 * The password is generated and nobody sees it — not the log, not the operator, not this class. The
 * account arrives by activation email, the same way an admin-created account does, because an audit
 * entry has to name the person who acted and a shared password names nobody.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminInitializer implements ApplicationRunner, Ordered {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RegistrationServices registrationServices;

    @Value("${app.bootstrap.admin.email:}")
    private String email;

    @Value("${app.bootstrap.admin.username:admin}")
    private String username;

    @Value("${app.bootstrap.admin.first-name:System}")
    private String firstName;

    @Value("${app.bootstrap.admin.last-name:Administrator}")
    private String lastName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) return;

        if (email == null || email.isBlank()) {
            log.warn("""
                This installation has no accounts and no app.bootstrap.admin.email is set, so nobody \
                can sign in. Set that property and restart, or create the first account by hand.""");
            return;
        }

        Role superadmin = roleRepository.findByName("SUPERADMIN").orElse(null);
        if (superadmin == null) {
            log.error("Cannot create the first account: the SUPERADMIN role does not exist yet. "
                + "RoleInitializer must run before this one.");
            return;
        }

        try {
            /* generated, never printed: the account is claimed through the activation email */
            RegistrationRequest request = new RegistrationRequest(
                email.trim(),
                username.trim(),
                StrongPasswordGenerator.generateStrongPassword(20),
                firstName.trim(),
                lastName.trim(),
                null);

            User created = registrationServices.registerUser(request, true);
            created.setRoles(Set.of(superadmin));
            userRepository.save(created);

            log.info("""
                First account created: {} ({}) with SUPERADMIN. An activation email has been sent — \
                the account cannot be used until that link is followed, and no password was set for \
                anybody.""", created.getEmail(), created.getUsername());

        } catch (Exception e) {
            /*
             * Loud, and not fatal. A company whose SMTP is not configured yet still wants a booted
             * API; the operator reads this line, fixes mail, and restarts.
             */
            log.error("Could not create the first account for {}. Fix the cause and restart, or "
                + "create it by hand once mail works.", email, e);
        }
    }

    /** After roles and permissions exist, before anything that assumes an operator. */
    @Override
    public int getOrder() {
        return 27;
    }
}
