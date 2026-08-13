package com.itineraryledger.kabengosafaris.Initializers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Permission.Permission;
import com.itineraryledger.kabengosafaris.Permission.PermissionAction;
import com.itineraryledger.kabengosafaris.Permission.PermissionRepository;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RoleInitializer - Initializes and updates system roles at application startup
 *
 * This initializer creates default system roles with predefined permissions:
 *
 * 1. SUPERADMIN - Has ALL permissions (CREATE, READ, UPDATE, DELETE) on all entities
 * 2. ADMIN - Has all permissions EXCEPT DELETE, plus full control of users, roles and
 *    permissions including DELETE (see ADMINISTRATION_ENTITIES)
 * 3. USER - Has CREATE and READ permissions only (no UPDATE, no DELETE)
 * 4. GUEST - Has READ permission only on all entities
 *
 * Runs AFTER PermissionInitializer (Order = HIGHEST_PRECEDENCE + 20)
 *
 * Behavior:
 * - If a role doesn't exist, it will be created with appropriate permissions
 * - If a role exists, it will be updated with any NEW permissions that have been added
 * - This ensures roles automatically get permissions for new entities added to the system
 * - Existing permissions are never removed, only new ones are added
 *
 * These are system roles and cannot be deleted via API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 4) // Run fourth after PermissionInitializer
