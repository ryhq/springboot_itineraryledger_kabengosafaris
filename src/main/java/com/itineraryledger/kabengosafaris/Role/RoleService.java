package com.itineraryledger.kabengosafaris.Role;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Permission.Permission;
import com.itineraryledger.kabengosafaris.Permission.PermissionRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RoleService - Manages role definitions and role-to-permission assignments
 *
 * This service handles all CRUD operations for roles (ADMIN, MANAGER, STAFF, etc.).
 * Roles group multiple permissions together for easier assignment to users.
 *
 * Key Responsibilities:
 * - Create roles
 * - Manage role-permission assignments
 * - Query roles and their permissions
 * - Deactivate roles
 * - Initialize system roles on application startup
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Create a new role
     * Role names must be unique and cannot be reserved system role names
     *
     * @param name unique role identifier (e.g., "booking_manager")
     * @param displayName human-readable role name (e.g., "Booking Manager")
     * @param description detailed description of role purpose
     * @return the created Role (initially with no permissions)
     * @throws IllegalArgumentException if role name already exists
     */
    @Transactional
    @AuditLogAnnotation(action = "CREATE_ROLE", entityType = "Role", entityIdParamName = "", description = "Create a new role")
    public Role createRole(String name, String displayName, String description) {
        if (roleRepository.existsByName(name)) {
            log.warn("Role already exists: {}", name);
            throw new IllegalArgumentException("Role name already exists: " + name);
        }

        Role role = Role.builder()
            .name(name.toLowerCase())
            .displayName(displayName)
            .description(description)
            .active(true)
            .isSystemRole(false)
            .build();

        Role saved = roleRepository.save(role);
        log.info("Created role: name={}, displayName={}", name, displayName);
        return saved;
    }

    /**
     * Add a permission to a role
     *
     * @param roleId role ID to add permission to
     * @param permissionId permission ID to add
     * @throws IllegalArgumentException if role or permission not found
     */
    @Transactional
    @AuditLogAnnotation(action = "ADD_PERMISSION_TO_ROLE", entityType = "Role", entityIdParamName = "roleId", description = "Add permission to role")
    public void addPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> {
                log.warn("Role not found: {}", roleId);
                return new IllegalArgumentException("Role not found with ID: " + roleId);
            });

        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> {
                log.warn("Permission not found: {}", permissionId);
                return new IllegalArgumentException("Permission not found with ID: " + permissionId);
            });

        if (role.getPermissions().contains(permission)) {
            log.debug("Permission already assigned to role: role={}, permission={}", roleId, permissionId);
            return;
        }

        role.addPermission(permission);
        roleRepository.save(role);
        log.info("Added permission to role: role={}, permission={}", role.getName(), permission.getName());
    }

    /**
     * Remove a permission from a role
     *
     * @param roleId role ID to remove permission from
     * @param permissionId permission ID to remove
     */
    @Transactional
    @AuditLogAnnotation(action = "REMOVE_PERMISSION_FROM_ROLE", entityType = "Role", entityIdParamName = "roleId", description = "Remove permission from role")
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> {
                log.warn("Role not found: {}", roleId);
                return new IllegalArgumentException("Role not found with ID: " + roleId);
            });

        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> {
                log.warn("Permission not found: {}", permissionId);
                return new IllegalArgumentException("Permission not found with ID: " + permissionId);
            });

        role.removePermission(permission);
        roleRepository.save(role);
        log.info("Removed permission from role: role={}, permission={}", role.getName(), permission.getName());
    }

    /**
     * Get role with all its permissions
     *
     * @param roleId role ID to retrieve
     * @return Role with eager-loaded permissions
     */
    @Transactional(readOnly = true)
    public Role getRoleWithPermissions(Long roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> {
                log.warn("Role not found: {}", roleId);
                return new IllegalArgumentException("Role not found with ID: " + roleId);
            });
    }

    /**
     * Get role by name
     *
     * @param roleName role name
     * @return Role if found
     */
    @Transactional(readOnly = true)
    public Role getRoleByName(String roleName) {
        return roleRepository.findByName(roleName)
            .orElseThrow(() -> {
                log.warn("Role not found: {}", roleName);
                return new IllegalArgumentException("Role not found: " + roleName);
            });
    }

    /**
     * Get all active roles
     *
     * @return list of active roles
     */
    @Transactional(readOnly = true)
    public List<Role> getAllActiveRoles() {
        return roleRepository.findByActiveTrue();
    }

    /**
     * Get all active system roles
     *
     * @return list of active system roles
     */
    @Transactional(readOnly = true)
    public List<Role> getActiveSystemRoles() {
        return roleRepository.findActiveSystemRoles();
    }

    /**
     * Deactivate a role (soft delete)
     * System roles cannot be deactivated
     *
     * @param id role ID to deactivate
     * @return the deactivated Role
     * @throws IllegalArgumentException if trying to deactivate system role
     */
    @Transactional
    @AuditLogAnnotation(action = "DEACTIVATE_ROLE", entityType = "Role", entityIdParamName = "id", description = "Deactivate a role")
    public Role deactivateRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        if (role.getIsSystemRole()) {
            log.warn("Attempted to deactivate system role: {}", role.getName());
            throw new IllegalArgumentException("Cannot deactivate system roles");
        }

        role.setActive(false);
        Role updated = roleRepository.save(role);
        log.info("Deactivated role: name={}", role.getName());
        return updated;
    }

    /**
     * Reactivate a role
     *
     * @param id role ID to reactivate
     * @return the reactivated Role
     */
    @Transactional
    @AuditLogAnnotation(action = "REACTIVATE_ROLE", entityType = "Role", entityIdParamName = "id", description = "Reactivate a role")
    public Role reactivateRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + id));

        role.setActive(true);
        Role updated = roleRepository.save(role);
        log.info("Reactivated role: name={}", role.getName());
        return updated;
    }

    /**
     * Get all permissions for a role
     *
     * @param roleId role ID
     * @return set of permissions assigned to role
     */
    @Transactional(readOnly = true)
    public Set<Permission> getRolePermissions(Long roleId) {
        Role role = getRoleWithPermissions(roleId);
        return role.getPermissions();
    }

    /**
     * Initialize system roles on application startup
     * Creates ADMIN and USER roles if they don't exist
     */
    @Transactional
    public void initializeSystemRoles() {
        String[][] systemRoles = {
            {"admin", "Administrator", "Full system access - create, read, update, delete all resources"},
            {"user", "User", "Basic user access - read own data and create new items"}
        };

        for (String[] roleData : systemRoles) {
            String name = roleData[0];

            if (!roleRepository.existsByName(name)) {
                Role role = Role.builder()
                    .name(name)
                    .displayName(roleData[1])
                    .description(roleData[2])
                    .active(true)
                    .isSystemRole(true)
                    .build();

                roleRepository.save(role);
                log.info("Created system role: name={}, displayName={}", name, roleData[1]);
            }
        }

        log.info("System roles initialization complete");
    }

    /**
     * Check if role exists
     *
     * @param roleName role name
     * @return true if role exists
     */
    public boolean roleExists(String roleName) {
        return roleRepository.existsByName(roleName);
    }

    /**
     * Get total count of active roles
     *
     * @return count of active roles
     */
    public long countActiveRoles() {
        return roleRepository.countByActive(true);
    }
}
