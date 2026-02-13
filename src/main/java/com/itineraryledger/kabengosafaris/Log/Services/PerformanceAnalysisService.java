package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for analyzing request performance
 *
 * Analyzes:
 * - Slow requests (exceeding threshold)
 * - Performance grading (A-F)
 * - Response time categorization
 */
@Service
@Slf4j
public class PerformanceAnalysisService {

    @Autowired
    private AccessLogSettingGetterServices settings;

    /**
     * Analyze request performance
     * Enriches the DTO with performance analysis results
     *
     * @param dto the access log DTO to analyze
     */
    public void analyze(AccessLogDTO dto) {
        if (!settings.isPerformanceMonitoringEnabled()) {
            return;
        }

        Long timeTakenMillis = dto.getTimeTakenMillis();
        if (timeTakenMillis == null) {
            return;
        }

        // Check if request is slow
        Long slowThreshold = settings.getSlowRequestThreshold();
        boolean isSlowRequest = timeTakenMillis > slowThreshold;
        dto.setIsSlowRequest(isSlowRequest);

        if (isSlowRequest) {
            log.warn("Slow request detected: {} took {}ms (threshold: {}ms) - {}",
                dto.getRequestUri(), timeTakenMillis, slowThreshold, dto.getRemoteAddress());
        }

        // Assign performance grade
        String grade = calculatePerformanceGrade(timeTakenMillis);
        dto.setPerformanceGrade(grade);
    }

    /**
     * Calculate performance grade based on response time
     *
     * Grading scale:
     * - A: < 100ms (Excellent)
     * - B: 100-500ms (Good)
     * - C: 500-2000ms (Acceptable)
     * - D: 2000-5000ms (Poor)
     * - F: > 5000ms (Unacceptable)
     *
     * @param timeTakenMillis response time in milliseconds
     * @return performance grade (A-F)
     */
    private String calculatePerformanceGrade(Long timeTakenMillis) {
        if (timeTakenMillis < 100) {
            return "A";
        } else if (timeTakenMillis < 500) {
            return "B";
        } else if (timeTakenMillis < 2000) {
            return "C";
        } else if (timeTakenMillis < 5000) {
            return "D";
        } else {
            return "F";
        }
    }
}
