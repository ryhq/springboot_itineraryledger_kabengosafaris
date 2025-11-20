package com.itineraryledger.kabengosafaris.User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.itineraryledger.kabengosafaris.Permission.Permission;
import com.itineraryledger.kabengosafaris.Role.Role;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data // This will generate getters, setters, toString, equals, and hashCode methods automatically
@AllArgsConstructor // This will generate an all-arguments constructor
@NoArgsConstructor // This will generate a no-arguments constructor
@Builder // This will enable the builder pattern for this class
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(nullable = false)
    private Boolean accountLocked;

    private int failedAttempt;

    // Tracks when the last failed login attempt occurred for counterResetHours logic

    private LocalDateTime lastFailedAttemptTime;

    private LocalDateTime accountLockedTime;

    private LocalDateTime passwordExpiryDate;

    @Lob
    @Column(length = 2000)
    private String bio;

    @Column(length = 2083)
    private String profilePictureUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Many-to-many relationship with Role
     * A user can have multiple roles, each role has multiple permissions
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.enabled == null) this.enabled = false;
        if (this.accountLocked == null) this.accountLocked = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add all role authorities from the new RBAC system
        roles.stream().filter(
            Role::getActive
        ).forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName())));

        // Add all permission authorities from all roles
        roles.stream().filter(
            Role::getActive
        ).flatMap(
            role -> role.getPermissions().stream()
        ).filter(
            Permission::getActive
        ).forEach(
            permission -> authorities.add(
                new SimpleGrantedAuthority("PERM_" + permission.getName())
            )
        );
        return authorities;
    }

    /**
     * Check if user has a specific permission
     *
     * @param permissionName name of the permission
     * @return true if user has the permission
     */
    public boolean hasPermission(String permissionName) {
        return roles.stream().filter(
            Role::getActive
        ).anyMatch(
            role -> role.hasPermission(permissionName)
        );
    }

    /**
     * Check if user has a specific action on a resource
     * Database-driven: action code must exist in permission_action_types table
     *
     * @param actionCode action code (e.g., "create", "read", "update", "delete")
     * @param resource resource/document type
     * @return true if user has the permission
     */
    public boolean hasPermission(String actionCode, String resource) {
        return roles.stream().filter(
            Role::getActive
        ).anyMatch(
            role -> role.hasPermission(actionCode, resource)
        );
    }

    /**
     * Check if user has a specific role
     *
     * @param roleName name of the role
     * @return true if user has the role
     */
    public boolean hasRole(String roleName) {
        return roles.stream().filter(
            Role::getActive
        ).anyMatch(
            role -> role.getName().equalsIgnoreCase(roleName)
        );
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Account doesn't have expiration date in current implementation
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (passwordExpiryDate == null) {
            return true;
        }
        return passwordExpiryDate.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
