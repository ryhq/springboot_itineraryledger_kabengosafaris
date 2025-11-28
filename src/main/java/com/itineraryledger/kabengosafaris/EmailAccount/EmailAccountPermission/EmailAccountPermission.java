package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountPermission;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Role.Role;
import com.itineraryledger.kabengosafaris.User.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailAccountPermission - Maps which users/roles can use specific email accounts
 *
 * This entity allows granular control over who can send emails from which accounts
 * Examples:
 * - Only "Finance" role can send from "finance@company.com"
 * - Only specific admin users can send from "noreply@company.com"
 * - All users in "Booking Manager" role can send from "bookings@company.com"
 */

@Entity
@Table(name = "email_account_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAccountPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The email account that access is being granted for
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    /**
     * The user granted access (nullable - if null, then role-based permission)
     * Specific user can use this email account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * The role granted access (nullable - if null, then user-specific permission)
     * All users with this role can use this email account
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = true)
    private Role role;

    /**
     * Whether this permission is active/enabled
     * If false deny access
     */
    @Column(nullable = false)
    private Boolean enabled;

    /**
     * Maximum emails per day this user/role can send using this config
     * 0 = unlimited
     */
    @Column(nullable = false)
    private Integer maxEmailsPerDay;

    /**
     * Description of why this access was granted (audit trail)
     */
    @Lob
    @Column(length = 500)
    private String grantReason;

    /**
     * User who granted this permission (audit trail)
     */
    private String grantedBy;

    /**
     * Expiration date for this permission (optional)
     * null means permission never expires
     */
    private LocalDateTime expiresAt;

    /**
     * When this permission was created
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this permission was last updated
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.enabled == null) this.enabled = true;
        if (this.maxEmailsPerDay == null) this.maxEmailsPerDay = 0;
    }

    /**
     * Check if this permission is still valid
     */
    public boolean isValid() {
        if (!enabled) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }

    /**
     * Check if this permission grants access for a specific configuration
     */
    public boolean grantsAccessTo(EmailAccount emailAccount) {
        return this.emailAccount.getId().equals(emailAccount.getId()) && isValid();
    }

    @Override
    public String toString() {
        String target = user != null ? "User: " + user.getUsername() : "Role: " + role.getName();
        return "EmailConfigurationPermission{" +
                "id=" + id +
                ", emailAccount='" + emailAccount.getEmail() + '\'' +
                ", " + target +
                ", enabled=" + enabled +
                ", expiresAt=" + expiresAt +
                '}';
    }
    
}
