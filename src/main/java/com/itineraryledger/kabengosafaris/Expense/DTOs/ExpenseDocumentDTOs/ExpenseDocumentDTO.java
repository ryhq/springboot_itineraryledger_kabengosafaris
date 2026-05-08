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

    private String title;
    private ExpenseDocument.DocumentType documentType;
    private String documentTypeDisplayName;

    private String fileUrl;
    private String fileName;
    private String originalFileName;
    private Long fileSize;
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
