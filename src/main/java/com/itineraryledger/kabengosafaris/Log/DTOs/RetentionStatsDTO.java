package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * DTO for log retention statistics
 *
 * Provides information about active and archived logs,
 * their counts, sizes, and retention dates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetentionStatsDTO {

    /**
     * Number of active (non-archived) log files
     */
    private Integer activeLogCount;

    /**
     * Number of archived (.gz) log files
     */
    private Integer archivedLogCount;

    /**
     * Total size of active logs in bytes
     */
    private Long activeSizeBytes;

    /**
     * Total size of archived logs in bytes
     */
    private Long archivedSizeBytes;

    /**
     * Date of the oldest active log file
     */
    private LocalDate oldestActiveLog;

    /**
     * Date of the oldest archived log file
     */
    private LocalDate oldestArchivedLog;
}
