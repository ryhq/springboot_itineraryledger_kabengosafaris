package com.itineraryledger.kabengosafaris.Dashboard.Controller;

import com.itineraryledger.kabengosafaris.Dashboard.Service.DashboardService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Dashboard Controller — aggregated statistics, trends, leaderboards, alerts.
 */
@RestController
@RequestMapping("/api/dashboard")
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // -----------------------------------------------------------------------
    // Existing overview endpoints
    // -----------------------------------------------------------------------

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getDashboardStats() {
        log.info("GET /api/dashboard/stats");
        return dashboardService.getDashboardStats();
    }

    @GetMapping("/stats/quotes")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_QUOTE')")
    public ResponseEntity<ApiResponse<?>> getQuoteStats() {
        return dashboardService.getQuoteStats();
    }

    @GetMapping("/stats/invoices")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getInvoiceStats() {
        return dashboardService.getInvoiceStats();
    }

    @GetMapping("/stats/customers")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_CUSTOMER')")
    public ResponseEntity<ApiResponse<?>> getCustomerStats() {
        return dashboardService.getCustomerStats();
    }

    @GetMapping("/stats/safaris")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getSafariStats() {
        return dashboardService.getSafariStats();
    }

    // -----------------------------------------------------------------------
    // Trends (time-series)
    // -----------------------------------------------------------------------

    @GetMapping("/trends/revenue")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getRevenueTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String period) {
        return dashboardService.getRevenueTrend(from, to, period);
    }

    @GetMapping("/trends/bookings")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getBookingsTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String period) {
        return dashboardService.getBookingsTrend(from, to, period);
    }

    // -----------------------------------------------------------------------
    // Leaderboards
    // -----------------------------------------------------------------------

    @GetMapping("/top/parks")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getTopParks(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopParks(limit);
    }

    @GetMapping("/top/activities")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getTopActivities(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopActivities(limit);
    }

    @GetMapping("/top/accommodations")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getTopAccommodations(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopAccommodations(limit);
    }

    @GetMapping("/top/customers")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getTopCustomers(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopCustomers(limit);
    }

    // -----------------------------------------------------------------------
    // Pipeline, payments, alerts
    // -----------------------------------------------------------------------

    @GetMapping("/pipeline")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_SAFARI')")
    public ResponseEntity<ApiResponse<?>> getSafariPipeline() {
        return dashboardService.getSafariPipeline();
    }

    @GetMapping("/stats/payments")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_INVOICE')")
    public ResponseEntity<ApiResponse<?>> getPaymentStats() {
        return dashboardService.getPaymentStats();
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD')")
    public ResponseEntity<ApiResponse<?>> getAlerts() {
        return dashboardService.getAlerts();
    }

    // -----------------------------------------------------------------------
    // Expense / money-out aggregations
    // -----------------------------------------------------------------------

    @GetMapping("/trends/expenses")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getExpenseTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String period) {
        return dashboardService.getExpenseTrend(from, to, period);
    }

    @GetMapping("/stats/expenses")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getExpenseStats() {
        return dashboardService.getExpenseStats();
    }

    @GetMapping("/top/vendors")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getTopVendors(@RequestParam(defaultValue = "5") int limit) {
        return dashboardService.getTopVendors(limit);
    }

    /**
     * Per-safari profit & loss. Pass {@code safariId} (obfuscated) to scope
     * to one safari, or omit to receive an array of all safaris that have
     * either an invoice or an expense linked to them.
     */
    @GetMapping("/safari-pnl")
    @PreAuthorize("hasAuthority('PERM_VIEW_DASHBOARD') or hasAuthority('PERM_READ_EXPENSE')")
    public ResponseEntity<ApiResponse<?>> getSafariPnL(
            @RequestParam(required = false) String safariId) {
        return dashboardService.getSafariPnL(safariId);
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> health() {
        return ResponseEntity.ok(ApiResponse.success(200, "Dashboard API is healthy", null));
    }
}
