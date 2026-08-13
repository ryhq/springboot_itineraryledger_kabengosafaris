package com.itineraryledger.kabengosafaris.Initializers;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Permission.Permission;
import com.itineraryledger.kabengosafaris.Permission.PermissionRepository;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Repairs permissions that were seeded under a wrong name.
 *
 * A permission is only ever granted by name. If the catalogue holds a misspelling,
 * the endpoint asking for the correct spelling can never be satisfied by anybody —
 * not even SUPERADMIN, whose grant is "every permission that exists". That failure
 * is invisible from the roles screen, because the role looks fully granted; it only
 * shows up as a 403 on one feature.
 *
 * Two such faults existed, both fixed in the JSON catalogue as well:
 *
 * 1. entities.json carried `tACCOMMODATION_RATE`, so READ/UPDATE/DELETE of
 *    accommodation rates could not be granted while AccommodationRateController
 *    required exactly those. Renamed in place rather than recreated, so the
 *    role_permissions rows already pointing at them survive and every role that
 *    had the misspelt permission keeps working.
 * 2. A custom permission was literally named `PERM_READ_LOG`. Authorities are built
 *    as "PERM_" + name, so it granted `PERM_PERM_READ_LOG`, which nothing checks.
 *    READ_LOG already exists as a standard permission, so this one is deleted —
 *    unlinked from its roles first, as nothing else clears that join table.
 *
 * Runs BEFORE PermissionInitializer so the rename lands first and the corrected
 * names are then seen as already present, rather than being created a second time
 * and leaving the granted-but-misspelt copies behind as duplicates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class PermissionCatalogueRepairInitializer implements ApplicationRunner {

    private static final String WRONG_ENTITY = "tACCOMMODATION_RATE";
    private static final String RIGHT_ENTITY = "ACCOMMODATION_RATE";
    private static final String JUNK_PERMISSION = "PERM_READ_LOG";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            renameMisspeltEntity();
            deleteJunkPermission();
        } catch (Exception e) {
            // A repair must never stop the application from starting.
            log.error("Permission catalogue repair failed", e);
        }
    }

    private void renameMisspeltEntity() {
        List<Permission> misspelt = permissionRepository.findByEntity(WRONG_ENTITY);
        if (misspelt.isEmpty()) return;

        int renamed = 0;
        for (Permission permission : misspelt) {
            String corrected = permission.getName().replace(WRONG_ENTITY, RIGHT_ENTITY);

            /*
             * If the corrected name somehow already exists, renaming would break the
             * unique constraint. Leave the stray one alone and say so — a duplicate
             * is untidy, a failed startup is not.
             */
            if (permissionRepository.existsByName(corrected)) {
                log.warn("Cannot rename {} — {} already exists; leaving it", permission.getName(), corrected);
                continue;
            }

            log.info("Repairing permission {} -> {}", permission.getName(), corrected);
            permission.setName(corrected);
            permission.setEntity(RIGHT_ENTITY);
            permission.setDescription(permission.getDescription() == null
                ? null
                : permission.getDescription().replace(WRONG_ENTITY, RIGHT_ENTITY));
            permissionRepository.save(permission);
            renamed++;
        }

        if (renamed > 0) {
            log.info("Permission catalogue repair: renamed {} {} permission(s) to {}",
                renamed, WRONG_ENTITY, RIGHT_ENTITY);
        }
    }

    private void deleteJunkPermission() {
        Permission junk = permissionRepository.findByName(JUNK_PERMISSION).orElse(null);
        if (junk == null) return;

        // Unlink first: role_permissions has no cascade, so a delete would fail on the FK.
        List<Role> holders = roleRepository.findRolesByPermissionName(JUNK_PERMISSION);
        for (Role role : holders) {
            role.removePermission(junk);
            roleRepository.save(role);
        }

        permissionRepository.delete(junk);
        log.info("Permission catalogue repair: removed {} (granted PERM_{}, which nothing checks) from {} role(s)",
            JUNK_PERMISSION, JUNK_PERMISSION, holders.size());
    }
}
