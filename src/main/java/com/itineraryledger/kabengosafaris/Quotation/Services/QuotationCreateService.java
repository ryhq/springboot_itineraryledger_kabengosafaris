package com.itineraryledger.kabengosafaris.Quotation.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Quotation.DTOs.*;
import com.itineraryledger.kabengosafaris.Quotation.Entity.Quotation;
import com.itineraryledger.kabengosafaris.Quotation.Entity.QuotationLineItem;
import com.itineraryledger.kabengosafaris.Quotation.Entity.QuotationPax;
import com.itineraryledger.kabengosafaris.Quotation.Enums.DiscountType;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import com.itineraryledger.kabengosafaris.Quotation.Repository.QuotationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * QuotationCreateService - Service for creating quotations
 */
@Service
@Slf4j
@Transactional
public class QuotationCreateService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final ItineraryRepository itineraryRepository;
    private final PaxNationCategoryRepository nationCategoryRepository;
    private final PaxAgeCategoryRepository ageCategoryRepository;
    private final QuotationGetService quotationGetService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuotationCreateService(
        QuotationRepository quotationRepository,
        CustomerRepository customerRepository,
        ItineraryRepository itineraryRepository,
        PaxNationCategoryRepository nationCategoryRepository,
        PaxAgeCategoryRepository ageCategoryRepository,
        QuotationGetService quotationGetService,
        IdObfuscator idObfuscator
    ) {
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
        this.itineraryRepository = itineraryRepository;
        this.nationCategoryRepository = nationCategoryRepository;
        this.ageCategoryRepository = ageCategoryRepository;
        this.quotationGetService = quotationGetService;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Create a new quotation
     */
    @AuditLogAnnotation(
        action = "CREATE_QUOTATION",
        description = "Creating a new quotation",
        entityType = "Quotation"
    )
    public ResponseEntity<ApiResponse<?>> createQuotation(CreateQuotationDTO createDTO) {
        log.info("Creating new quotation: {}", createDTO.getName());

        try {
            // Validate and decode customer ID
            Long customerId;
            try {
                customerId = idObfuscator.decodeId(createDTO.getCustomerId());
            } catch (Exception e) {
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

            // Validate customer can receive quotations
            if (!customer.canBook()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Customer is not eligible for quotations", "CUSTOMER_NOT_ELIGIBLE")
                );
            }

            // Get current user
            Long currentUserId = getCurrentUserId();

            // Build quotation entity
            Quotation quotation = Quotation.builder()
                .code("TEMP") // Will be generated after save
                .name(createDTO.getName())
                .customer(customer)
                .status(QuotationStatus.DRAFT)
                .version(1)
                .startDate(createDTO.getStartDate())
                .endDate(createDTO.getEndDate())
                .totalDays(createDTO.getTotalDays())
                .totalNights(createDTO.getTotalNights())
                .currency(createDTO.getCurrency() != null ? createDTO.getCurrency() : "USD")
                .exchangeRate(createDTO.getExchangeRate() != null ? createDTO.getExchangeRate() : BigDecimal.ONE)
                .discountType(createDTO.getDiscountType() != null ? createDTO.getDiscountType() : DiscountType.NONE)
                .discountValue(createDTO.getDiscountValue() != null ? createDTO.getDiscountValue() : BigDecimal.ZERO)
                .discountReason(createDTO.getDiscountReason())
                .taxRate(createDTO.getTaxRate() != null ? createDTO.getTaxRate() : BigDecimal.ZERO)
                .depositPercentage(createDTO.getDepositPercentage() != null ? createDTO.getDepositPercentage() : new BigDecimal("50.00"))
                .validUntil(createDTO.getValidUntil())
                .termsAndConditions(createDTO.getTermsAndConditions())
                .inclusions(createDTO.getInclusions())
                .exclusions(createDTO.getExclusions())
                .internalNotes(createDTO.getInternalNotes())
                .customerNotes(createDTO.getCustomerNotes())
                .createdBy(currentUserId)
                .build();

            // Handle itinerary reference
            if (createDTO.getItineraryId() != null && !createDTO.getItineraryId().isEmpty()) {
                try {
                    Long itineraryId = idObfuscator.decodeId(createDTO.getItineraryId());
                    Itinerary itinerary = itineraryRepository.findById(itineraryId).orElse(null);
                    if (itinerary != null) {
                        quotation.setItinerary(itinerary);
                        // Copy days/nights from itinerary if not provided
                        if (quotation.getTotalDays() == null) {
                            quotation.setTotalDays(itinerary.getTotalDays());
                        }
                        if (quotation.getTotalNights() == null) {
                            quotation.setTotalNights(itinerary.getTotalNights());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to decode itinerary ID: {}", createDTO.getItineraryId());
                }
            }

            // Handle assigned to
            if (createDTO.getAssignedToId() != null && !createDTO.getAssignedToId().isEmpty()) {
                try {
                    Long assignedToId = idObfuscator.decodeId(createDTO.getAssignedToId());
                    quotation.setAssignedTo(assignedToId);
                } catch (Exception e) {
                    log.warn("Failed to decode assigned to ID: {}", createDTO.getAssignedToId());
                }
            } else {
                // Default to current user
                quotation.setAssignedTo(currentUserId);
            }

            // Calculate end date if not provided
            if (quotation.getEndDate() == null && quotation.getStartDate() != null && quotation.getTotalDays() != null) {
                quotation.setEndDate(quotation.getStartDate().plusDays(quotation.getTotalDays() - 1));
            }

            // Save quotation first to get ID
            quotation = quotationRepository.save(quotation);

            // Generate and set code
            quotation.setCode(quotation.generateCode());
            quotation = quotationRepository.save(quotation);

            // Add pax configurations if provided
            if (createDTO.getPaxList() != null && !createDTO.getPaxList().isEmpty()) {
                for (CreateQuotationPaxDTO paxDTO : createDTO.getPaxList()) {
                    ResponseEntity<ApiResponse<?>> paxResult = addPaxToQuotation(quotation, paxDTO);
                    if (!paxResult.getStatusCode().is2xxSuccessful()) {
                        // Rollback would happen automatically due to @Transactional
                        return paxResult;
                    }
                }
            }

            // Add line items if provided
            if (createDTO.getLineItems() != null && !createDTO.getLineItems().isEmpty()) {
                for (CreateQuotationLineItemDTO lineItemDTO : createDTO.getLineItems()) {
                    addLineItemToQuotation(quotation, lineItemDTO);
                }
            }

            // Calculate totals
            quotation.calculateTotals();
            quotation.setTotalPax(quotation.calculateTotalPax());
            quotation = quotationRepository.save(quotation);

            log.info("Quotation created successfully with ID: {}", quotation.getId());

            QuotationDTO dto = quotationGetService.convertToDTO(quotation);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Quotation created successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error creating quotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create quotation: " + e.getMessage(), "QUOTATION_CREATE_FAILED")
            );
        }
    }

    /**
     * Add pax configuration to quotation
     */
    private ResponseEntity<ApiResponse<?>> addPaxToQuotation(Quotation quotation, CreateQuotationPaxDTO paxDTO) {
        try {
            Long nationCategoryId = idObfuscator.decodeId(paxDTO.getNationCategoryId());
            Long ageCategoryId = idObfuscator.decodeId(paxDTO.getAgeCategoryId());

            PaxNationCategory nationCategory = nationCategoryRepository.findById(nationCategoryId).orElse(null);
            if (nationCategory == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Nation category not found", "NATION_CATEGORY_NOT_FOUND")
                );
            }

            PaxAgeCategory ageCategory = ageCategoryRepository.findById(ageCategoryId).orElse(null);
            if (ageCategory == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Age category not found", "AGE_CATEGORY_NOT_FOUND")
                );
            }

            QuotationPax pax = QuotationPax.builder()
                .quotation(quotation)
                .nationCategory(nationCategory)
                .ageCategory(ageCategory)
                .count(paxDTO.getCount() != null ? paxDTO.getCount() : 1)
                .unitPrice(paxDTO.getUnitPrice() != null ? paxDTO.getUnitPrice() : BigDecimal.ZERO)
                .notes(paxDTO.getNotes())
                .build();

            pax.calculateTotalPrice();
            quotation.addPax(pax);

            return ResponseEntity.ok().body(ApiResponse.success(200, "Pax added", null));

        } catch (Exception e) {
            log.error("Error adding pax to quotation", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Failed to add pax: " + e.getMessage(), "PAX_ADD_FAILED")
            );
        }
    }

    /**
     * Add line item to quotation
     */
    private void addLineItemToQuotation(Quotation quotation, CreateQuotationLineItemDTO lineItemDTO) {
        Long referenceId = null;
        if (lineItemDTO.getReferenceId() != null && !lineItemDTO.getReferenceId().isEmpty()) {
            try {
                referenceId = idObfuscator.decodeId(lineItemDTO.getReferenceId());
            } catch (Exception e) {
                log.warn("Failed to decode reference ID: {}", lineItemDTO.getReferenceId());
            }
        }

        QuotationLineItem lineItem = QuotationLineItem.builder()
            .quotation(quotation)
            .dayNumber(lineItemDTO.getDayNumber())
            .sortOrder(lineItemDTO.getSortOrder() != null ? lineItemDTO.getSortOrder() : 0)
            .itemType(lineItemDTO.getItemType())
            .itemName(lineItemDTO.getItemName())
            .description(lineItemDTO.getDescription())
            .referenceId(referenceId)
            .referenceType(lineItemDTO.getReferenceType())
            .quantity(lineItemDTO.getQuantity() != null ? lineItemDTO.getQuantity() : 1)
            .unitOfMeasure(lineItemDTO.getUnitOfMeasure())
            .unitPrice(lineItemDTO.getUnitPrice() != null ? lineItemDTO.getUnitPrice() : BigDecimal.ZERO)
            .currency(lineItemDTO.getCurrency())
            .taxable(lineItemDTO.getTaxable() != null ? lineItemDTO.getTaxable() : true)
            .isIncluded(lineItemDTO.getIsIncluded() != null ? lineItemDTO.getIsIncluded() : true)
            .isOptional(lineItemDTO.getIsOptional() != null ? lineItemDTO.getIsOptional() : false)
            .notes(lineItemDTO.getNotes())
            .build();

        lineItem.calculateTotalPrice();
        quotation.addLineItem(lineItem);
    }

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getId();
            }
        }
        return null;
    }
}
