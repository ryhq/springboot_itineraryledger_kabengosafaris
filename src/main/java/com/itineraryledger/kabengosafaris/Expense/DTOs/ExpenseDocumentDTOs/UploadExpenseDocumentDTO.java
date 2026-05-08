package com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDocumentDTOs;

import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Multipart upload — bound from form-data on POST /api/expenses/{expenseId}/documents.
 * Spring populates this from @ModelAttribute.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadExpenseDocumentDTO {

    /** The actual file part. Required. */
    private MultipartFile document;

    private String title;                      // Required; defaults to original filename if blank
    private ExpenseDocument.DocumentType documentType; // Required; defaults to RECEIPT
    private String description;
    private String documentNumber;             // Vendor's ref number printed on the receipt
    private String version;
    private String notes;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    /** Optional: tie the upload to a specific ExpensePayment (obfuscated id). */
    private String expensePaymentId;
}
