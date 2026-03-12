package com.itineraryledger.kabengosafaris.ContactMessage.Entity;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages", indexes = {
    @Index(name = "idx_contact_code", columnList = "code"),
    @Index(name = "idx_contact_email", columnList = "email"),
    @Index(name = "idx_contact_status", columnList = "status"),
    @Index(name = "idx_contact_created_at", columnList = "created_at"),
    @Index(name = "idx_contact_customer_id", columnList = "customer_id")
},
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_contact_code", columnNames = {"code"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContactMessageStatus status = ContactMessageStatus.NEW;

    @Column(length = 50)
    @Builder.Default
    private String source = "WEBSITE";

    @Column(length = 10)
    @Builder.Default
    private String preferredLocale = "en";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private LocalDateTime respondedAt;
}
