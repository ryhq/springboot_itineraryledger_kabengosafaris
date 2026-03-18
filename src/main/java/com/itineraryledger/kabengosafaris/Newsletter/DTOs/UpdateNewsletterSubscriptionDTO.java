package com.itineraryledger.kabengosafaris.Newsletter.DTOs;

import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateNewsletterSubscriptionDTO {

    private String name;
    private SubscriptionStatus status;
    private String preferredLocale;
}
