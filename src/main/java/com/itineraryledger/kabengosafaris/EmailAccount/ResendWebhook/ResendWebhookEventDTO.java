package com.itineraryledger.kabengosafaris.EmailAccount.ResendWebhook;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendWebhookEventDTO {
    private String id;
    private String svixId;
    private String eventType;
    private String emailId;
    private String fromEmail;
    private String toEmail;
    private String subject;
    private LocalDateTime eventTimestamp;
    private LocalDateTime receivedAt;
}
