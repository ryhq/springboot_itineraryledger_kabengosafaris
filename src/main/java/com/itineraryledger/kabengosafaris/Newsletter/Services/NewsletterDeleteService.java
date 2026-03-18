package com.itineraryledger.kabengosafaris.Newsletter.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Newsletter.Repository.NewsletterSubscriptionRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class NewsletterDeleteService {

    private final NewsletterSubscriptionRepository repository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public NewsletterDeleteService(NewsletterSubscriptionRepository repository, IdObfuscator idObfuscator) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> deleteSubscriptions(List<String> idObfuscatedList) {
        log.info("Deleting {} newsletter subscriptions", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            int deletedCount = 0;
            for (Long id : ids) {
                try {
                    if (repository.existsById(id)) {
                        ((NewsletterDeleteService) AopContext.currentProxy()).deleteSubscription(id);
                        deletedCount++;
                        log.info("Newsletter subscription deleted successfully: {}", id);
                    } else {
                        log.warn("Newsletter subscription not found: {}", id);
                    }
                } catch (Exception e) {
                    log.error("Error deleting newsletter subscription: {}", id, e);
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " subscription(s) deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("Error deleting newsletter subscriptions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete subscriptions", "SUBSCRIPTIONS_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_NEWSLETTER_SUBSCRIPTION", description = "Deleting newsletter subscription", entityType = "NewsletterSubscription", entityIdParamName = "id")
    public void deleteSubscription(Long id) {
        repository.deleteById(id);
    }
}
