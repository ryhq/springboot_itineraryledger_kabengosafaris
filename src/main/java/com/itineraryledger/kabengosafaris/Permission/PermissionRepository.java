package com.itineraryledger.kabengosafaris.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    /**
     * Find permission by exact name
     * Example: findByName("CREATE_USER")
     */
    Optional<Permission> findByName(String name);

    /**
     * Check if permission exists by name
     * Example: existsByName("CREATE_USER")
     */
    boolean existsByName(String name);

    /**
     * Find all permissions for a specific entity
     * Example: findByEntity("USER") returns CREATE_USER, READ_USER, UPDATE_USER, DELETE_USER
     */
    List<Permission> findByEntity(String entity);

    /**
     * Find all permissions for a specific action type
     * Example: findByAction(PermissionAction.CREATE) returns all CREATE permissions
     */
    List<Permission> findByAction(PermissionAction action);

    /**
     * Find permission by action and entity
     * Example: findByActionAndEntity(PermissionAction.CREATE, "USER") returns CREATE_USER
     */
    Optional<Permission> findByActionAndEntity(PermissionAction action, String entity);

    /**
     * Find all active permissions
     */
    List<Permission> findByActiveTrue();

    /**
     * Find all inactive permissions
     */
    List<Permission> findByActiveFalse();

    /**
     * Find active permissions by entity
     * Example: findByEntityAndActiveTrue("USER")
     */
    List<Permission> findByEntityAndActiveTrue(String entity);

    /**
     * Find permissions by entity that match pattern
     * Example: findByEntityLike("EMAIL") might match "EMAIL_ACCOUNT", "EMAIL_EVENT"
     */
    @Query("SELECT p FROM Permission p WHERE LOWER(p.entity) LIKE LOWER(CONCAT('%', :entity, '%'))")
    List<Permission> findByEntityLike(@Param("entity") String entity);

    /**
     * Find all permissions that match a name pattern
     * Example: findByNameLike("USER") returns CREATE_USER, READ_USER, etc.
     */
    @Query("SELECT p FROM Permission p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Permission> findByNameLike(@Param("name") String name);

    /**
     * Count permissions by entity
     * Example: countByEntity("USER") returns 4 (CREATE, READ, UPDATE, DELETE)
     */
    long countByEntity(String entity);

    /**
     * Count permissions by action type
     * Example: countByAction(PermissionAction.CREATE) returns count of all CREATE permissions
     */
    long countByAction(PermissionAction action);

    /**
     * Find permissions for multiple entities
     * Example: findByEntityIn(Set.of("USER", "ROLE", "EMAIL_ACCOUNT"))
     */
    @Query("SELECT p FROM Permission p WHERE p.entity IN :entities")
    List<Permission> findByEntityIn(@Param("entities") Set<String> entities);

    /**
     * Find all distinct entities that have permissions
     * Useful for UI dropdowns
     */
    @Query("SELECT DISTINCT p.entity FROM Permission p ORDER BY p.entity")
    List<String> findAllDistinctEntities();
}
