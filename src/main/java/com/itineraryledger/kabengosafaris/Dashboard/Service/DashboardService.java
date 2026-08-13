package com.itineraryledger.kabengosafaris.Dashboard.Service;

import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.AlertsDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RecentActivityDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RevenueByCurrency;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.ExpenseStatsDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.LeaderboardItemDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.PaymentStatsDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.PipelineStageDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.SafariPnLDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.TrendPointDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseLineItem;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseLineItemRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices.ExpensePaymentAggregationService;
import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Enums.PaymentMethod;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoicePaymentAggregationService;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
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
    private final InvoicePaymentAggregationService paymentAggregationService;
    private final PaymentRepository paymentRepository;
    // Expense side (money out)
    private final ExpenseRepository expenseRepository;
    private final ExpenseLineItemRepository expenseLineItemRepository;
    private final ExpensePaymentRepository expensePaymentRepository;
    private final ExpensePaymentAggregationService expensePaymentAggregationService;
    private final VendorRepository vendorRepository;

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

    /**
     * What share of the quotes we actually sent came to something, as a percentage.
     *
     * The denominator is every quote that ever left the building — still out, accepted,
     * converted, rejected or expired — not the ones sitting in SENT right now. Status is a
     * position, not a history: a quote that is accepted is no longer SENT, so dividing by
     * SENT alone measured winners against the dwindling pile of undecided ones. With eight
     * accepted and two still out it reported 400%, and a rate above 100% is a number nobody
     * can act on.
     *
     * DRAFT, READY and CANCELLED are excluded because the customer never saw them, and a
     * quote nobody was shown says nothing about how persuasive we are.
     *
     * Returns null rather than 0.0 when nothing has been sent: "none of our quotes convert"
     * and "we have not sent any" are opposite statements, and only one of them is bad.
     */
    private Double calculateQuoteConversionRate() {
        long accepted = quoteRepository.countByStatus(QuoteStatus.ACCEPTED);
        long converted = quoteRepository.countByStatus(QuoteStatus.CONVERTED);
        long everSent = accepted
            + converted
            + quoteRepository.countByStatus(QuoteStatus.SENT)
            + quoteRepository.countByStatus(QuoteStatus.REJECTED)
            + quoteRepository.countByStatus(QuoteStatus.EXPIRED);

        if (everSent == 0) return null;
        return ((double) (accepted + converted) / everSent) * 100;
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
                Arrays.asList(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE)
        );
        return aggregateRevenueByCurrency(pendingInvoices, paymentAggregationService::computeBalances);
    }

    private List<RevenueByCurrency> calculateOverdueRevenue() {
        List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
        return aggregateRevenueByCurrency(overdueInvoices, paymentAggregationService::computeBalances);
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

    // =========================================================================
    // TRENDS (time-series)
    // =========================================================================

    public ResponseEntity<ApiResponse<?>> getRevenueTrend(LocalDate from, LocalDate to, String period) {
        try {
            LocalDate[] range = resolveRange(from, to, 30);
            TrendPeriod p = TrendPeriod.parse(period);

            List<Payment> payments = paymentRepository.findByPaymentDateBetween(range[0], range[1]);

            Map<String, List<Payment>> grouped = payments.stream()
                    .collect(Collectors.groupingBy(pmt -> p.bucketKey(pmt.getPaymentDate()),
                            LinkedHashMap::new, Collectors.toList()));

            List<TrendPointDTO> points = new ArrayList<>();
            for (LocalDate cursor = range[0]; !cursor.isAfter(range[1]); cursor = p.advance(cursor)) {
                String key = p.bucketKey(cursor);
                List<Payment> bucket = grouped.getOrDefault(key, Collections.emptyList());

                Map<String, BigDecimal> byCurrency = new HashMap<>();
                for (Payment pmt : bucket) {
                    String currency = pmt.getInvoiceCurrency() != null ? pmt.getInvoiceCurrency() : pmt.getCurrency();
                    BigDecimal amount = pmt.getBaseAmount() != null ? pmt.getBaseAmount() : pmt.getAmount();
                    if (amount != null && currency != null) {
                        byCurrency.merge(currency, amount, BigDecimal::add);
                    }
                }

                points.add(TrendPointDTO.builder()
                        .period(key)
                        .label(p.label(cursor))
                        .count((long) bucket.size())
                        .revenue(byCurrency.entrySet().stream()
                                .map(e -> RevenueByCurrency.builder().currency(e.getKey()).amount(e.getValue()).build())
                                .collect(Collectors.toList()))
                        .build());
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("period", p.name().toLowerCase());
            body.put("from", range[0].toString());
            body.put("to", range[1].toString());
            body.put("points", points);
            return ResponseEntity.ok(ApiResponse.success(200, "Revenue trend retrieved successfully", body));
        } catch (Exception e) {
            log.error("Error building revenue trend", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build revenue trend: " + e.getMessage(), "REVENUE_TREND_ERROR"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getBookingsTrend(LocalDate from, LocalDate to, String period) {
        try {
            LocalDate[] range = resolveRange(from, to, 30);
            TrendPeriod p = TrendPeriod.parse(period);

            LocalDateTime fromDt = range[0].atStartOfDay();
            LocalDateTime toDt = range[1].atTime(23, 59, 59);

            List<Safari> safaris = safariRepository.findAll().stream()
                    .filter(s -> s.getCreatedAt() != null
                            && !s.getCreatedAt().isBefore(fromDt)
                            && !s.getCreatedAt().isAfter(toDt))
                    .collect(Collectors.toList());

            Map<String, List<Safari>> grouped = safaris.stream()
                    .collect(Collectors.groupingBy(s -> p.bucketKey(s.getCreatedAt().toLocalDate()),
                            LinkedHashMap::new, Collectors.toList()));

            List<TrendPointDTO> points = new ArrayList<>();
            for (LocalDate cursor = range[0]; !cursor.isAfter(range[1]); cursor = p.advance(cursor)) {
                String key = p.bucketKey(cursor);
                List<Safari> bucket = grouped.getOrDefault(key, Collections.emptyList());

                points.add(TrendPointDTO.builder()
                        .period(key)
                        .label(p.label(cursor))
                        .count((long) bucket.size())
                        .revenue(Collections.emptyList())
                        .build());
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("period", p.name().toLowerCase());
            body.put("from", range[0].toString());
            body.put("to", range[1].toString());
            body.put("points", points);
            body.put("total", (long) safaris.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Bookings trend retrieved successfully", body));
        } catch (Exception e) {
            log.error("Error building bookings trend", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build bookings trend: " + e.getMessage(), "BOOKINGS_TREND_ERROR"));
        }
    }

    // =========================================================================
    // LEADERBOARDS
    // =========================================================================

    public ResponseEntity<ApiResponse<?>> getTopParks(int limit) {
        return leaderboardFromAggregate("Top parks",
                () -> safariRepository.findTopParksByBookings(excludedBookingStates(), PageRequest.of(0, clampLimit(limit))));
    }

    public ResponseEntity<ApiResponse<?>> getTopActivities(int limit) {
        return leaderboardFromAggregate("Top activities",
                () -> safariRepository.findTopActivitiesByBookings(excludedBookingStates(), PageRequest.of(0, clampLimit(limit))));
    }

    public ResponseEntity<ApiResponse<?>> getTopAccommodations(int limit) {
        return leaderboardFromAggregate("Top accommodations",
                () -> safariRepository.findTopAccommodationsByBookings(excludedBookingStates(), PageRequest.of(0, clampLimit(limit))));
    }

    public ResponseEntity<ApiResponse<?>> getTopCustomers(int limit) {
        try {
            int n = clampLimit(limit);
            // Aggregate invoice grand totals per customer per currency.
            Map<Long, Map<String, BigDecimal>> customerRevenue = new HashMap<>();
            Map<Long, Long> customerInvoiceCount = new HashMap<>();

            for (Invoice inv : invoiceRepository.findByStatus(InvoiceStatus.PAID)) {
                if (inv.getCustomer() == null || inv.getGrandTotals() == null) continue;
                Long cid = inv.getCustomer().getId();
                customerInvoiceCount.merge(cid, 1L, Long::sum);
                Map<String, BigDecimal> map = customerRevenue.computeIfAbsent(cid, k -> new HashMap<>());
                for (Price price : inv.getGrandTotals()) {
                    if (price.getCurrency() == null) continue;
                    BigDecimal amt = price.getTotalPrice() != null ? price.getTotalPrice() : BigDecimal.ZERO;
                    map.merge(price.getCurrency(), amt, BigDecimal::add);
                }
            }

            // Rank by sum of all currencies (simple heuristic — could be FX-converted later)
            List<Map.Entry<Long, Map<String, BigDecimal>>> ranked = customerRevenue.entrySet().stream()
                    .sorted((a, b) -> sumAll(b.getValue()).compareTo(sumAll(a.getValue())))
                    .limit(n)
                    .collect(Collectors.toList());

            List<LeaderboardItemDTO> items = new ArrayList<>();
            for (Map.Entry<Long, Map<String, BigDecimal>> entry : ranked) {
                Customer customer = customerRepository.findById(entry.getKey()).orElse(null);
                if (customer == null) continue;
                List<RevenueByCurrency> revenue = entry.getValue().entrySet().stream()
                        .map(e -> RevenueByCurrency.builder().currency(e.getKey()).amount(e.getValue()).build())
                        .collect(Collectors.toList());
                items.add(LeaderboardItemDTO.builder()
                        .id(idObfuscator.encodeId(customer.getId()))
                        .code(customer.getCode())
                        .name(customer.getDisplayName())
                        .subtitle(customer.getCustomerType() != null ? customer.getCustomerType().getDisplayName() : null)
                        .count(customerInvoiceCount.getOrDefault(customer.getId(), 0L))
                        .revenue(revenue)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Top customers retrieved successfully", items));
        } catch (Exception e) {
            log.error("Error building top customers", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build top customers: " + e.getMessage(), "TOP_CUSTOMERS_ERROR"));
        }
    }

    // =========================================================================
    // SAFARI PIPELINE
    // =========================================================================

    public ResponseEntity<ApiResponse<?>> getSafariPipeline() {
        try {
            // Index invoices by safari id once
            Map<Long, Invoice> bySafari = new HashMap<>();
            for (Invoice inv : invoiceRepository.findAll()) {
                if (inv.getSafari() != null) bySafari.put(inv.getSafari().getId(), inv);
            }

            Map<SafariState, List<Safari>> bucketed = safariRepository.findAll().stream()
                    .filter(s -> s.getState() != null)
                    .collect(Collectors.groupingBy(Safari::getState));

            List<PipelineStageDTO> stages = new ArrayList<>();
            for (SafariState state : SafariState.values()) {
                List<Safari> group = bucketed.getOrDefault(state, Collections.emptyList());

                Map<String, BigDecimal> revenueMap = new HashMap<>();
                for (Safari s : group) {
                    Invoice inv = bySafari.get(s.getId());
                    if (inv == null || inv.getGrandTotals() == null) continue;
                    for (Price price : inv.getGrandTotals()) {
                        if (price.getCurrency() == null) continue;
                        BigDecimal amt = price.getTotalPrice() != null ? price.getTotalPrice() : BigDecimal.ZERO;
                        revenueMap.merge(price.getCurrency(), amt, BigDecimal::add);
                    }
                }
                List<RevenueByCurrency> revenue = revenueMap.entrySet().stream()
                        .map(e -> RevenueByCurrency.builder().currency(e.getKey()).amount(e.getValue()).build())
                        .collect(Collectors.toList());

                stages.add(PipelineStageDTO.builder()
                        .state(state.name())
                        .displayName(state.getDisplayName())
                        .group(state.isCoreJourney() ? "CORE" : "EXCEPTION")
                        .count((long) group.size())
                        .projectedRevenue(revenue)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Safari pipeline retrieved successfully", stages));
        } catch (Exception e) {
            log.error("Error building safari pipeline", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build safari pipeline: " + e.getMessage(), "PIPELINE_ERROR"));
        }
    }

    // =========================================================================
    // PAYMENT STATS
    // =========================================================================

    public ResponseEntity<ApiResponse<?>> getPaymentStats() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate yearStart = today.withDayOfYear(1);

            List<Payment> all = paymentRepository.findAll();

            List<Payment> todayList = all.stream().filter(p -> p.getPaymentDate() != null && p.getPaymentDate().isEqual(today)).collect(Collectors.toList());
            List<Payment> weekList = all.stream().filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(weekStart)).collect(Collectors.toList());
            List<Payment> monthList = all.stream().filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(monthStart)).collect(Collectors.toList());
            List<Payment> yearList = all.stream().filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(yearStart)).collect(Collectors.toList());

            Map<String, Long> byMethod = new LinkedHashMap<>();
            for (PaymentMethod m : PaymentMethod.values()) byMethod.put(m.name(), 0L);
            Map<String, Map<String, BigDecimal>> revByMethod = new LinkedHashMap<>();
            for (Payment p : all) {
                String method = p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "OTHER";
                byMethod.merge(method, 1L, Long::sum);
                String currency = p.getInvoiceCurrency() != null ? p.getInvoiceCurrency() : p.getCurrency();
                BigDecimal amt = p.getBaseAmount() != null ? p.getBaseAmount() : p.getAmount();
                if (currency != null && amt != null) {
                    revByMethod.computeIfAbsent(method, k -> new HashMap<>()).merge(currency, amt, BigDecimal::add);
                }
            }

            Map<String, List<RevenueByCurrency>> revByMethodDto = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, BigDecimal>> e : revByMethod.entrySet()) {
                revByMethodDto.put(e.getKey(), e.getValue().entrySet().stream()
                        .map(en -> RevenueByCurrency.builder().currency(en.getKey()).amount(en.getValue()).build())
                        .collect(Collectors.toList()));
            }

            PaymentStatsDTO stats = PaymentStatsDTO.builder()
                    .collectedToday(aggregatePaymentsByCurrency(todayList))
                    .collectedThisWeek(aggregatePaymentsByCurrency(weekList))
                    .collectedThisMonth(aggregatePaymentsByCurrency(monthList))
                    .collectedThisYear(aggregatePaymentsByCurrency(yearList))
                    .totalPayments((long) all.size())
                    .paymentsToday((long) todayList.size())
                    .paymentsThisWeek((long) weekList.size())
                    .paymentsThisMonth((long) monthList.size())
                    .byMethod(byMethod)
                    .revenueByMethod(revByMethodDto)
                    .recentPayments(mapRecentPayments(paymentRepository.findTop20ByOrderByPaymentDateDescIdDesc()))
                    .build();
            return ResponseEntity.ok(ApiResponse.success(200, "Payment statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error building payment stats", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build payment stats: " + e.getMessage(), "PAYMENT_STATS_ERROR"));
        }
    }

    // =========================================================================
    // ALERTS (ops health panel)
    // =========================================================================

    public ResponseEntity<ApiResponse<?>> getAlerts() {
        try {
            LocalDate today = LocalDate.now();

            // Overdue aging buckets
            List<Invoice> overdue = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
            Map<String, BigDecimal> b0_30 = new HashMap<>();
            Map<String, BigDecimal> b31_60 = new HashMap<>();
            Map<String, BigDecimal> b61 = new HashMap<>();
            long c0_30 = 0, c31_60 = 0, c61 = 0;
            for (Invoice inv : overdue) {
                LocalDate due = inv.getDueDate();
                long days = due == null ? 0 : ChronoUnit.DAYS.between(due, today);
                List<Price> balances = paymentAggregationService.computeBalances(inv);
                Map<String, BigDecimal> bucket;
                if (days <= 30) { bucket = b0_30; c0_30++; }
                else if (days <= 60) { bucket = b31_60; c31_60++; }
                else { bucket = b61; c61++; }
                if (balances != null) {
                    for (Price bal : balances) {
                        if (bal.getCurrency() == null) continue;
                        BigDecimal amt = bal.getTotalPrice() != null ? bal.getTotalPrice() : BigDecimal.ZERO;
                        bucket.merge(bal.getCurrency(), amt, BigDecimal::add);
                    }
                }
            }

            AlertsDTO.OverdueAgingDTO aging = AlertsDTO.OverdueAgingDTO.builder()
                    .zeroToThirty(mapRevenue(b0_30))
                    .thirtyOneToSixty(mapRevenue(b31_60))
                    .sixtyOnePlus(mapRevenue(b61))
                    .invoiceCount0_30(c0_30)
                    .invoiceCount31_60(c31_60)
                    .invoiceCount61Plus(c61)
                    .build();

            // Expiring quotes (next 7 days, still live)
            LocalDate weekAhead = today.plusDays(7);
            List<Quote> expiringQuotes = quoteRepository.findAll().stream()
                    .filter(q -> q.getValidTo() != null
                            && !q.getValidTo().isBefore(today)
                            && !q.getValidTo().isAfter(weekAhead)
                            && q.getStatus() != null
                            && (q.getStatus() == QuoteStatus.READY
                                    || q.getStatus() == QuoteStatus.SENT
                                    || q.getStatus() == QuoteStatus.ACCEPTED))
                    .sorted(Comparator.comparing(Quote::getValidTo))
                    .limit(10)
                    .collect(Collectors.toList());

            // Unpaid upcoming safaris (start date within 14 days, not paid)
            LocalDate twoWeeksAhead = today.plusDays(14);
            List<Safari> unpaidUpcoming = safariRepository.findAll().stream()
                    .filter(s -> s.getStartDate() != null
                            && !s.getStartDate().isBefore(today)
                            && !s.getStartDate().isAfter(twoWeeksAhead)
                            && s.getState() != null
                            && (s.getState() == SafariState.PENDING_PAYMENT
                                    || s.getState() == SafariState.CONFIRMED
                                    || s.getState() == SafariState.APPROVED))
                    .sorted(Comparator.comparing(Safari::getStartDate))
                    .limit(10)
                    .collect(Collectors.toList());

            // Safaris ready to start (FULLY_PAID, start today)
            List<Safari> readyToStart = safariRepository.findReadyToStart(today).stream()
                    .limit(10)
                    .collect(Collectors.toList());

            // Blacklisted customers (stale but worth flagging)
            List<Customer> blacklisted = customerRepository.findByIsBlacklistedTrue().stream()
                    .limit(10)
                    .collect(Collectors.toList());

            long total = c0_30 + c31_60 + c61 + expiringQuotes.size() + unpaidUpcoming.size()
                    + readyToStart.size() + blacklisted.size();

            AlertsDTO dto = AlertsDTO.builder()
                    .overdueAging(aging)
                    .expiringQuotes(expiringQuotes.stream().map(this::toRecent).collect(Collectors.toList()))
                    .unpaidUpcomingSafaris(unpaidUpcoming.stream().map(this::toRecent).collect(Collectors.toList()))
                    .safarisReadyToStart(readyToStart.stream().map(this::toRecent).collect(Collectors.toList()))
                    .blacklistedCustomers(blacklisted.stream().map(this::toRecent).collect(Collectors.toList()))
                    .totalAlerts(total)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(200, "Alerts retrieved successfully", dto));
        } catch (Exception e) {
            log.error("Error building alerts", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build alerts: " + e.getMessage(), "ALERTS_ERROR"));
        }
    }

    // =========================================================================
    // INTERNAL HELPERS
    // =========================================================================

    private LocalDate[] resolveRange(LocalDate from, LocalDate to, int defaultDaysBack) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(defaultDaysBack);
        if (start.isAfter(end)) {
            LocalDate swap = start; start = end; end = swap;
        }
        return new LocalDate[]{start, end};
    }

    private int clampLimit(int limit) {
        if (limit <= 0) return 5;
        return Math.min(limit, 50);
    }

    private List<SafariState> excludedBookingStates() {
        return Arrays.asList(SafariState.DRAFT, SafariState.CANCELLED);
    }

    private ResponseEntity<ApiResponse<?>> leaderboardFromAggregate(String label,
                                                                     java.util.function.Supplier<List<Object[]>> source) {
        try {
            List<Object[]> rows = source.get();
            List<LeaderboardItemDTO> items = rows.stream()
                    .map(row -> LeaderboardItemDTO.builder()
                            .id(row[0] != null ? idObfuscator.encodeId(((Number) row[0]).longValue()) : null)
                            .name((String) row[1])
                            .count(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                            .revenue(Collections.emptyList())
                            .build())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(200, label + " retrieved successfully", items));
        } catch (Exception e) {
            log.error("Error building {}", label, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build " + label + ": " + e.getMessage(), "LEADERBOARD_ERROR"));
        }
    }

    private BigDecimal sumAll(Map<String, BigDecimal> byCurrency) {
        return byCurrency.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RevenueByCurrency> mapRevenue(Map<String, BigDecimal> byCurrency) {
        return byCurrency.entrySet().stream()
                .map(e -> RevenueByCurrency.builder().currency(e.getKey()).amount(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private List<RevenueByCurrency> aggregatePaymentsByCurrency(List<Payment> payments) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Payment p : payments) {
            String currency = p.getInvoiceCurrency() != null ? p.getInvoiceCurrency() : p.getCurrency();
            BigDecimal amount = p.getBaseAmount() != null ? p.getBaseAmount() : p.getAmount();
            if (currency != null && amount != null) {
                map.merge(currency, amount, BigDecimal::add);
            }
        }
        return mapRevenue(map);
    }

    private List<PaymentStatsDTO.RecentPaymentDTO> mapRecentPayments(List<Payment> payments) {
        return payments.stream().map(p -> PaymentStatsDTO.RecentPaymentDTO.builder()
                .id(idObfuscator.encodeId(p.getId()))
                .invoiceCode(p.getInvoice() != null ? p.getInvoice().getInvoiceCode() : null)
                .customerName(p.getInvoice() != null && p.getInvoice().getCustomer() != null
                        ? p.getInvoice().getCustomer().getDisplayName() : null)
                .method(p.getPaymentMethod() != null ? p.getPaymentMethod().getDisplayName() : null)
                .currency(p.getCurrency())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate() != null ? p.getPaymentDate().toString() : null)
                .build()).collect(Collectors.toList());
    }

    private RecentActivityDTO toRecent(Quote q) {
        return RecentActivityDTO.builder()
                .id(idObfuscator.encodeId(q.getId()))
                .code(q.getQuoteCode())
                .title(q.getTitle())
                .type("QUOTE")
                .status(q.getStatus() != null ? q.getStatus().getDisplayName() : null)
                .createdAt(q.getValidTo() != null ? q.getValidTo().toString() : null)
                .createdBy(q.getCreatedBy() != null ? q.getCreatedBy().getUsername() : "System")
                .build();
    }

    private RecentActivityDTO toRecent(Safari s) {
        return RecentActivityDTO.builder()
                .id(idObfuscator.encodeId(s.getId()))
                .code(s.getCode())
                .title(s.getName())
                .type("SAFARI")
                .status(s.getState() != null ? s.getState().getDisplayName() : null)
                .createdAt(s.getStartDate() != null ? s.getStartDate().toString() : null)
                .createdBy(s.getCreatedBy() != null ? s.getCreatedBy().getUsername() : "System")
                .build();
    }

    private RecentActivityDTO toRecent(Customer c) {
        return RecentActivityDTO.builder()
                .id(idObfuscator.encodeId(c.getId()))
                .code(c.getCode())
                .title(c.getDisplayName())
                .type("CUSTOMER")
                .status(Boolean.TRUE.equals(c.getIsBlacklisted()) ? "Blacklisted" : "Active")
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_TIME_FORMATTER) : null)
                .createdBy("System")
                .build();
    }

    private enum TrendPeriod {
        DAY, WEEK, MONTH;

        static TrendPeriod parse(String value) {
            if (value == null) return DAY;
            try { return TrendPeriod.valueOf(value.trim().toUpperCase()); }
            catch (IllegalArgumentException ex) { return DAY; }
        }

        String bucketKey(LocalDate date) {
            switch (this) {
                case WEEK: return date.get(IsoFields.WEEK_BASED_YEAR) + "-W" + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                case MONTH: return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
                case DAY:
                default: return date.toString();
            }
        }

        String label(LocalDate date) {
            switch (this) {
                case WEEK: return "W" + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                case MONTH: return date.getMonth().name().substring(0, 3) + " " + date.getYear();
                case DAY:
                default: return date.format(DateTimeFormatter.ofPattern("MMM d"));
            }
        }

        LocalDate advance(LocalDate date) {
            switch (this) {
                case WEEK: return date.plusWeeks(1);
                case MONTH: return date.plusMonths(1);
                case DAY:
                default: return date.plusDays(1);
            }
        }
    }

    // =========================================================================
    // EXPENSE-SIDE AGGREGATIONS (money out)
    // =========================================================================

    public ResponseEntity<ApiResponse<?>> getExpenseTrend(LocalDate from, LocalDate to, String period) {
        try {
            LocalDate[] range = resolveRange(from, to, 30);
            TrendPeriod p = TrendPeriod.parse(period);

            List<ExpensePayment> payments = expensePaymentRepository.findByPaymentDateBetween(range[0], range[1]);

            Map<String, List<ExpensePayment>> grouped = payments.stream()
                    .collect(Collectors.groupingBy(pmt -> p.bucketKey(pmt.getPaymentDate()),
                            LinkedHashMap::new, Collectors.toList()));

            List<TrendPointDTO> points = new ArrayList<>();
            for (LocalDate cursor = range[0]; !cursor.isAfter(range[1]); cursor = p.advance(cursor)) {
                String key = p.bucketKey(cursor);
                List<ExpensePayment> bucket = grouped.getOrDefault(key, Collections.emptyList());

                Map<String, BigDecimal> byCurrency = new HashMap<>();
                for (ExpensePayment pmt : bucket) {
                    String currency = pmt.getExpenseCurrency() != null ? pmt.getExpenseCurrency() : pmt.getCurrency();
                    BigDecimal amount = pmt.getBaseAmount() != null ? pmt.getBaseAmount() : pmt.getAmount();
                    if (amount != null && currency != null) {
                        byCurrency.merge(currency, amount, BigDecimal::add);
                    }
                }

                points.add(TrendPointDTO.builder()
                        .period(key)
                        .label(p.label(cursor))
                        .count((long) bucket.size())
                        .revenue(byCurrency.entrySet().stream()
                                .map(e -> RevenueByCurrency.builder().currency(e.getKey()).amount(e.getValue()).build())
                                .collect(Collectors.toList()))
                        .build());
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("period", p.name().toLowerCase());
            body.put("from", range[0].toString());
            body.put("to", range[1].toString());
            body.put("points", points);
            return ResponseEntity.ok(ApiResponse.success(200, "Expense trend retrieved successfully", body));
        } catch (Exception e) {
            log.error("Error building expense trend", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build expense trend: " + e.getMessage(), "EXPENSE_TREND_ERROR"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getExpenseStats() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(DayOfWeek.MONDAY);
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate yearStart = today.withDayOfYear(1);

            List<ExpensePayment> all = expensePaymentRepository.findAll();
            List<ExpensePayment> todayList = filterPayments(all, today, today);
            List<ExpensePayment> weekList = filterPayments(all, weekStart, today);
            List<ExpensePayment> monthList = filterPayments(all, monthStart, today);
            List<ExpensePayment> yearList = filterPayments(all, yearStart, today);

            // Outstanding balances across all unpaid expenses
            Map<String, BigDecimal> outstanding = new HashMap<>();
            List<Expense> unpaidExpenses = expenseRepository.findByStatusIn(List.of(
                    ExpenseStatus.RECORDED, ExpenseStatus.PARTIALLY_PAID));
            for (Expense exp : unpaidExpenses) {
                for (Price bal : expensePaymentAggregationService.computeBalances(exp)) {
                    if (bal.getCurrency() == null) continue;
                    BigDecimal v = bal.getTotalPrice() != null ? bal.getTotalPrice() : BigDecimal.ZERO;
                    outstanding.merge(bal.getCurrency(), v, BigDecimal::add);
                }
            }

            // Counts by status
            Map<String, Long> byStatus = new LinkedHashMap<>();
            for (ExpenseStatus s : ExpenseStatus.values()) {
                byStatus.put(s.name(), expenseRepository.countByStatus(s));
            }

            // Spend by category — sum line-item totalPrices grouped by category, per currency
            Map<String, Map<String, BigDecimal>> byCategoryRaw = new LinkedHashMap<>();
            List<ExpenseLineItem> allLineItems = expenseLineItemRepository.findAll();
            for (ExpenseLineItem li : allLineItems) {
                if (li.getCategory() == null || !Boolean.TRUE.equals(li.getIsActive())) continue;
                Map<String, BigDecimal> bucket = byCategoryRaw
                        .computeIfAbsent(li.getCategory().name(), k -> new HashMap<>());
                if (li.getPrices() != null) {
                    for (Price pr : li.getPrices()) {
                        if (pr.getCurrency() == null) continue;
                        BigDecimal v = pr.getTotalPrice() != null ? pr.getTotalPrice() : BigDecimal.ZERO;
                        bucket.merge(pr.getCurrency(), v, BigDecimal::add);
                    }
                }
            }
            Map<String, List<RevenueByCurrency>> spendByCategory = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, BigDecimal>> e : byCategoryRaw.entrySet()) {
                spendByCategory.put(e.getKey(), mapRevenue(e.getValue()));
            }

            // Counts by payment method
            Map<String, Long> byMethod = new LinkedHashMap<>();
            for (PaymentMethod m : PaymentMethod.values()) byMethod.put(m.name(), 0L);
            for (ExpensePayment p : all) {
                String method = p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "OTHER";
                byMethod.merge(method, 1L, Long::sum);
            }

            ExpenseStatsDTO stats = ExpenseStatsDTO.builder()
                    .spentToday(aggregateExpensePaymentsByCurrency(todayList))
                    .spentThisWeek(aggregateExpensePaymentsByCurrency(weekList))
                    .spentThisMonth(aggregateExpensePaymentsByCurrency(monthList))
                    .spentThisYear(aggregateExpensePaymentsByCurrency(yearList))
                    .totalExpenses(expenseRepository.count())
                    .totalPayments((long) all.size())
                    .paymentsToday((long) todayList.size())
                    .paymentsThisWeek((long) weekList.size())
                    .paymentsThisMonth((long) monthList.size())
                    .outstandingByCurrency(mapRevenue(outstanding))
                    .expensesByStatus(byStatus)
                    .spendByCategory(spendByCategory)
                    .paymentsByMethod(byMethod)
                    .recentPayments(mapRecentExpensePayments(
                            expensePaymentRepository.findTop20ByOrderByPaymentDateDescIdDesc()))
                    .build();

            return ResponseEntity.ok(ApiResponse.success(200, "Expense statistics retrieved successfully", stats));
        } catch (Exception e) {
            log.error("Error building expense stats", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build expense stats: " + e.getMessage(), "EXPENSE_STATS_ERROR"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getTopVendors(int limit) {
        try {
            int n = clampLimit(limit);

            // Sum expense.grandTotals per vendor, per currency.
            Map<Long, Map<String, BigDecimal>> vendorSpend = new HashMap<>();
            Map<Long, Long> vendorExpenseCount = new HashMap<>();

            for (Expense exp : expenseRepository.findAll()) {
                if (exp.getVendor() == null) continue;
                if (exp.getStatus() == ExpenseStatus.CANCELLED) continue;

                Long vid = exp.getVendor().getId();
                vendorExpenseCount.merge(vid, 1L, Long::sum);

                Map<String, BigDecimal> bucket = vendorSpend.computeIfAbsent(vid, k -> new HashMap<>());
                if (exp.getGrandTotals() != null) {
                    for (Price gt : exp.getGrandTotals()) {
                        if (gt.getCurrency() == null) continue;
                        BigDecimal v = gt.getTotalPrice() != null ? gt.getTotalPrice() : BigDecimal.ZERO;
                        bucket.merge(gt.getCurrency(), v, BigDecimal::add);
                    }
                }
            }

            List<Map.Entry<Long, Map<String, BigDecimal>>> ranked = vendorSpend.entrySet().stream()
                    .sorted((a, b) -> sumAll(b.getValue()).compareTo(sumAll(a.getValue())))
                    .limit(n)
                    .collect(Collectors.toList());

            List<LeaderboardItemDTO> items = new ArrayList<>();
            for (Map.Entry<Long, Map<String, BigDecimal>> entry : ranked) {
                Vendor vendor = vendorRepository.findById(entry.getKey()).orElse(null);
                if (vendor == null) continue;
                items.add(LeaderboardItemDTO.builder()
                        .id(idObfuscator.encodeId(vendor.getId()))
                        .code(vendor.getCode())
                        .name(vendor.getName())
                        .subtitle(vendor.getType() != null ? vendor.getType().getDisplayName() : null)
                        .count(vendorExpenseCount.getOrDefault(vendor.getId(), 0L))
                        .revenue(mapRevenue(entry.getValue()))
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Top vendors retrieved successfully", items));
        } catch (Exception e) {
            log.error("Error building top vendors", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build top vendors: " + e.getMessage(), "TOP_VENDORS_ERROR"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getSafariPnL(String safariIdObfuscated) {
        try {
            /*
             * Build the invoice index by safari id once (one query).
             *
             * Cancelled invoices are excluded, because SafariBillingService excludes them and
             * that is the figure the safari's own Money tab shows. When the two disagreed, the
             * dashboard reported USD 50,030.48 against a safari whose record page said
             * 26,304.76 — the difference being one cancelled invoice — and of the two numbers
             * the one on the record is the one people trust. A dashboard that contradicts the
             * record it summarises is worse than no dashboard.
             *
             * Note it is only CANCELLED that goes: a draft invoice still counts here, exactly
             * as it counts there. The rule is copied deliberately rather than improved on,
             * because the point is that the two agree.
             */
            Map<Long, List<Invoice>> invoicesBySafari = new HashMap<>();
            for (Invoice inv : invoiceRepository.findAll()) {
                if (inv.getSafari() == null) continue;
                if (inv.getStatus() == InvoiceStatus.CANCELLED) continue;
                invoicesBySafari.computeIfAbsent(inv.getSafari().getId(), k -> new ArrayList<>()).add(inv);
            }
            // Same for expenses (only those linked to a safari — operational expenses excluded)
            Map<Long, List<Expense>> expensesBySafari = new HashMap<>();
            for (Expense exp : expenseRepository.findAll()) {
                if (exp.getSafari() != null) {
                    expensesBySafari.computeIfAbsent(exp.getSafari().getId(), k -> new ArrayList<>()).add(exp);
                }
            }

            List<SafariPnLDTO> result = new ArrayList<>();

            if (safariIdObfuscated != null && !safariIdObfuscated.isBlank()) {
                Long safariId = idObfuscator.decodeId(safariIdObfuscated);
                Safari s = safariRepository.findById(safariId).orElse(null);
                if (s == null) {
                    return ResponseEntity.status(404).body(
                            ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND"));
                }
                result.add(buildSafariPnL(s,
                        invoicesBySafari.getOrDefault(safariId, List.of()),
                        expensesBySafari.getOrDefault(safariId, List.of())));
            } else {
                Set<Long> ids = new HashSet<>();
                ids.addAll(invoicesBySafari.keySet());
                ids.addAll(expensesBySafari.keySet());
                for (Long sid : ids) {
                    Safari s = safariRepository.findById(sid).orElse(null);
                    if (s == null) continue;
                    result.add(buildSafariPnL(s,
                            invoicesBySafari.getOrDefault(sid, List.of()),
                            expensesBySafari.getOrDefault(sid, List.of())));
                }
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Safari P&L retrieved successfully", result));
        } catch (Exception e) {
            log.error("Error building safari P&L", e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to build safari P&L: " + e.getMessage(), "SAFARI_PNL_ERROR"));
        }
    }

    // =========================================================================
    // Internal helpers (expense side)
    // =========================================================================

    private SafariPnLDTO buildSafariPnL(Safari safari, List<Invoice> invoices, List<Expense> expenses) {
        Map<String, BigDecimal> revenue = new HashMap<>();
        for (Invoice inv : invoices) {
            if (inv.getGrandTotals() == null) continue;
            for (Price gt : inv.getGrandTotals()) {
                if (gt.getCurrency() == null) continue;
                BigDecimal v = gt.getTotalPrice() != null ? gt.getTotalPrice() : BigDecimal.ZERO;
                revenue.merge(gt.getCurrency(), v, BigDecimal::add);
            }
        }
        Map<String, BigDecimal> expensesByCurrency = new HashMap<>();
        for (Expense exp : expenses) {
            if (exp.getGrandTotals() == null) continue;
            if (exp.getStatus() == ExpenseStatus.CANCELLED) continue;
            for (Price gt : exp.getGrandTotals()) {
                if (gt.getCurrency() == null) continue;
                BigDecimal v = gt.getTotalPrice() != null ? gt.getTotalPrice() : BigDecimal.ZERO;
                expensesByCurrency.merge(gt.getCurrency(), v, BigDecimal::add);
            }
        }

        // Net per currency: revenue − expenses. Currencies present in either side appear.
        Map<String, BigDecimal> net = new LinkedHashMap<>();
        Set<String> currencies = new LinkedHashSet<>();
        currencies.addAll(revenue.keySet());
        currencies.addAll(expensesByCurrency.keySet());
        for (String c : currencies) {
            BigDecimal r = revenue.getOrDefault(c, BigDecimal.ZERO);
            BigDecimal e = expensesByCurrency.getOrDefault(c, BigDecimal.ZERO);
            net.put(c, r.subtract(e));
        }

        return SafariPnLDTO.builder()
                .safariId(idObfuscator.encodeId(safari.getId()))
                .safariCode(safari.getCode())
                .safariName(safari.getName())
                .customerName(safari.getCustomer() != null ? safari.getCustomer().getDisplayName() : null)
                .state(safari.getState() != null ? safari.getState().name() : null)
                .revenue(mapRevenue(revenue))
                .expenses(mapRevenue(expensesByCurrency))
                .net(mapRevenue(net))
                .build();
    }

    private List<ExpensePayment> filterPayments(List<ExpensePayment> all, LocalDate fromInclusive, LocalDate toInclusive) {
        return all.stream()
                .filter(p -> p.getPaymentDate() != null
                        && !p.getPaymentDate().isBefore(fromInclusive)
                        && !p.getPaymentDate().isAfter(toInclusive))
                .collect(Collectors.toList());
    }

    private List<RevenueByCurrency> aggregateExpensePaymentsByCurrency(List<ExpensePayment> payments) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (ExpensePayment p : payments) {
            String currency = p.getExpenseCurrency() != null ? p.getExpenseCurrency() : p.getCurrency();
            BigDecimal amount = p.getBaseAmount() != null ? p.getBaseAmount() : p.getAmount();
            if (currency != null && amount != null) {
                map.merge(currency, amount, BigDecimal::add);
            }
        }
        return mapRevenue(map);
    }

    private List<ExpenseStatsDTO.RecentExpensePaymentDTO> mapRecentExpensePayments(List<ExpensePayment> payments) {
        return payments.stream().map(p -> ExpenseStatsDTO.RecentExpensePaymentDTO.builder()
                .id(idObfuscator.encodeId(p.getId()))
                .expenseCode(p.getExpense() != null ? p.getExpense().getExpenseCode() : null)
                .vendorName(p.getExpense() != null && p.getExpense().getVendor() != null
                        ? p.getExpense().getVendor().getName() : null)
                .safariCode(p.getExpense() != null && p.getExpense().getSafari() != null
                        ? p.getExpense().getSafari().getCode() : null)
                .method(p.getPaymentMethod() != null ? p.getPaymentMethod().getDisplayName() : null)
                .currency(p.getCurrency())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate() != null ? p.getPaymentDate().toString() : null)
                .build()).collect(Collectors.toList());
    }
}
