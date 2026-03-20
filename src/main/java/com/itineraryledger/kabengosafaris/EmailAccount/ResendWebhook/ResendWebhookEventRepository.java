package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResendWebhookEventRepository extends JpaRepository<ResendWebhookEvent, Long> {

    boolean existsBySvixId(String svixId);

    List<ResendWebhookEvent> findByEmailId(String emailId);
}
