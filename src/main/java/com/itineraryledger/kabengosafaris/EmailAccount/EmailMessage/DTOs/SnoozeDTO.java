package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SnoozeDTO {
    /** ISO timestamp to snooze until. Must be in the future. */
    @NotNull
    private LocalDateTime snoozeUntil;

    /** Used by the batch endpoint only. */
    private List<String> messageIds;
}
