package com.itineraryledger.kabengosafaris.Dashboard.DTOs;

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
public class PipelineStageDTO {
    private String state;
    private String displayName;
    private String group;
    private Long count;
    private List<RevenueByCurrency> projectedRevenue;
}
