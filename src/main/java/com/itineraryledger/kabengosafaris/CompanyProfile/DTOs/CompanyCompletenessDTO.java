package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What is still missing, and what it costs.
 *
 * A blank TIN is not a validation error — the app runs fine without it — but an invoice prints a
 * blank line where the tax number belongs, and nobody notices until a client asks. So the gaps are
 * reported as a list with a severity rather than enforced as required fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyCompletenessDTO {

    private int filled;
    private int total;
    /** 0-100, for the progress readout. */
    private int percent;
    private boolean readyForDocuments;
    private List<GapDTO> gaps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GapDTO {
        private String key;
        private String label;
        /** BLOCKING = a document or email prints a blank; RECOMMENDED = merely thin. */
        private String severity;
        private String consequence;
        /** which panel of the Company page fixes it: IDENTITY, EMAILS, PHONES, ADDRESSES, LINKS, ASSETS, BANK. */
        private String section;
    }
}
