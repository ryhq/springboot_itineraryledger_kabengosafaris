package com.itineraryledger.kabengosafaris.Quote.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Quote responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteDTO {

    private String id;
    private String quoteCode;
    private String title;
    private String description;

    // Relationships
    private String itineraryId;
    private String itineraryCode;
    private String itineraryName;

    private String customerId;
    private String customerName;
    private String customerEmail;

    // Multi-currency totals
    private List<Price> subtotals;
    private List<Price> taxes;
    private List<Price> discounts;
    private List<Price> grandTotals;

    // Pricing details
    private Boolean isStoRate;
    private BigDecimal taxPercentage;
    private BigDecimal discountPercentage;
    private String discountReason;

    // Versioning
    private Integer version;
    private String previousVersionId;
    private String previousVersionCode;
    private String nextVersionId;
    private String nextVersionCode;
    private String versionNotes;

    // Workflow
    private QuoteStatus status;
    private String statusDisplayName;
    private LocalDate sentDate;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean isValid;

    // Payment terms
    private BigDecimal depositPercentage;
    private LocalDate depositDueDate;
    private LocalDate fullPaymentDueDate;

    // Approval
    private String approverId;
    private String approverName;
    private String approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalNotes;

    // Additional
    private String internalNotes;
    private String customerNotes;
    private Boolean isActive;

    // Counts
    private Long itemCount;
    private Long documentCount;

    // Audit
    private String createdById;
    private String createdByName;
    private String updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
