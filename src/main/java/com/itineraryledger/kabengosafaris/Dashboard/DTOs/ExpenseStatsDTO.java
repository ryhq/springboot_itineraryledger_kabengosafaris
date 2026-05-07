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
public class ExpenseStatsDTO {

    private List<RevenueByCurrency> spentToday;
    private List<RevenueByCurrency> spentThisWeek;
    private List<RevenueByCurrency> spentThisMonth;
    private List<RevenueByCurrency> spentThisYear;

    private Long totalExpenses;
    private Long totalPayments;
    private Long paymentsToday;
    private Long paymentsThisWeek;
    private Long paymentsThisMonth;

    /** Sum of unpaid balances across all unpaid expenses, per currency. */
    private List<RevenueByCurrency> outstandingByCurrency;

    /** Count of expenses by status: { DRAFT: 3, RECORDED: 12, PARTIALLY_PAID: 4, PAID: 22, CANCELLED: 1 }. */
    private Map<String, Long> expensesByStatus;

    /** Total spend per ExpenseCategory, per currency. */
    private Map<String, List<RevenueByCurrency>> spendByCategory;

    /** Count of payments per PaymentMethod. */
    private Map<String, Long> paymentsByMethod;

    private List<RecentExpensePaymentDTO> recentPayments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentExpensePaymentDTO {
        private String id;
        private String expenseCode;
        private String vendorName;
        private String safariCode;
        private String method;
        private String currency;
        private java.math.BigDecimal amount;
        private String paymentDate;
    }
}
