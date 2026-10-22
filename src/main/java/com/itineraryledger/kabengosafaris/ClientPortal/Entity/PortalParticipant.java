package com.itineraryledger.kabengosafaris.ClientPortal.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ParticipantRole;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One person who may open a journey, and the address their code is sent to.
 *
 * <p>Not an account. There is no password here and there never will be: a password is a
 * credential to steal, a reset flow to abuse, and the thing a client will have forgotten nine
 * months before their trip. What identifies them is that they can read an inbox we already hold
 * on the customer record.
 *
 * <p>The email is the key, so it is stored lowercase and is unique within the journey. A second
 * invitation to the same person is the same participant, not a second one with its own devices.
 *
 * <p>{@code policyVersion} records which privacy notice they were actually shown when they first
 * verified. "The policy as it stands today" is not an answer to what somebody consented to, and a
 * notice that can be edited in the panel is a notice that will be.
 */
@Entity
@Table(
    name = "portal_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_portal_participant", columnNames = {"journey_id", "email"}),
    indexes = @Index(name = "idx_portal_participant_email", columnList = "email"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private PortalJourney journey;

    /** Lowercase. The address a verification code is sent to, and nowhere else. */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 30)
    private ParticipantRole participantRole;

    /** The first time they proved they could read that inbox. Null means never. */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_participant_id")
    private PortalParticipant invitedBy;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "policy_version", length = 30)
    private String policyVersion;

    @Column(name = "policy_accepted_at")
    private LocalDateTime policyAcceptedAt;

    /**
     * Switched off rather than deleted, so the access log keeps pointing at somebody.
     *
     * <p>An inactive participant's devices are revoked with them; the record that they once read
     * the quotation is not a thing to erase, it is the thing somebody will need.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PortalDevice> devices = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (isActive == null) isActive = true;
        if (email != null) email = email.trim().toLowerCase();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (email != null) email = email.trim().toLowerCase();
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    /** Whether this person may propose, submit, accept or decline. Readers may not. */
    public boolean mayAct() {
        return Boolean.TRUE.equals(isActive)
            && participantRole != null
            && participantRole.mayAct();
    }
}
