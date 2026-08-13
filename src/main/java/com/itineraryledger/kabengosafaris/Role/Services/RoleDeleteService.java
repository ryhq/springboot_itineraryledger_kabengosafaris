package com.itineraryledger.kabengosafaris.Role.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Deleting roles.
 *
 * Reference-checked, per id, reporting what it refused and why — the house contract for
 * every delete in this API. It did not used to be: a role somebody still held failed on
 * the user_roles foreign key, the failure was logged and swallowed, and the caller got
 * "0 role(s) deleted successfully" with a 200. A success that deleted nothing is the
 * worst possible answer, because the next thing the reader does is assume the access is
 * gone.
 *
 * Two things are refused:
 *
 *   · A built-in role. SUPERADMIN, ADMIN, USER and GUEST are read-only: the initializer
 *     owns their definition and rewrites it on every startup, so any change here is either
 *     undone overnight or, for a delete, loses every assignment to it in the meantime.
 *   · A role somebody still holds. Deleting it would strip their access silently, and
 *     nothing would record that it had happened; switching the role off is the reversible
 *     way to suspend a group, and reassigning people first is the deliberate way.
 *
 * A mixed selection no longer fails as a whole. Deleting three unused roles and one that
 * is in use now deletes the three and says why the fourth was kept.
 */
@Service
@Slf4j
@Transactional
public class RoleDeleteService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public RoleDeleteService(
        RoleRepository roleRepository,
        UserRepository userRepository,
        IdObfuscator idObfuscator
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete roles by list of obfuscated IDs.
     *
     * @return {deletedCount, deletedIds, skipped:[{id, code, reason}]}
     */
    public ResponseEntity<ApiResponse<?>> deleteRoles(List<String> idObfuscatedList) {
        if (idObfuscatedList == null || idObfuscatedList.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No ids supplied", "NO_IDS"));
        }

        log.info("Deleting {} roles", idObfuscatedList.size());

        List<String> deletedIds = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();

        for (String obfuscated : idObfuscatedList) {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscated);
            } catch (Exception e) {
                skipped.add(reason(obfuscated, null, "Unreadable id"));
                continue;
            }

            Role role = roleRepository.findById(id).orElse(null);
            if (role == null) {
                skipped.add(reason(obfuscated, null, "No longer exists"));
                continue;
            }

            if (Boolean.TRUE.equals(role.getIsSystemRole())) {
                skipped.add(reason(obfuscated, role.getName(),
                    "A built-in role. These are read-only — switch off a custom role instead, "
                        + "or remove people from this one"));
                continue;
            }

            long holders = countHolders(id);
            if (holders > 0) {
                skipped.add(reason(obfuscated, role.getName(),
                    holders + (holders == 1 ? " person still holds it" : " people still hold it")
                        + " — reassign them first, or switch the role off to suspend it"));
                continue;
            }

            try {
                /*
                 * The role_permissions rows have no cascade, so clearing the set first is
                 * what makes the delete land instead of failing on a foreign key.
                 */
                role.getPermissions().clear();
                roleRepository.save(role);

                // through the proxy, so the audit annotation actually fires
                ((RoleDeleteService) AopContext.currentProxy()).deleteRole(id);
                deletedIds.add(obfuscated);
                log.info("Role deleted: {}", role.getName());
            } catch (Exception e) {
                log.error("Error deleting role {}", role.getName(), e);
                skipped.add(reason(obfuscated, role.getName(),
                    "Still referenced elsewhere — switch it off instead"));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("deletedCount", deletedIds.size());
        data.put("deletedIds", deletedIds);
        data.put("skipped", skipped);

        String message = deletedIds.size() + " role" + (deletedIds.size() == 1 ? "" : "s") + " deleted"
            + (skipped.isEmpty() ? "" : ", " + skipped.size() + " kept");

        return ResponseEntity.ok(ApiResponse.success(200, message, data));
    }

    /**
     * How many people hold this role.
     *
     * A failure here must not read as "nobody holds it" — that is how a delete gets
     * through and silently strips somebody's access — so an error counts as one holder
     * and the delete is refused.
     */
    private long countHolders(Long roleId) {
        try {
            List<Object[]> rows = userRepository.countUsersByRoleIds(List.of(roleId));
            if (rows.isEmpty()) return 0;
            Object count = rows.get(0)[1];
            return count instanceof Number number ? number.longValue() : 1;
        } catch (Exception e) {
            log.error("Could not count holders of role {}; refusing the delete", roleId, e);
            return 1;
        }
    }

    private Map<String, String> reason(String id, String code, String reason) {
        Map<String, String> entry = new HashMap<>();
        entry.put("id", id);
        if (code != null) entry.put("code", code);
        entry.put("reason", reason);
        return entry;
    }

    @AuditLogAnnotation(action = "DELETE_ROLE", description = "Deleting role", entityType = "Role", entityIdParamName = "id")
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    /** For the record page, so the ⋯ menu can explain itself before anything is clicked. */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> checkDeletable(String obfuscated) {
        try {
            Long id = idObfuscator.decodeId(obfuscated);
            Role role = roleRepository.findById(id).orElse(null);
            if (role == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Role not found", "ROLE_NOT_FOUND"));
            }

            long holders = countHolders(id);
            boolean system = Boolean.TRUE.equals(role.getIsSystemRole());

            Map<String, Object> data = new HashMap<>();
            data.put("deletable", !system && holders == 0);
            data.put("isSystemRole", system);
            data.put("holders", holders);
            return ResponseEntity.ok(ApiResponse.success(200, "Checked", data));
        } catch (Exception e) {
            log.error("Error checking whether role {} can be deleted", obfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to check the role", "ROLE_CHECK_FAILED"));
        }
    }
}
