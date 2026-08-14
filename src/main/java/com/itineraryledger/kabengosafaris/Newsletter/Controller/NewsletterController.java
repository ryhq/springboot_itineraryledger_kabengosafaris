package com.itineraryledger.kabengosafaris.Newsletter.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Newsletter.DTOs.UpdateNewsletterSubscriptionDTO;
import com.itineraryledger.kabengosafaris.Newsletter.Specifications.NewsletterFilter;
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
        /*
         * Every parameter the old signature took is still spelled the same on the wire —
         * @ModelAttribute binds them onto the filter — plus the multi-value forms
         * (statuses, sources, qualities).
         */
        @ModelAttribute NewsletterFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false) Integer pageNumber,
        @RequestParam(required = false) Integer pageSize,
        /*
         * page/size are what every other list in this API is paged by. pageNumber/pageSize
         * stay because the v1 panel sends them; whichever arrives wins, house names first.
         */
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/newsletter-subscriptions - Listing subscriptions with filters");
        return getService.listSubscriptions(
            filter,
            includeStats,
            page != null ? page : pageNumber,
            size != null ? size : pageSize,
            sortBy,
            sortDirection
        );
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_NEWSLETTER_SUBSCRIPTION')")
    public ResponseEntity<ApiResponse<?>> getSubscriptionById(
        @PathVariable String idObfuscated,
        // the list's filter and sort, so prev/next stays inside the set on screen
        @ModelAttribute NewsletterFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/newsletter-subscriptions/{} - Fetching subscription by ID", idObfuscated);
        return getService.getSubscriptionById(idObfuscated, filter, sortBy, sortDirection);
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