public class RoleInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Runs at application startup to initialize system roles
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = false;

        try {
            int createdCount = 0;
            int updatedCount = 0;

            // Initialize or update SUPERADMIN role
            Role superAdmin = roleRepository.findByName("SUPERADMIN").orElse(null);
            if (superAdmin == null) {
                superAdmin = createSuperAdminRole();
                roleRepository.save(superAdmin);
                log.info("Created SUPERADMIN role with {} permissions", superAdmin.getPermissions().size());
                createdCount++;
            } else {
                int beforeCount = superAdmin.getPermissions().size();
                updateRolePermissions(superAdmin, getSuperAdminPermissions());
                roleRepository.save(superAdmin);
                int afterCount = superAdmin.getPermissions().size();
                if (afterCount > beforeCount) {
                    log.info("Updated SUPERADMIN role: {} -> {} permissions (+{})",
                        beforeCount, afterCount, afterCount - beforeCount);
                    updatedCount++;
                } else {
                    log.debug("SUPERADMIN role already up-to-date with {} permissions", afterCount);
                }
            }

            // Initialize or update ADMIN role
            Role admin = roleRepository.findByName("ADMIN").orElse(null);
            if (admin == null) {
                admin = createAdminRole();
                roleRepository.save(admin);
                log.info("Created ADMIN role with {} permissions", admin.getPermissions().size());
                createdCount++;
            } else {
                int beforeCount = admin.getPermissions().size();
                updateRolePermissions(admin, getAdminPermissions());
                roleRepository.save(admin);
                int afterCount = admin.getPermissions().size();
                if (afterCount > beforeCount) {
                    log.info("Updated ADMIN role: {} -> {} permissions (+{})",
                        beforeCount, afterCount, afterCount - beforeCount);
                    updatedCount++;
                } else {
                    log.debug("ADMIN role already up-to-date with {} permissions", afterCount);
                }
            }

            // Initialize or update USER role
            Role user = roleRepository.findByName("USER").orElse(null);
            if (user == null) {
                user = createUserRole();
                roleRepository.save(user);
                log.info("Created USER role with {} permissions", user.getPermissions().size());
                createdCount++;
            } else {
                int beforeCount = user.getPermissions().size();
                updateRolePermissions(user, getUserPermissions());
                roleRepository.save(user);
                int afterCount = user.getPermissions().size();
                if (afterCount > beforeCount) {
                    log.info("Updated USER role: {} -> {} permissions (+{})",
                        beforeCount, afterCount, afterCount - beforeCount);
                    updatedCount++;
                } else {
                    log.debug("USER role already up-to-date with {} permissions", afterCount);
                }
            }

            // Initialize or update GUEST role
            Role guest = roleRepository.findByName("GUEST").orElse(null);
            if (guest == null) {
                guest = createGuestRole();
                roleRepository.save(guest);
                log.info("Created GUEST role with {} permissions", guest.getPermissions().size());
                createdCount++;
            } else {
                int beforeCount = guest.getPermissions().size();
                updateRolePermissions(guest, getGuestPermissions());
                roleRepository.save(guest);
                int afterCount = guest.getPermissions().size();
                if (afterCount > beforeCount) {
                    log.info("Updated GUEST role: {} -> {} permissions (+{})",
                        beforeCount, afterCount, afterCount - beforeCount);
                    updatedCount++;
                } else {
                    log.debug("GUEST role already up-to-date with {} permissions", afterCount);
                }
            }

            log.info("Role initialization complete: {} roles created, {} roles updated",
                    createdCount, updatedCount);

            // Log summary of all system roles
            List<Role> systemRoles = roleRepository.findByIsSystemRoleTrue();
            log.info("Total system roles in database: {}", systemRoles.size());
            for (Role role : systemRoles) {
                log.debug("  - {} ({} permissions)", role.getName(), role.getPermissions().size());
            }

            success = true;
        } catch (Exception e) {
            log.error("Error during role initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    /**
     * Print start banner for role initialization
     */
    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║                  ROLE INITIALIZER - START                          ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    /**
     * Print end banner for role initialization
     *
     * @param success whether the initialization was successful
     */
    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║              ✓ ROLE INITIALIZER - COMPLETED                        ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║              ✗ ROLE INITIALIZER - FAILED                           ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Update role permissions by adding new permissions while keeping existing ones
     *
     * @param role the role to update
     * @param newPermissions the expected set of permissions for this role
     */
    private void updateRolePermissions(Role role, Set<Permission> newPermissions) {
        // Add any new permissions that don't exist in the role
        for (Permission permission : newPermissions) {
            if (!role.getPermissions().contains(permission)) {
                role.addPermission(permission);
            }
        }
    }

    /**
     * Get SUPERADMIN permissions (ALL permissions)
     */
    private Set<Permission> getSuperAdminPermissions() {
        return new HashSet<>(permissionRepository.findByActiveTrue());
    }

    /**
     * The entities an administrator runs the system through, rather than works in.
     *
     * Deleting a customer or an invoice destroys history somebody may need to answer
     * for, which is why ADMIN is denied DELETE everywhere else. Deleting an account
     * or a role destroys no history — it withdraws access — and it is the one thing
     * an administrator must be able to do without waiting for a superadmin. Somebody
     * leaves the company on a Friday; the person who can turn off their login has to
     * be the person who is there.
     */
    private static final Set<String> ADMINISTRATION_ENTITIES = Set.of("USER", "ROLE", "PERMISSION");

    /**
     * Get ADMIN permissions (all except DELETE, which is still granted over
     * users, roles and permissions — see ADMINISTRATION_ENTITIES)
     */
    private Set<Permission> getAdminPermissions() {
        List<Permission> allPermissions = permissionRepository.findByActiveTrue();
        Set<Permission> adminPermissions = new HashSet<>();

        for (Permission permission : allPermissions) {
            boolean isDelete = permission.getAction() == PermissionAction.DELETE;
            boolean administration = ADMINISTRATION_ENTITIES.contains(permission.getEntity());
            if (!isDelete || administration) {
                adminPermissions.add(permission);
            }
        }

        return adminPermissions;
    }

    /**
     * Get USER permissions (CREATE and READ only)
     */
    private Set<Permission> getUserPermissions() {
        List<Permission> allPermissions = permissionRepository.findByActiveTrue();
        Set<Permission> userPermissions = new HashSet<>();

        for (Permission permission : allPermissions) {
            if (permission.getAction() == PermissionAction.CREATE ||
                permission.getAction() == PermissionAction.READ) {
                userPermissions.add(permission);
            }
        }

        return userPermissions;
    }

    /**
     * Get GUEST permissions (READ only)
     */
    private Set<Permission> getGuestPermissions() {
        List<Permission> allPermissions = permissionRepository.findByActiveTrue();
        Set<Permission> guestPermissions = new HashSet<>();

        for (Permission permission : allPermissions) {
            if (permission.getAction() == PermissionAction.READ) {
                guestPermissions.add(permission);
            }
        }

        return guestPermissions;
    }

    /**
     * Create SUPERADMIN role with ALL permissions
     * Permissions: CREATE, READ, UPDATE, DELETE on all entities
     */
    private Role createSuperAdminRole() {
        Set<Permission> allPermissions = getSuperAdminPermissions();

        Role superAdmin = Role.builder()
                .name("SUPERADMIN")
                .displayName("Super Administrator")
                .description("Full system access with all permissions. Can create, read, update, and delete all entities.")
                .active(true)
                .isSystemRole(true)
                .permissions(allPermissions)
                .build();

        log.debug("SUPERADMIN role created with {} permissions", allPermissions.size());
        return superAdmin;
    }

    /**
     * Create ADMIN role with all permissions EXCEPT DELETE
     * Permissions: CREATE, READ, UPDATE on all entities
     */
    private Role createAdminRole() {
        Set<Permission> adminPermissions = getAdminPermissions();

        Role admin = Role.builder()
                .name("ADMIN")
                .displayName("Administrator")
                .description("Administrative access with create, read, and update permissions. Cannot delete business records, but has full control of users, roles and permissions.")
                .active(true)
                .isSystemRole(true)
                .permissions(adminPermissions)
                .build();

        log.debug("ADMIN role created with {} permissions (excluded DELETE)", adminPermissions.size());
        return admin;
    }

    /**
     * Create USER role with CREATE and READ permissions only
     * Permissions: CREATE, READ on all entities (no UPDATE, no DELETE)
     */
    private Role createUserRole() {
        Set<Permission> userPermissions = getUserPermissions();

        Role user = Role.builder()
                .name("USER")
                .displayName("Standard User")
                .description("Standard user access with create and read permissions. Cannot update or delete entities.")
                .active(true)
                .isSystemRole(true)
                .permissions(userPermissions)
                .build();

        log.debug("USER role created with {} permissions (CREATE and READ only)", userPermissions.size());
        return user;
    }

    /**
     * Create GUEST role with READ permission only
     * Permissions: READ on all entities
     */
    private Role createGuestRole() {
        Set<Permission> guestPermissions = getGuestPermissions();

        Role guest = Role.builder()
                .name("GUEST")
                .displayName("Guest")
                .description("Read-only access. Can view all entities but cannot create, update, or delete.")
                .active(true)
                .isSystemRole(true)
                .permissions(guestPermissions)
                .build();

        log.debug("GUEST role created with {} permissions (READ only)", guestPermissions.size());
        return guest;
    }
}
