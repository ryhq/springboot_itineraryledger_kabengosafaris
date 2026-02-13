package com.itineraryledger.kabengosafaris.Dashboard.Service;

import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RecentActivityDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RevenueByCurrency;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard Service
 * Provides aggregated statistics and metrics for the dashboard
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final SafariRepository safariRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final AccommodationRepository accommodationRepository;
    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get comprehensive dashboard statistics
     */
    public ResponseEntity<ApiResponse<?>> getDashboardStats() {
        try {
            log.info("Fetching dashboard statistics");

            DashboardStatsDTO stats = DashboardStatsDTO.builder()
                    // Quote metrics
                    .totalQuotes(quoteRepository.count())
                    .draftQuotes(quoteRepository.countByStatus(QuoteStatus.DRAFT))
                    .sentQuotes(quoteRepository.countByStatus(QuoteStatus.SENT))
                    .acceptedQuotes(quoteRepository.countByStatus(QuoteStatus.ACCEPTED))
                    .convertedQuotes(quoteRepository.countByStatus(QuoteStatus.CONVERTED))
                    .expiredQuotes(quoteRepository.countByStatus(QuoteStatus.EXPIRED))
                    .quoteConversionRate(calculateQuoteConversionRate())
                    .quotesByStatus(getQuotesByStatus())

                    // Invoice metrics
                    .totalInvoices(invoiceRepository.count())
                    .draftInvoices(invoiceRepository.countByStatus(InvoiceStatus.DRAFT))
                    .sentInvoices(invoiceRepository.countByStatus(InvoiceStatus.SENT))
                    .paidInvoices(invoiceRepository.countByStatus(InvoiceStatus.PAID))
                    .overdueInvoices(invoiceRepository.countByStatus(InvoiceStatus.OVERDUE))
                    .partiallyPaidInvoices(invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID))
                    .invoicesByStatus(getInvoicesByStatus())
                    .totalRevenue(calculateTotalRevenue())
                    .pendingRevenue(calculatePendingRevenue())
                    .overdueRevenue(calculateOverdueRevenue())

                    // Customer metrics
                    .totalCustomers(customerRepository.count())
                    .activeCustomers(customerRepository.countByIsActiveTrue())
                    .vipCustomers(customerRepository.countByIsVipTrue())
                    .newCustomersThisMonth(getNewCustomersCount(LocalDateTime.now().minusMonths(1)))
                    .newCustomersThisWeek(getNewCustomersCount(LocalDateTime.now().minusWeeks(1)))

                    // Safari metrics
                    .totalSafaris(safariRepository.count())
                    .activeSafaris(safariRepository.countByIsActiveTrue())
                    .upcomingSafaris(getUpcomingSafarisCount())
                    .ongoingSafaris(getOngoingSafarisCount())
                    .completedSafaris(safariRepository.countByState(SafariState.COMPLETED))
                    .cancelledSafaris(safariRepository.countByState(SafariState.CANCELLED))
                    .safarisByState(getSafarisByState())

                    // Activity metrics
                    .totalActivities(activityRepository.count())
                    .totalAccommodations(accommodationRepository.count())
                    .totalParks(parkRepository.count())

                    // User metrics
                    .totalUsers(userRepository.count())
                    .activeUsers(userRepository.countByEnabledTrue())

                    // Recent activity
                    .recentQuotes(getRecentQuotes())
                    .recentInvoices(getRecentInvoices())
                    .recentSafaris(getRecentSafaris())
                    .recentCustomers(getRecentCustomers())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(200, "Dashboard statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error fetching dashboard statistics", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to fetch dashboard statistics: " + e.getMessage(), "DASHBOARD_ERROR")
            );
        }
    }

    /**
     * Get quote statistics only
     */
    public ResponseEntity<ApiResponse<?>> getQuoteStats() {
        try {
            Map<String, Object> quoteStats = new HashMap<>();
            quoteStats.put("totalQuotes", quoteRepository.count());
            quoteStats.put("draftQuotes", quoteRepository.countByStatus(QuoteStatus.DRAFT));
            quoteStats.put("sentQuotes", quoteRepository.countByStatus(QuoteStatus.SENT));
            quoteStats.put("acceptedQuotes", quoteRepository.countByStatus(QuoteStatus.ACCEPTED));
            quoteStats.put("convertedQuotes", quoteRepository.countByStatus(QuoteStatus.CONVERTED));
            quoteStats.put("expiredQuotes", quoteRepository.countByStatus(QuoteStatus.EXPIRED));
            quoteStats.put("conversionRate", calculateQuoteConversionRate());
            quoteStats.put("quotesByStatus", getQuotesByStatus());
            quoteStats.put("recentQuotes", getRecentQuotes());

            return ResponseEntity.ok(ApiResponse.success(200, "Quote statistics retrieved successfully", quoteStats));
        } catch (Exception e) {
            log.error("Error fetching quote statistics", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to fetch quote statistics: " + e.getMessage(), "QUOTE_STATS_ERROR")
            );
        }
    }

    /**
     * Get invoice statistics only
     */
    public ResponseEntity<ApiResponse<?>> getInvoiceStats() {
        try {
            Map<String, Object> invoiceStats = new HashMap<>();
            invoiceStats.put("totalInvoices", invoiceRepository.count());
            invoiceStats.put("draftInvoices", invoiceRepository.countByStatus(InvoiceStatus.DRAFT));
            invoiceStats.put("sentInvoices", invoiceRepository.countByStatus(InvoiceStatus.SENT));
            invoiceStats.put("paidInvoices", invoiceRepository.countByStatus(InvoiceStatus.PAID));
            invoiceStats.put("overdueInvoices", invoiceRepository.countByStatus(InvoiceStatus.OVERDUE));
            invoiceStats.put("partiallyPaidInvoices", invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID));
            invoiceStats.put("invoicesByStatus", getInvoicesByStatus());
            invoiceStats.put("totalRevenue", calculateTotalRevenue());
            invoiceStats.put("pendingRevenue", calculatePendingRevenue());
            invoiceStats.put("overdueRevenue", calculateOverdueRevenue());
            invoiceStats.put("recentInvoices", getRecentInvoices());

            return ResponseEntity.ok(ApiResponse.success(200, "Invoice statistics retrieved successfully", invoiceStats));
        } catch (Exception e) {
            log.error("Error fetching invoice statistics", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to fetch invoice statistics: " + e.getMessage(), "INVOICE_STATS_ERROR")
            );
        }
    }

    /**
     * Get customer statistics only
     */
    public ResponseEntity<ApiResponse<?>> getCustomerStats() {
        try {
            Map<String, Object> customerStats = new HashMap<>();
            customerStats.put("totalCustomers", customerRepository.count());
            customerStats.put("activeCustomers", customerRepository.countByIsActiveTrue());
            customerStats.put("vipCustomers", customerRepository.countByIsVipTrue());
            customerStats.put("newCustomersThisMonth", getNewCustomersCount(LocalDateTime.now().minusMonths(1)));
            customerStats.put("newCustomersThisWeek", getNewCustomersCount(LocalDateTime.now().minusWeeks(1)));
            customerStats.put("recentCustomers", getRecentCustomers());

            return ResponseEntity.ok(ApiResponse.success(200, "Customer statistics retrieved successfully", customerStats));
        } catch (Exception e) {
            log.error("Error fetching customer statistics", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to fetch customer statistics: " + e.getMessage(), "CUSTOMER_STATS_ERROR")
            );
        }
    }

    /**
     * Get safari statistics only
     */
    public ResponseEntity<ApiResponse<?>> getSafariStats() {
        try {
            Map<String, Object> safariStats = new HashMap<>();
            safariStats.put("totalSafaris", safariRepository.count());
            safariStats.put("activeSafaris", safariRepository.countByIsActiveTrue());
            safariStats.put("upcomingSafaris", getUpcomingSafarisCount());
            safariStats.put("ongoingSafaris", getOngoingSafarisCount());
            safariStats.put("completedSafaris", safariRepository.countByState(SafariState.COMPLETED));
            safariStats.put("cancelledSafaris", safariRepository.countByState(SafariState.CANCELLED));
            safariStats.put("safarisByState", getSafarisByState());
            safariStats.put("recentSafaris", getRecentSafaris());

            return ResponseEntity.ok(ApiResponse.success(200, "Safari statistics retrieved successfully", safariStats));
        } catch (Exception e) {
            log.error("Error fetching safari statistics", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to fetch safari statistics: " + e.getMessage(), "SAFARI_STATS_ERROR")
            );
        }
    }

    // ========================
    // HELPER METHODS
    // ========================

    private Double calculateQuoteConversionRate() {
        long totalSent = quoteRepository.countByStatus(QuoteStatus.SENT);
        long accepted = quoteRepository.countByStatus(QuoteStatus.ACCEPTED);
        long converted = quoteRepository.countByStatus(QuoteStatus.CONVERTED);

        if (totalSent == 0) return 0.0;
        return ((double) (accepted + converted) / totalSent) * 100;
    }

    private Map<String, Long> getQuotesByStatus() {
        Map<String, Long> statusMap = new HashMap<>();
        for (QuoteStatus status : QuoteStatus.values()) {
            statusMap.put(status.name(), quoteRepository.countByStatus(status));
        }
        return statusMap;
    }

    private Map<String, Long> getInvoicesByStatus() {
        Map<String, Long> statusMap = new HashMap<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            statusMap.put(status.name(), invoiceRepository.countByStatus(status));
        }
        return statusMap;
    }

    private Map<String, Long> getSafarisByState() {
        Map<String, Long> stateMap = new HashMap<>();
        for (SafariState state : SafariState.values()) {
            stateMap.put(state.name(), safariRepository.countByState(state));
        }
        return stateMap;
    }

    private List<RevenueByCurrency> calculateTotalRevenue() {
        List<Invoice> paidInvoices = invoiceRepository.findByStatus(InvoiceStatus.PAID);
        return aggregateRevenueByCurrency(paidInvoices, Invoice::getGrandTotals);
    }

    private List<RevenueByCurrency> calculatePendingRevenue() {
        List<Invoice> pendingInvoices = invoiceRepository.findByStatusIn(
                Arrays.asList(InvoiceStatus.SENT, InvoiceStatus.VIEWED, InvoiceStatus.PARTIALLY_PAID)
        );
        return aggregateRevenueByCurrency(pendingInvoices, Invoice::getBalances);
    }

    private List<RevenueByCurrency> calculateOverdueRevenue() {
        List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
        return aggregateRevenueByCurrency(overdueInvoices, Invoice::getBalances);
    }

    private List<RevenueByCurrency> aggregateRevenueByCurrency(
            List<Invoice> invoices,
            java.util.function.Function<Invoice, List<Price>> priceExtractor
    ) {
        Map<String, BigDecimal> revenueMap = new HashMap<>();

        for (Invoice invoice : invoices) {
            List<Price> prices = priceExtractor.apply(invoice);
            if (prices != null) {
                for (Price price : prices) {
                    String currency = price.getCurrency();
                    BigDecimal amount = price.getTotalPrice() != null ? price.getTotalPrice() : BigDecimal.ZERO;
                    revenueMap.merge(currency, amount, BigDecimal::add);
                }
            }
        }

        return revenueMap.entrySet().stream()
                .map(entry -> RevenueByCurrency.builder()
                        .currency(entry.getKey())
                        .amount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private Long getNewCustomersCount(LocalDateTime since) {
        return customerRepository.countByCreatedAtAfter(since);
    }

    private Long getUpcomingSafarisCount() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);
        return safariRepository.countByStartDateBetween(today, thirtyDaysFromNow);
    }

    private Long getOngoingSafarisCount() {
        LocalDate today = LocalDate.now();
        return safariRepository.countByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today);
    }

    private List<RecentActivityDTO> getRecentQuotes() {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Quote> quotes = quoteRepository.findAll(pageRequest).getContent();

        return quotes.stream()
                .map(quote -> RecentActivityDTO.builder()
                        .id(idObfuscator.encodeId(quote.getId()))
                        .code(quote.getQuoteCode())
                        .title(quote.getTitle())
                        .type("QUOTE")
                        .status(quote.getStatus().getDisplayName())
                        .createdAt(quote.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .createdBy(quote.getCreatedBy() != null ? quote.getCreatedBy().getUsername() : "System")
                        .build())
                .collect(Collectors.toList());
    }

    private List<RecentActivityDTO> getRecentInvoices() {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Invoice> invoices = invoiceRepository.findAll(pageRequest).getContent();

        return invoices.stream()
                .map(invoice -> RecentActivityDTO.builder()
                        .id(idObfuscator.encodeId(invoice.getId()))
                        .code(invoice.getInvoiceCode())
                        .title(invoice.getTitle())
                        .type("INVOICE")
                        .status(invoice.getStatus().getDisplayName())
                        .createdAt(invoice.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .createdBy(invoice.getCreatedBy() != null ? invoice.getCreatedBy().getUsername() : "System")
                        .build())
                .collect(Collectors.toList());
    }

    private List<RecentActivityDTO> getRecentSafaris() {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Safari> safaris = safariRepository.findAll(pageRequest).getContent();

        return safaris.stream()
                .map(safari -> RecentActivityDTO.builder()
                        .id(idObfuscator.encodeId(safari.getId()))
                        .code(safari.getCode())
                        .title(safari.getName())
                        .type("SAFARI")
                        .status(safari.getState().name())
                        .createdAt(safari.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .createdBy(safari.getCreatedBy() != null ? safari.getCreatedBy().getUsername() : "System")
                        .build())
                .collect(Collectors.toList());
    }

    private List<RecentActivityDTO> getRecentCustomers() {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Customer> customers = customerRepository.findAll(pageRequest).getContent();

        return customers.stream()
                .map(customer -> RecentActivityDTO.builder()
                        .id(idObfuscator.encodeId(customer.getId()))
                        .code(customer.getCode())
                        .title(customer.getDisplayName())
                        .type("CUSTOMER")
                        .status(customer.getIsActive() ? "Active" : "Inactive")
                        .createdAt(customer.getCreatedAt().format(DATE_TIME_FORMATTER))
                        .createdBy("System")
                        .build())
                .collect(Collectors.toList());
    }
}
