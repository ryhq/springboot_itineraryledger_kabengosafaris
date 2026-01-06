package com.itineraryledger.kabengosafaris.ParkActivity.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for bulk upsert operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParkActivityUpsertResponseDTO {
    private int totalProcessed;
    private int successful;
    private int failed;
    private List<String> errors = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
    }
}
