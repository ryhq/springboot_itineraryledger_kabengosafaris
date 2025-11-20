package com.itineraryledger.kabengosafaris.Role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find role by exact name
     */
    Optional<Role> findByName(String name);

    /**
     * Check if role exists by name
     */
    boolean existsByName(String name);

    /**
     * Find all active roles
     */
    List<Role> findByActiveTrue();

    /**
     * Find all inactive roles
     */
    List<Role> findByActiveFalse();

    /**
     * Find all system roles (cannot be deleted)
     */
    List<Role> findByIsSystemRoleTrue();

    /**
     * Find all custom roles (can be deleted)
     */
    List<Role> findByIsSystemRoleFalse();

    /**
     * Find roles by name pattern (case-insensitive)
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Role> findByNameLike(@Param("name") String name);

    /**
     * Find roles by display name pattern
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.displayName) LIKE LOWER(CONCAT('%', :displayName, '%'))")
    List<Role> findByDisplayNameLike(@Param("displayName") String displayName);

    /**
     * Find active and system roles
     */
    @Query("SELECT r FROM Role r WHERE r.active = true AND r.isSystemRole = true")
    List<Role> findActiveSystemRoles();

    /**
     * Find roles that have a specific permission
     */
    @Query("SELECT DISTINCT r FROM Role r JOIN r.permissions p WHERE p.name = :permissionName")
    List<Role> findRolesByPermissionName(@Param("permissionName") String permissionName);

    /**
     * Find roles that have all specified permissions
     */
    @Query("SELECT r FROM Role r WHERE r.id IN " +
            "(SELECT r2.id FROM Role r2 JOIN r2.permissions p WHERE p.name IN :permissionNames " +
            "GROUP BY r2.id HAVING COUNT(DISTINCT p.name) = :permissionCount)")
    List<Role> findRolesByAllPermissions(
            @Param("permissionNames") Set<String> permissionNames,
            @Param("permissionCount") long permissionCount
    );

    /**
     * Count roles by active status
     */
    long countByActive(Boolean active);

    /**
     * Find all roles with their permissions (eager loaded)
     */
    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.active = true")
    List<Role> findAllActiveRolesWithPermissions();

    /**
     * Find multiple roles by IDs
     */
    List<Role> findByIdIn(Set<Long> ids);
}
