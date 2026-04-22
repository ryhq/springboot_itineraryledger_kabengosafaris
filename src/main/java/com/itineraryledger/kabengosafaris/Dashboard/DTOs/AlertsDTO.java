package com.itineraryledger.kabengosafaris.Dashboard.DTOs;

import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RecentActivityDTO;
import com.itineraryledger.kabengosafaris.Dashboard.DTOs.DashboardStatsDTO.RevenueByCurrency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertsDTO {

    private OverdueAgingDTO overdueAging;
    private List<RecentActivityDTO> expiringQuotes;
    private List<RecentActivityDTO> unpaidUpcomingSafaris;
    private List<RecentActivityDTO> safarisReadyToStart;
    private List<RecentActivityDTO> blacklistedCustomers;
    private Long totalAlerts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverdueAgingDTO {
        private List<RevenueByCurrency> zeroToThirty;
        private List<RevenueByCurrency> thirtyOneToSixty;
        private List<RevenueByCurrency> sixtyOnePlus;
        private Long invoiceCount0_30;
        private Long invoiceCount31_60;
        private Long invoiceCount61Plus;
    }
}
