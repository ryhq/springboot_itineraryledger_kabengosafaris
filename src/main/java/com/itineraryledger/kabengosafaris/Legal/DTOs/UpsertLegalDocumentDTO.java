package com.itineraryledger.kabengosafaris.Legal.DTOs;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** What the panel sends when writing a notice. */
@Data
public class UpsertLegalDocumentDTO {

    /** PRIVACY_POLICY or TERMS. Immutable once created. */
    private String documentType;

    @Size(max = 30)
    private String version;

    @Size(max = 200)
    private String title;

    /**
     * The notice, as text.
     *
     * <p>Not HTML. See {@code LegalDocument.body}: this is rendered on two public websites and
     * inside the portal, and there is no sanitiser on this side.
     */
    @NotBlank
    private String body;

    private LocalDate effectiveFrom;

    /** Publishing this version unpublishes whichever one was live. */
    private Boolean published;
}
