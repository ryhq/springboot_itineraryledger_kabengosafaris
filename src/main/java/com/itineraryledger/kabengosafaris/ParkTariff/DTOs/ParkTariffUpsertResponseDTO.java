package com.itineraryledger.kabengosafaris.ParkTariff.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for bulk upsert operations
 *
 * Provides detailed feedback on:
 * - Total operations processed
 * - Successful operations count
 * - Failed operations count
 * - List of error messages for failures
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParkTariffUpsertResponseDTO {
    private int totalProcessed;
    private int successful;
    private int failed;
    private List<String> errors = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
    }
}
