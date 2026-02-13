package com.itineraryledger.kabengosafaris.Dashboard.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Statistics DTO
 * Comprehensive dashboard statistics for the safari management system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // ========================
    // QUOTE METRICS
    // ========================
    private Long totalQuotes;
    private Long draftQuotes;
    private Long sentQuotes;
    private Long acceptedQuotes;
    private Long convertedQuotes;
    private Long expiredQuotes;
    private Double quoteConversionRate; // (accepted + converted) / total sent
    private Map<String, Long> quotesByStatus;

    // ========================
    // INVOICE METRICS
    // ========================
    private Long totalInvoices;
    private Long draftInvoices;
    private Long sentInvoices;
    private Long paidInvoices;
    private Long overdueInvoices;
    private Long partiallyPaidInvoices;
    private Map<String, Long> invoicesByStatus;

    // Revenue metrics
    private List<RevenueByCurrency> totalRevenue;
    private List<RevenueByCurrency> pendingRevenue;
    private List<RevenueByCurrency> overdueRevenue;

    // ========================
    // CUSTOMER METRICS
    // ========================
    private Long totalCustomers;
    private Long activeCustomers;
    private Long vipCustomers;
    private Long newCustomersThisMonth;
    private Long newCustomersThisWeek;

    // ========================
    // SAFARI METRICS
    // ========================
    private Long totalSafaris;
    private Long activeSafaris;
    private Long upcomingSafaris; // Starting within 30 days
    private Long ongoingSafaris; // Currently in progress
    private Long completedSafaris;
    private Long cancelledSafaris;
    private Map<String, Long> safarisByState;

    // ========================
    // ACTIVITY METRICS
    // ========================
    private Long totalActivities;
    private Long totalAccommodations;
    private Long totalParks;

    // ========================
    // USER METRICS
    // ========================
    private Long totalUsers;
    private Long activeUsers;

    // ========================
    // RECENT ACTIVITY
    // ========================
    private List<RecentActivityDTO> recentQuotes;
    private List<RecentActivityDTO> recentInvoices;
    private List<RecentActivityDTO> recentSafaris;
    private List<RecentActivityDTO> recentCustomers;

    /**
     * Revenue by currency DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueByCurrency {
        private String currency;
        private BigDecimal amount;
    }

    /**
     * Recent activity DTO for timeline
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentActivityDTO {
        private String id;
        private String code;
        private String title;
        private String type; // QUOTE, INVOICE, SAFARI, CUSTOMER
        private String status;
        private String createdAt;
        private String createdBy;
    }
}
