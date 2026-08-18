package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Pointing a request at the message that answered it.
 *
 * By hand, for the reply that arrives from a different address with a fresh subject and therefore
 * matches no header. Automatic matching handles the rest; this is the honest escape hatch rather
 * than a heuristic that guesses wrong quietly.
 */
@Data
public class LinkReplyDTO {

    @NotBlank
    private String messageId;
}
