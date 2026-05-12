package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A rule that collapses predictable automated mail into a single muted
 * summary row instead of cluttering the inbox. See §7 in
 * EMAIL_INBOX_API.md.
 *
 * matchField + matchPattern + matchMode encode a single substring/prefix
 * predicate. ANDing two rules isn't supported on purpose — define two
 * separate rules instead.
 */
@Entity
@Table(name = "email_mute_rules", indexes = {
    @Index(name = "idx_mute_rule_account_id", columnList = "email_account_id"),
    @Index(name = "idx_mute_rule_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MuteRule {

    public enum MatchField { FROM_ADDRESS, SUBJECT }
    public enum MatchMode { CONTAINS, STARTS_WITH, EQUALS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_account_id", nullable = false)
    private EmailAccount emailAccount;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_field", nullable = false, length = 20)
    private MatchField matchField;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_mode", nullable = false, length = 20)
    @Builder.Default
    private MatchMode matchMode = MatchMode.CONTAINS;

    @Column(name = "match_pattern", nullable = false, length = 400)
    private String matchPattern;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.isActive == null) this.isActive = true;
        if (this.matchMode == null) this.matchMode = MatchMode.CONTAINS;
    }
}
