package com.itineraryledger.kabengosafaris.Newsletter.Repository;

import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long> {
    Optional<NewsletterSubscription> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByStatus(SubscriptionStatus status);
}
