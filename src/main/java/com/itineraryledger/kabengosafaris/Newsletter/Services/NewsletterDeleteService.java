package com.itineraryledger.kabengosafaris.Newsletter.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /** One skipped row, named the way the caller can show it. */
    private Map<String, Object> skip(String idObfuscated, String reason) {
        Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", idObfuscated);
        row.put("reason", reason);
        return row;
    }

    public ResponseEntity<ApiResponse<?>> deleteSubscriptions(List<String> idObfuscatedList) {
        log.info("Deleting {} newsletter subscriptions", idObfuscatedList.size());

        try {
            /*
             * Per-row outcomes, because this used to answer "200, none deleted" in the same
             * words as "200, all deleted" — an id that would not decode, a row already
             * gone, or a row that threw was logged here and never mentioned to the caller.
             */
            int deletedCount = 0;
            List<String> deletedIds = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();

            for (String idObfuscated : idObfuscatedList) {
                Long id;
                try {
                    id = idObfuscator.decodeId(idObfuscated);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                    skipped.add(skip(idObfuscated, "Not a valid subscriber reference"));
                    continue;
                }

                try {
                    if (repository.existsById(id)) {
                        ((NewsletterDeleteService) AopContext.currentProxy()).deleteSubscription(id);
                        deletedCount++;
                        deletedIds.add(idObfuscated);
                        log.info("Newsletter subscription deleted successfully: {}", id);
                    } else {
                        log.warn("Newsletter subscription not found: {}", id);
                        skipped.add(skip(idObfuscated, "No such subscriber — it may already have been deleted"));
                    }
                } catch (Exception e) {
                    log.error("Error deleting newsletter subscription: {}", id, e);
                    skipped.add(skip(idObfuscated, e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
                }
            }

            Map<String, Object> report = new java.util.HashMap<>();
            report.put("deletedCount", deletedCount);
            report.put("deletedIds", deletedIds);
            report.put("skipped", skipped);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " subscription(s) deleted successfully", report)
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
