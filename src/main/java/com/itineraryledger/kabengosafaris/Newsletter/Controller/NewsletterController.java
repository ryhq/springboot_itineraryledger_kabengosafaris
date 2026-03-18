package com.itineraryledger.kabengosafaris.Newsletter.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Newsletter.DTOs.UpdateNewsletterSubscriptionDTO;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import com.itineraryledger.kabengosafaris.Newsletter.Services.NewsletterDeleteService;
import com.itineraryledger.kabengosafaris.Newsletter.Services.NewsletterGetService;
import com.itineraryledger.kabengosafaris.Newsletter.Services.NewsletterUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/newsletter-subscriptions")
@Slf4j
public class NewsletterController {

    private final NewsletterGetService getService;
    private final NewsletterUpdateService updateService;
    private final NewsletterDeleteService deleteService;

    @Autowired
    public NewsletterController(
        NewsletterGetService getService,
        NewsletterUpdateService updateService,
        NewsletterDeleteService deleteService
    ) {
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_NEWSLETTER_SUBSCRIPTION')")
    public ResponseEntity<ApiResponse<?>> listSubscriptions(
        @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
        @RequestParam(required = false, defaultValue = "20") Integer pageSize,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestParam(required = false) SubscriptionStatus status,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String source,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime subscribedAfter,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime subscribedBefore,
        @RequestParam(required = false) String keyword
    ) {
        log.info("GET /api/newsletter-subscriptions - Listing subscriptions with filters");
        return getService.listSubscriptions(
            pageNumber, pageSize, sortBy, sortDirection,
            status, email, name, source, subscribedAfter, subscribedBefore, keyword
        );
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_NEWSLETTER_SUBSCRIPTION')")
    public ResponseEntity<ApiResponse<?>> getSubscriptionById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/newsletter-subscriptions/{} - Fetching subscription by ID", idObfuscated);
        return getService.getSubscriptionById(idObfuscated);
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_NEWSLETTER_SUBSCRIPTION')")
    public ResponseEntity<ApiResponse<?>> updateSubscription(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateNewsletterSubscriptionDTO updateDTO
    ) {
        log.info("PUT /api/newsletter-subscriptions/{} - Updating subscription", idObfuscated);
        return updateService.updateSubscription(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_NEWSLETTER_SUBSCRIPTION')")
    public ResponseEntity<ApiResponse<?>> deleteSubscriptions(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/newsletter-subscriptions - Deleting {} subscriptions", idObfuscatedList.size());
        return deleteService.deleteSubscriptions(idObfuscatedList);
    }
}
