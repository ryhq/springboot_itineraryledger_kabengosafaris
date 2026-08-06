package com.itineraryledger.kabengosafaris.Newsletter.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Newsletter.DTOs.NewsletterSubscriptionDTO;
import com.itineraryledger.kabengosafaris.Newsletter.DTOs.UpdateNewsletterSubscriptionDTO;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import com.itineraryledger.kabengosafaris.Newsletter.Repository.NewsletterSubscriptionRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class NewsletterUpdateService {

    private final NewsletterSubscriptionRepository repository;
    private final IdObfuscator idObfuscator;
    private final NewsletterGetService getService;

    @Autowired
    public NewsletterUpdateService(
        NewsletterSubscriptionRepository repository,
        IdObfuscator idObfuscator,
        NewsletterGetService getService
    ) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
        this.getService = getService;
    }

    @AuditLogAnnotation(action = "UPDATE_NEWSLETTER_SUBSCRIPTION", description = "Updating newsletter subscription", entityType = "NewsletterSubscription", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateSubscription(String idObfuscated, UpdateNewsletterSubscriptionDTO updateDTO) {
        log.info("Updating newsletter subscription with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode subscription ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid subscription ID", "INVALID_SUBSCRIPTION_ID")
                );
            }

            NewsletterSubscription subscription = repository.findById(id).orElse(null);
            if (subscription == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Subscription not found", "SUBSCRIPTION_NOT_FOUND")
                );
            }

            if (updateDTO.getName() != null) subscription.setName(updateDTO.getName());
            if (updateDTO.getPreferredLocale() != null) subscription.setPreferredLocale(updateDTO.getPreferredLocale());

            if (updateDTO.getStatus() != null) {
                SubscriptionStatus oldStatus = subscription.getStatus();
                // the status arrives as a String so a blank can CLEAR it; parse it once
                SubscriptionStatus requestedStatus = updateDTO.getStatus().isBlank()
                    ? null
                    : SubscriptionStatus.valueOf(updateDTO.getStatus().trim());
                subscription.setStatus(requestedStatus);

                if (requestedStatus == SubscriptionStatus.UNSUBSCRIBED && oldStatus != SubscriptionStatus.UNSUBSCRIBED) {
                    subscription.setUnsubscribedAt(LocalDateTime.now());
                } else if (requestedStatus == SubscriptionStatus.ACTIVE && oldStatus == SubscriptionStatus.UNSUBSCRIBED) {
                    subscription.setUnsubscribedAt(null);
                }
            }

            subscription = repository.save(subscription);

            NewsletterSubscriptionDTO dto = getService.convertToDTO(subscription);

            log.info("Newsletter subscription updated successfully: {}", subscription.getEmail());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Subscription updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update subscription", "SUBSCRIPTION_UPDATE_FAILED")
            );
        }
    }
}
