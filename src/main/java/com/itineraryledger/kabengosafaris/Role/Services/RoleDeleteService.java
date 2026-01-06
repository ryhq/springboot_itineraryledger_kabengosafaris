package com.itineraryledger.kabengosafaris.Role.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.Role.RoleRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * RoleDeleteService - Service for deleting roles
 *
 * This service handles:
 * - Single or batch deletion of roles by obfuscated IDs
 * - Validation preventing deletion of system roles
 * - Atomic operation: if any role in the list is a system role, no roles are deleted
 * - Response formatting with success message
 */
@Service
@Slf4j
@Transactional
public class RoleDeleteService {

    private final RoleRepository roleRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public RoleDeleteService(
        RoleRepository roleRepository,
        IdObfuscator idObfuscator
    ) {
        this.roleRepository = roleRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete roles by list of obfuscated IDs
     *
     * Validation: If any role in the list is a system role, no roles will be deleted
     * Returns success message
     *
     * @param idObfuscatedList List of obfuscated role IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteRoles(List<String> idObfuscatedList) {
        log.info("Deleting {} roles", idObfuscatedList.size());

        try {
            // Decode all obfuscated IDs
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteRolesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete roles",
                    "ROLES_DELETE_FAILED"
                )
            );
        }
    }

    private ResponseEntity<ApiResponse<?>> deleteRolesInternal(List<Long> ids) {
        // First, validate that no role in the list is a system role
        List<Long> systemRoleIds = new ArrayList<>();

        for (Long id : ids) {
            Role role = roleRepository.findById(id).orElse(null);

            if (role != null && Boolean.TRUE.equals(role.getIsSystemRole())) {
                systemRoleIds.add(id);
            }
        }

        // If any system roles found, reject entire operation
        if (!systemRoleIds.isEmpty()) {
            log.warn("Cannot delete: {} role(s) in the list are system roles", systemRoleIds.size());

            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    400,
                    "Cannot delete system roles. System roles cannot be deleted.",
                    "CANNOT_DELETE_SYSTEM_ROLES"
                )
            );
        }

        // Proceed with deletion
        int deletedCount = 0;
        for (Long id : ids) {
            try {
                Role role = roleRepository.findById(id).orElse(null);

                if (role == null) {
                    log.warn("Role not found: {}", id);
                    continue;
                }

                ((RoleDeleteService) AopContext.currentProxy()).deleteRole(id);
                deletedCount++;
                log.info("Role deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting role: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " role(s) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(action = "DELETE_ROLE", description = "Deleting role", entityType = "Role", entityIdParamName = "id")
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}
