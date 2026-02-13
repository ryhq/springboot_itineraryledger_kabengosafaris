package com.itineraryledger.kabengosafaris.Dashboard.Controller;

import com.itineraryledger.kabengosafaris.Dashboard.Service.DashboardService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard Controller
 * REST API endpoints for dashboard statistics and metrics
 */
@RestController
@RequestMapping("/api/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Get comprehensive dashboard statistics
     * Includes all metrics: quotes, invoices, customers, safaris, users, activities
     *
     * @return ResponseEntity with ApiResponse containing DashboardStatsDTO
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getDashboardStats() {
        log.info("GET /api/dashboard/stats - Fetching comprehensive dashboard statistics");
        return dashboardService.getDashboardStats();
    }

    /**
     * Get quote statistics only
     * Includes: total quotes, quotes by status, conversion rate, recent quotes
     *
     * @return ResponseEntity with ApiResponse containing quote statistics
     */
    @GetMapping("/stats/quotes")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_QUOTE')")
    public ResponseEntity<ApiResponse<?>> getQuoteStats() {
        log.info("GET /api/dashboard/stats/quotes - Fetching quote statistics");
        return dashboardService.getQuoteStats();
    }

    /**
     * Get invoice statistics only
     * Includes: total invoices, invoices by status, revenue metrics, recent invoices
     *
     * @return ResponseEntity with ApiResponse containing invoice statistics
     */
    @GetMapping("/stats/invoices")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getInvoiceStats() {
        log.info("GET /api/dashboard/stats/invoices - Fetching invoice statistics");
        return dashboardService.getInvoiceStats();
    }

    /**
     * Get customer statistics only
     * Includes: total customers, active/VIP customers, new customers, recent customers
     *
     * @return ResponseEntity with ApiResponse containing customer statistics
     */
    @GetMapping("/stats/customers")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> getCustomerStats() {
        log.info("GET /api/dashboard/stats/customers - Fetching customer statistics");
        return dashboardService.getCustomerStats();
    }

    /**
     * Get safari statistics only
     * Includes: total safaris, safaris by state, upcoming/ongoing safaris, recent safaris
     *
     * @return ResponseEntity with ApiResponse containing safari statistics
     */
    @GetMapping("/stats/safaris")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getSafariStats() {
        log.info("GET /api/dashboard/stats/safaris - Fetching safari statistics");
        return dashboardService.getSafariStats();
    }

    /**
     * Health check endpoint for dashboard API
     *
     * @return ResponseEntity with success message
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> health() {
        return ResponseEntity.ok(
                ApiResponse.success(200, "Dashboard API is healthy", null)
        );
    }
}
