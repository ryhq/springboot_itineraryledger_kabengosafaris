package com.itineraryledger.kabengosafaris.Dashboard.DTOs;

import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RevenueByCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Per-safari (or aggregate) profit/loss view: revenue from invoice grand totals
 * minus expense grand totals, kept per currency without FX consolidation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafariPnLDTO {
    private String safariId;
    private String safariCode;
    private String safariName;
    private String customerName;
    private String state;

    private List<RevenueByCurrency> revenue;     // sum of invoice.grandTotals across linked invoices
    private List<RevenueByCurrency> expenses;    // sum of expense.grandTotals across linked expenses
    private List<RevenueByCurrency> net;         // revenue - expenses, per currency
}
