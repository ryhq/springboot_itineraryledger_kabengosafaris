package com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDocumentDTOs;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseDocumentDTO {

    private String id;
    private String expenseId;
    private String expenseCode;
    private String expensePaymentId;        // nullable

    /**
     * The payment this document is proof OF, in words: "USD 184.00 · 2026-08-20".
     *
     * A document tied to a payment proves that transfer; one tied to the bill alone is what the
     * supplier asked for in the first place. That distinction is the reason the link exists, so it
     * has to survive into any list that shows these — and an obfuscated payment id cannot say it.
     */
    private String expensePaymentLabel;    // nullable
    /* who the bill was from and which trip it was for — a global list is
       unreadable without them */
    private String vendorId;
    private String vendorName;
    private String safariId;
    private String safariName;
    private String expenseTitle;

    private String title;
    private ExpenseDocument.DocumentType documentType;
    private String documentTypeDisplayName;

    private String fileUrl;

    /**
     * The same two names every other document module answers with.
     *
     * The shared documents table reads `fileDocumentUrl` for the preview and `fileSizeFormatted`
     * for the Size column; this module answered `fileUrl` and a raw byte count, so a bill's
     * documents tab showed no preview eye and an empty Size while a safari's showed both. The
     * table is generated from one contract — a module that speaks a dialect of it silently loses
     * whichever parts it did not say.
     */
    private String fileDocumentUrl;
    private String documentUrl;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
    private String fileSizeFormatted;
    private String fileType;

    private String description;
    private String documentNumber;
    private String version;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private Boolean isActive;
    private Boolean isCurrentlyValid;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
