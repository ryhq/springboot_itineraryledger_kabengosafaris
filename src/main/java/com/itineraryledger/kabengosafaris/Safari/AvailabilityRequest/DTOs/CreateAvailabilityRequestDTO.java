package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * Recording an ask that has just gone out.
 *
 * Written AFTER the message is sent, never before: a request row for mail that failed to leave
 * would tell the next person the property has been chased when nobody has heard from us.
 */
@Data
public class CreateAvailabilityRequestDTO {

    @NotBlank
    private String accommodationId;

    /** Our sent copy — the thread this ask lives in. */
    private String emailMessageId;
    private String emailAccountId;

    private String toAddress;
    private List<String> ccAddresses;
    /** invisible to everyone on the message, which is precisely why it is recorded */
    private List<String> bccAddresses;
    private String subject;

    /** The stay rows this covered — obfuscated ids of safari_day_accommodation. */
    @NotEmpty
    private List<String> stayIds;

    private String notes;
}
