package com.itineraryledger.kabengosafaris.Expense.Specifications;

import java.time.LocalDateTime;
import java.util.List;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument.DocumentType;

import lombok.Data;

/**
 * Everything a caller can narrow the expense-document list by, in one object.
 *
 * Bound from the query string with {@code @ModelAttribute}, and shared by the
 * rows, the stat cards and the record walk — so a card cannot report a figure the
 * table would contradict.
 */
@Data
public class ExpenseDocumentFilter {

    /** Free text: title, notes, either filename, their number, the bill, the vendor. */
    private String keyword;

    private String title;
    private String version;

    private DocumentType documentType;
    private List<DocumentType> documentTypes;

    /** Obfuscated ids, as the list page sends them. */
    private String expenseId;
    private String vendorId;
    private String safariId;

    /** True for a slip filed against a payment; false for what the supplier sent. */
    private Boolean isProofOfPayment;

    private Boolean isActive;
    private List<String> statuses;

    private Boolean currentlyValid;
    /** expired · expiring · no-expiry */
    private List<String> validity;

    private LocalDateTime createdAfter;
}
