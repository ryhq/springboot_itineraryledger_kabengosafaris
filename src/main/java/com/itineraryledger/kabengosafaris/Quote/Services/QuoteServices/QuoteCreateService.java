package com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices;

import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Quote.DTOs.CreateQuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDTO;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteTotalsCalculationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating quotes
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class QuoteCreateService {

    private final QuoteRepository quoteRepository;
    private final ItineraryRepository itineraryRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final QuoteTotalsCalculationService totalsCalculationService;

    @AuditLogAnnotation(
        action = "CREATE_QUOTE",
        description = "Creating a new quote",
        entityType = "Quote"
    )
    public ResponseEntity<ApiResponse<?>> createQuote(CreateQuoteDTO createDTO) {
        log.info("Creating new quote");

        try {
            // Validate and decode itinerary ID
            Long itineraryId;
            try {
                itineraryId = idObfuscator.decodeId(createDTO.getItineraryId());
            } catch (Exception e) {
                log.warn("Failed to decode itinerary ID: {}", createDTO.getItineraryId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                );
            }

            Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
            if (itinerary == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Itinerary not found", "ITINERARY_NOT_FOUND")
                );
            }

            // Validate and decode customer ID
            Long customerId;
            try {
                customerId = idObfuscator.decodeId(createDTO.getCustomerId());
            } catch (Exception e) {
                log.warn("Failed to decode customer ID: {}", createDTO.getCustomerId(), e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Customer not found", "CUSTOMER_NOT_FOUND")
                );
            }

            // Decode approver ID if provided
            User approver = null;
            if (createDTO.getApproverId() != null && !createDTO.getApproverId().isBlank()) {
                try {
                    Long approverId = idObfuscator.decodeId(createDTO.getApproverId());
                    approver = userRepository.findById(approverId).orElse(null);
                    if (approver == null) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Approver not found", "APPROVER_NOT_FOUND")
                        );
                    }
                } catch (Exception e) {
                    log.warn("Failed to decode approver ID: {}", createDTO.getApproverId(), e);
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid approver ID", "INVALID_APPROVER_ID")
                    );
                }
            }

            // Validate date fields
            LocalDate today = LocalDate.now();

            // Validate validFrom is not in the past
            if (createDTO.getValidFrom().isBefore(today)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Valid from date cannot be in the past", "INVALID_VALID_FROM_DATE")
                );
            }

            // Validate validTo is not in the past
            if (createDTO.getValidTo().isBefore(today)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Valid to date cannot be in the past", "INVALID_VALID_TO_DATE")
                );
            }

            // Validate validFrom is earlier than validTo
            if (!createDTO.getValidFrom().isBefore(createDTO.getValidTo())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Valid from date must be earlier than valid to date", "INVALID_DATE_RANGE")
                );
            }

            // Get current user for audit tracking
            User currentUser = getCurrentUser();

            // Build quote entity (quoteCode will be set after save)
            Quote quote = Quote.builder()
                .quoteCode("TEMP") // Temporary code, will be updated after save
                .title(createDTO.getTitle())
                .description(createDTO.getDescription())
                .itinerary(itinerary)
                .customer(customer)
                .items(new ArrayList<>())
                .documents(new ArrayList<>())
                .subtotals(new ArrayList<>())
                .taxes(new ArrayList<>())
                .discounts(new ArrayList<>())
                .grandTotals(new ArrayList<>())
                .isStoRate(createDTO.getIsStoRate() != null ? createDTO.getIsStoRate() : true)
                .taxPercentage(createDTO.getTaxPercentage())
                .discountPercentage(createDTO.getDiscountPercentage())
                .discountReason(createDTO.getDiscountReason())
                .version(1)
                .status(QuoteStatus.DRAFT)
                .validFrom(createDTO.getValidFrom())
                .validTo(createDTO.getValidTo())
                .isValid(true)
                .depositPercentage(createDTO.getDepositPercentage())
                .depositDueDate(createDTO.getDepositDueDate())
                .fullPaymentDueDate(createDTO.getFullPaymentDueDate())
                .approver(approver)
                .createdBy(currentUser)
                .internalNotes(createDTO.getInternalNotes())
                .customerNotes(createDTO.getCustomerNotes())
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .build();

            // Save quote to get ID
            quote = quoteRepository.save(quote);

            // Generate quote code based on ID
            String quoteCode = quote.generateCode();
            quote.setQuoteCode(quoteCode);

            // Save again with the generated code
            quote = quoteRepository.save(quote);

            // Calculate and update totals (will be zero for new quote with no items)
            totalsCalculationService.recalculateTotals(quote.getId());

            // Reload quote to get updated totals
            quote = quoteRepository.findById(quote.getId()).orElse(quote);

            log.info("Quote created successfully with code: {}", quoteCode);

            // Convert to DTO
            QuoteDTO quoteDTO = convertToDTO(quote);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Quote created successfully", quoteDTO)
            );

        } catch (Exception e) {
            log.error("Error creating quote", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create quote", "QUOTE_CREATE_FAILED")
            );
        }
    }

    /**
     * Convert Quote entity to QuoteDTO
     */
    public QuoteDTO convertToDTO(Quote quote) {
        QuoteDTO dto = QuoteDTO.builder()
            .id(idObfuscator.encodeId(quote.getId()))
            .quoteCode(quote.getQuoteCode())
            .title(quote.getTitle())
            .description(quote.getDescription())
            .itineraryId(idObfuscator.encodeId(quote.getItinerary().getId()))
            .itineraryCode(quote.getItinerary().getCode())
            .itineraryName(quote.getItinerary().getName())
            .customerId(idObfuscator.encodeId(quote.getCustomer().getId()))
            .customerName(quote.getCustomer().getDisplayName())
            .customerEmail(quote.getCustomer().getPrimaryEmail())
            .subtotals(quote.getSubtotals())
            .taxes(quote.getTaxes())
            .discounts(quote.getDiscounts())
            .grandTotals(quote.getGrandTotals())
            .isStoRate(quote.getIsStoRate())
            .taxPercentage(quote.getTaxPercentage())
            .discountPercentage(quote.getDiscountPercentage())
            .discountReason(quote.getDiscountReason())
            .version(quote.getVersion())
            .status(quote.getStatus())
            .sentDate(quote.getSentDate())
            .validFrom(quote.getValidFrom())
            .validTo(quote.getValidTo())
            .isValid(quote.getIsValid())
            .depositPercentage(quote.getDepositPercentage())
            .depositDueDate(quote.getDepositDueDate())
            .fullPaymentDueDate(quote.getFullPaymentDueDate())
            .internalNotes(quote.getInternalNotes())
            .customerNotes(quote.getCustomerNotes())
            .versionNotes(quote.getVersionNotes())
            .isActive(quote.getIsActive())
            .itemCount(quote.getItems() != null ? (long) quote.getItems().size() : 0L)
            .documentCount(quote.getDocuments() != null ? (long) quote.getDocuments().size() : 0L)
            .createdAt(quote.getCreatedAt())
            .updatedAt(quote.getUpdatedAt())
            .build();

        // Set approver if present
        if (quote.getApprover() != null) {
            dto.setApproverId(idObfuscator.encodeId(quote.getApprover().getId()));
            dto.setApproverName(quote.getApprover().getUsername());
        }

        // Set approved by if present
        if (quote.getApprovedBy() != null) {
            dto.setApprovedById(idObfuscator.encodeId(quote.getApprovedBy().getId()));
            dto.setApprovedByName(quote.getApprovedBy().getUsername());
            dto.setApprovedAt(quote.getApprovedAt());
            dto.setApprovalNotes(quote.getApprovalNotes());
        }

        // Set previous version if present
        if (quote.getPreviousVersion() != null) {
            dto.setPreviousVersionId(idObfuscator.encodeId(quote.getPreviousVersion().getId()));
            dto.setPreviousVersionCode(quote.getPreviousVersion().getQuoteCode());
        }

        // Set next version if present
        if (quote.getNextVersion() != null) {
            dto.setNextVersionId(idObfuscator.encodeId(quote.getNextVersion().getId()));
            dto.setNextVersionCode(quote.getNextVersion().getQuoteCode());
        }

        // Set created by if present
        if (quote.getCreatedBy() != null) {
            dto.setCreatedById(idObfuscator.encodeId(quote.getCreatedBy().getId()));
            dto.setCreatedByName(quote.getCreatedBy().getUsername());
        }

        // Set updated by if present
        if (quote.getUpdatedBy() != null) {
            dto.setUpdatedById(idObfuscator.encodeId(quote.getUpdatedBy().getId()));
            dto.setUpdatedByName(quote.getUpdatedBy().getUsername());
        }

        return dto;
    }

    /**
     * Get the current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User user = (User) principal;
                // Fetch from repository to ensure it's a managed entity
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
