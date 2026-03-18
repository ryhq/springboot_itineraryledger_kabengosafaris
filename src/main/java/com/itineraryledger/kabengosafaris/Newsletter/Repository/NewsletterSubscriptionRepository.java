package com.itineraryledger.kabengosafaris.Newsletter.Repository;

import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NewsletterSubscriptionRepository extends JpaRepository<NewsletterSubscription, Long>, JpaSpecificationExecutor<NewsletterSubscription> {
    Optional<NewsletterSubscription> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByStatus(SubscriptionStatus status);

    @Query("SELECT n.id FROM NewsletterSubscription n WHERE n.id > :currentId ORDER BY n.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT n.id FROM NewsletterSubscription n WHERE n.id < :currentId ORDER BY n.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT n.id FROM NewsletterSubscription n ORDER BY n.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT n.id FROM NewsletterSubscription n ORDER BY n.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
