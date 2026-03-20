package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ResendWebhookEventRepository extends JpaRepository<ResendWebhookEvent, Long>, JpaSpecificationExecutor<ResendWebhookEvent> {

    boolean existsBySvixId(String svixId);

    List<ResendWebhookEvent> findByEmailId(String emailId);

    // Circular navigation: find the event received just before this one (newer in desc order)
    Optional<ResendWebhookEvent> findFirstByReceivedAtGreaterThanOrderByReceivedAtAsc(LocalDateTime receivedAt);

    // Circular navigation: find the event received just after this one (older in desc order)
    Optional<ResendWebhookEvent> findFirstByReceivedAtLessThanOrderByReceivedAtDesc(LocalDateTime receivedAt);

    // Retention cleanup: delete events older than cutoff date
    long deleteByReceivedAtBefore(LocalDateTime cutoff);
}
