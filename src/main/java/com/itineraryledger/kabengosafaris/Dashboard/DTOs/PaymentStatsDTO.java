package com.itineraryledger.kabengosafaris.Dashboard.DTOs;

import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RevenueByCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatsDTO {

    private List<RevenueByCurrency> collectedToday;
    private List<RevenueByCurrency> collectedThisWeek;
    private List<RevenueByCurrency> collectedThisMonth;
    private List<RevenueByCurrency> collectedThisYear;

    private Long totalPayments;
    private Long paymentsToday;
    private Long paymentsThisWeek;
    private Long paymentsThisMonth;

    private Map<String, Long> byMethod;
    private Map<String, List<RevenueByCurrency>> revenueByMethod;

    private List<RecentPaymentDTO> recentPayments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentPaymentDTO {
        private String id;
        private String invoiceCode;
        private String customerName;
        private String method;
        private String currency;
        private java.math.BigDecimal amount;
        private String paymentDate;
    }
}
