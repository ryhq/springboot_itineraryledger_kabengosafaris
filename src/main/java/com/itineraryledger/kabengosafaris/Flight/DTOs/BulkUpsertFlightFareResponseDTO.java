package com.itineraryledger.kabengosafaris.Flight.DTOs;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-row outcomes: a batch that fails tells you nothing about which of 350 rows was wrong. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUpsertFlightFareResponseDTO {
    private int totalProcessed;
    private int created;
    private int updated;
    private int failed;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
