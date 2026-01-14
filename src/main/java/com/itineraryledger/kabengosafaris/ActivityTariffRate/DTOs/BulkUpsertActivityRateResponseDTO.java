package com.itineraryledger.kabengosafaris.ActivityTariffRate.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * BulkUpsertActivityRateResponseDTO - Response DTO for bulk upsert operations
 *
 * Provides detailed feedback on:
 * - Total operations processed
 * - Successful operations count (created + updated)
 * - Failed operations count
 * - List of error messages for failures
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkUpsertActivityRateResponseDTO {
    private int totalProcessed;
    private int created;
    private int updated;
    private int failed;
    private List<String> errors = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
    }

    public int getSuccessful() {
        return created + updated;
    }
}
