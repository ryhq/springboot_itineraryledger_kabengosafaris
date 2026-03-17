package com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core;

import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.RateIssueLogDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.CostItemType;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.RateIssueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for logging rate issues during cost estimation.
 *
 * Request-scoped to ensure thread safety and isolation between requests.
 * Issues are accumulated during a cost estimation and can be retrieved at the end.
 */
@Service
@Slf4j
public class RateIssueLoggerService {

    private final List<RateIssueLogDTO> issues = new ArrayList<>();

    /**
     * Log a missing rate issue.
     *
     * @param itemType Type of item (ACCOMMODATION, PARK_FEE, ACTIVITY)
     * @param itemName Name of the item
     * @param itemId Obfuscated ID
     * @param dayNumber Day number where issue occurred
     * @param paxCategory Pax category description (if applicable)
     * @param seasonName Season name that was looked up
     */
    public void logMissingRate(
            CostItemType itemType,
            String itemName,
            String itemId,
            Integer dayNumber,
            String paxCategory,
            String seasonName
    ) {
        String message = buildMessage(RateIssueType.MISSING, itemType, itemName, paxCategory, seasonName);

        RateIssueLogDTO issue = RateIssueLogDTO.builder()
            .issueType(RateIssueType.MISSING)
            .itemType(itemType)
            .itemName(itemName)
            .itemId(itemId)
            .dayNumber(dayNumber)
            .paxCategory(paxCategory)
            .seasonName(seasonName)
            .message(message)
            .build();

        issues.add(issue);
        log.warn("Rate issue: {}", message);
    }

    /**
     * Log an inactive rate issue.
     *
     * @param itemType Type of item
     * @param itemName Name of the item
     * @param itemId Obfuscated ID
     * @param dayNumber Day number where issue occurred
     * @param paxCategory Pax category description (if applicable)
     * @param seasonName Season name that was looked up
     */
    public void logInactiveRate(
            CostItemType itemType,
            String itemName,
            String itemId,
            Integer dayNumber,
            String paxCategory,
            String seasonName
    ) {
        String message = buildMessage(RateIssueType.INACTIVE, itemType, itemName, paxCategory, seasonName);

        RateIssueLogDTO issue = RateIssueLogDTO.builder()
            .issueType(RateIssueType.INACTIVE)
            .itemType(itemType)
            .itemName(itemName)
            .itemId(itemId)
            .dayNumber(dayNumber)
            .paxCategory(paxCategory)
            .seasonName(seasonName)
            .message(message)
            .build();

        issues.add(issue);
        log.warn("Rate issue: {}", message);
    }

    /**
     * Log a missing STO rate issue.
     *
     * @param itemType Type of item
     * @param itemName Name of the item
     * @param itemId Obfuscated ID
     * @param dayNumber Day number where issue occurred
     * @param paxCategory Pax category description (if applicable)
     */
    public void logNoStoRate(
            CostItemType itemType,
            String itemName,
            String itemId,
            Integer dayNumber,
            String paxCategory
    ) {
        String message = String.format("No STO rate defined for %s '%s'%s",
            itemType.getDisplayName(),
            itemName,
            paxCategory != null ? " for " + paxCategory : ""
        );

        RateIssueLogDTO issue = RateIssueLogDTO.builder()
            .issueType(RateIssueType.NO_STO_RATE)
            .itemType(itemType)
            .itemName(itemName)
            .itemId(itemId)
            .dayNumber(dayNumber)
            .paxCategory(paxCategory)
            .message(message)
            .build();

        issues.add(issue);
        log.info("Rate issue: {}", message);
    }

    /**
     * Log when an accommodation has no seasons configured at all.
     * Cost calculation is skipped for this accommodation.
     *
     * @param itemName Name of the accommodation
     * @param itemId Obfuscated ID
     * @param dayNumber Day number where issue occurred
     */
    public void logNoSeason(
            String itemName,
            String itemId,
            Integer dayNumber
    ) {
        String message = String.format(
            "Accommodation '%s' has no seasons configured - cost calculation skipped",
            itemName
        );

        RateIssueLogDTO issue = RateIssueLogDTO.builder()
            .issueType(RateIssueType.NO_SEASON)
            .itemType(CostItemType.ACCOMMODATION)
            .itemName(itemName)
            .itemId(itemId)
            .dayNumber(dayNumber)
            .message(message)
            .build();

        issues.add(issue);
        log.warn("Rate issue: {}", message);
    }

    /**
     * Log when season for specific date was not found and fallback season was used.
     * This is informational - cost calculation proceeds with fallback season.
     *
     * @param itemName Name of the accommodation
     * @param itemId Obfuscated ID
     * @param dayNumber Day number where issue occurred
     * @param requestedDate The date for which season was not found
     * @param fallbackSeasonName Name of the fallback season used
     * @param fallbackSeasonType Type of the fallback season
     */
    public void logSeasonNotFound(
            String itemName,
            String itemId,
            Integer dayNumber,
            String requestedDate,
            String fallbackSeasonName,
            String fallbackSeasonType
    ) {
        String message = String.format(
            "No season found for accommodation '%s' on %s - using fallback '%s' (%s)",
            itemName,
            requestedDate,
            fallbackSeasonName,
            fallbackSeasonType
        );

        RateIssueLogDTO issue = RateIssueLogDTO.builder()
            .issueType(RateIssueType.SEASON_NOT_FOUND)
            .itemType(CostItemType.ACCOMMODATION)
            .itemName(itemName)
            .itemId(itemId)
            .dayNumber(dayNumber)
            .seasonName(fallbackSeasonName)
            .message(message)
            .build();

        issues.add(issue);
        log.info("Rate issue: {}", message);
    }

    /**
     * Get all accumulated issues.
     *
     * @return Unmodifiable list of issues
     */
    public List<RateIssueLogDTO> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    /**
     * Check if any issues have been logged.
     *
     * @return true if there are issues
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * Get the count of issues.
     *
     * @return Number of issues
     */
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * Clear all accumulated issues.
     * Call this at the start of a new cost estimation.
     */
    public void clear() {
        issues.clear();
    }

    /**
     * Build a human-readable message for a rate issue.
     */
    private String buildMessage(
            RateIssueType issueType,
            CostItemType itemType,
            String itemName,
            String paxCategory,
            String seasonName
    ) {
        StringBuilder sb = new StringBuilder();

        switch (issueType) {
            case MISSING:
                sb.append("No active rate found for ");
                break;
            case INACTIVE:
                sb.append("Rate is inactive for ");
                break;
            case NO_SEASON:
                sb.append("No seasons configured for ");
                break;
            case SEASON_NOT_FOUND:
                sb.append("No matching season for date, used fallback for ");
                break;
            default:
                sb.append("Rate issue for ");
        }

        sb.append(itemType.getDisplayName())
          .append(" '")
          .append(itemName)
          .append("'");

        if (paxCategory != null && !paxCategory.isEmpty()) {
            sb.append(" for ").append(paxCategory);
        }

        if (seasonName != null && !seasonName.isEmpty()) {
            sb.append(" in ").append(seasonName);
        }

        return sb.toString();
    }
}
