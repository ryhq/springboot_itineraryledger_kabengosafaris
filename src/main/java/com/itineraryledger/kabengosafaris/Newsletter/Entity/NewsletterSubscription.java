package com.itineraryledger.kabengosafaris.Newsletter.Entity;

import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscriptions", indexes = {
    @Index(name = "idx_newsletter_email", columnList = "email", unique = true),
    @Index(name = "idx_newsletter_status", columnList = "status"),
    @Index(name = "idx_newsletter_locale", columnList = "preferredLocale")
})
public class NewsletterSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(length = 10, nullable = false)
    private String preferredLocale = "en";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String source = "WEBSITE";

    @CreationTimestamp
    private LocalDateTime subscribedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime unsubscribedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPreferredLocale() { return preferredLocale; }
    public void setPreferredLocale(String preferredLocale) { this.preferredLocale = preferredLocale; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(LocalDateTime subscribedAt) { this.subscribedAt = subscribedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getUnsubscribedAt() { return unsubscribedAt; }
    public void setUnsubscribedAt(LocalDateTime unsubscribedAt) { this.unsubscribedAt = unsubscribedAt; }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }
}
