package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailabilityRequestDTO {

    private String id;
    private String safariId;
    private String safariCode;
    private String accommodationId;
    private String accommodationName;

    private String status;
    private String closedReason;

    private String emailMessageId;
    private String emailAccountId;
    private String threadId;
    private String toAddress;
    private List<String> ccAddresses;
    private List<String> bccAddresses;
    private String subject;

    private LocalDateTime sentAt;
    private String sentByName;
    private LocalDateTime chaseDueAt;
    /** true once it is past its chase date with no reply — computed, never stored stale */
    private Boolean chaseDue;
    private LocalDateTime repliedAt;
    private String replyMessageId;
    private LocalDateTime closedAt;
    private String notes;

    /** the nights it asked about */
    private List<Night> nights;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Night {
        private String stayId;
        private String safariDayId;
        private Integer dayNumber;
        private LocalDate nightDate;
        /** false when the stay row has since been removed from the safari */
        private Boolean stayStillOnSafari;
    }
}
