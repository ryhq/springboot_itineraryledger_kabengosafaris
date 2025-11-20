package com.itineraryledger.kabengosafaris.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * Find permission by exact name
     */
    Optional<Permission> findByName(String name);

    /**
     * Check if permission exists by name
     */
    boolean existsByName(String name);

    /**
     * Find all permissions for a specific category
     */
    List<Permission> findByCategory(String category);

    /**
     * Find all permissions for a specific action type
     */
    List<Permission> findByActionType(PermissionActionType actionType);

    /**
     * Find all permissions for a specific resource
     */
    List<Permission> findByResource(String resource);

    /**
     * Find permissions by action type and resource
     */
    Optional<Permission> findByActionTypeAndResource(PermissionActionType actionType, String resource);

    /**
     * Find all active permissions
     */
    List<Permission> findByActiveTrue();

    /**
     * Find all inactive permissions
     */
    List<Permission> findByActiveFalse();

    /**
     * Find permissions by category and action type
     */
    List<Permission> findByCategoryAndActionType(String category, PermissionActionType actionType);

    /**
     * Find permissions by resource that match pattern
     */
    @Query("SELECT p FROM Permission p WHERE LOWER(p.resource) LIKE LOWER(CONCAT('%', :resource, '%'))")
    List<Permission> findByResourceLike(@Param("resource") String resource);

    /**
     * Find all permissions that match a name pattern
     */
    @Query("SELECT p FROM Permission p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Permission> findByNameLike(@Param("name") String name);

    /**
     * Count permissions by category
     */
    long countByCategory(String category);

    /**
     * Count permissions by action type
     */
    long countByActionType(PermissionActionType actionType);

    /**
     * Find permissions for multiple resources
     */
    @Query("SELECT p FROM Permission p WHERE p.resource IN :resources")
    List<Permission> findByResourceIn(@Param("resources") Set<String> resources);
}
