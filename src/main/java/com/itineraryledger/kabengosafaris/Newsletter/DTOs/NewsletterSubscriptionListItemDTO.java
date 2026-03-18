package com.itineraryledger.kabengosafaris.Newsletter.DTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterSubscriptionListItemDTO {

    private String id;
    private String email;
    private String name;
    private SubscriptionStatus status;
    private String statusDisplayName;
    private String source;
    private String customerName;
    private LocalDateTime subscribedAt;
}
