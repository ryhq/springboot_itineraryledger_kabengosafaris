package com.itineraryledger.kabengosafaris.Quotation.Services;

import com.itineraryledger.kabengosafaris.Quotation.DTOs.QuotationDTO;
import com.itineraryledger.kabengosafaris.Quotation.DTOs.QuotationLineItemDTO;
import com.itineraryledger.kabengosafaris.Quotation.DTOs.QuotationPaxDTO;
import com.itineraryledger.kabengosafaris.Quotation.Entity.Quotation;
import com.itineraryledger.kabengosafaris.Quotation.Entity.QuotationLineItem;
import com.itineraryledger.kabengosafaris.Quotation.Entity.QuotationPax;
import com.itineraryledger.kabengosafaris.Quotation.Enums.QuotationStatus;
import com.itineraryledger.kabengosafaris.Quotation.Repository.QuotationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * QuotationGetService - Service for retrieving quotations
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class QuotationGetService {

    private final QuotationRepository quotationRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public QuotationGetService(
        QuotationRepository quotationRepository,
        UserRepository userRepository,
        IdObfuscator idObfuscator
    ) {
        this.quotationRepository = quotationRepository;
        this.userRepository = userRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get quotation by obfuscated ID
     */
    public ResponseEntity<ApiResponse<?>> getQuotationById(String idObfuscated) {
        log.info("Fetching quotation with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode quotation ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid quotation ID", "INVALID_QUOTATION_ID")
                );
            }

            Quotation quotation = quotationRepository.findById(id).orElse(null);
            if (quotation == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            QuotationDTO dto = convertToDTO(quotation);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation retrieved successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error fetching quotation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quotation", "QUOTATION_FETCH_FAILED")
            );
        }
    }

    /**
     * Get quotation by code
     */
    public ResponseEntity<ApiResponse<?>> getQuotationByCode(String code) {
        log.info("Fetching quotation with code: {}", code);

        try {
            Optional<Quotation> quotationOpt = quotationRepository.findByCode(code);
            if (quotationOpt.isEmpty()) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Quotation not found", "QUOTATION_NOT_FOUND")
                );
            }

            QuotationDTO dto = convertToDTO(quotationOpt.get());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotation retrieved successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error fetching quotation by code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quotation", "QUOTATION_FETCH_FAILED")
            );
        }
    }

    /**
     * Get all quotations with filtering and pagination
     */
    public ResponseEntity<ApiResponse<?>> getAllQuotations(
        String customerId,
        String itineraryId,
        QuotationStatus status,
        String assignedToId,
        String createdById,
        String name,
        String code,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        Boolean expired,
        String currency,
        Boolean originalOnly,
        String keyword,
        Pageable pageable
    ) {
        log.info("Fetching all quotations with filters");

        try {
            Specification<Quotation> spec = Specification.unrestricted();

            // Customer filter
            if (customerId != null && !customerId.isEmpty()) {
                try {
                    Long decodedCustomerId = idObfuscator.decodeId(customerId);
                    spec = spec.and(QuotationSpecification.hasCustomerId(decodedCustomerId));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                    );
                }
            }

            // Itinerary filter
            if (itineraryId != null && !itineraryId.isEmpty()) {
                try {
                    Long decodedItineraryId = idObfuscator.decodeId(itineraryId);
                    spec = spec.and(QuotationSpecification.hasItineraryId(decodedItineraryId));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid itinerary ID", "INVALID_ITINERARY_ID")
                    );
                }
            }

            // Status filter
            if (status != null) {
                spec = spec.and(QuotationSpecification.hasStatus(status));
            }

            // Assigned to filter
            if (assignedToId != null && !assignedToId.isEmpty()) {
                try {
                    Long decodedUserId = idObfuscator.decodeId(assignedToId);
                    spec = spec.and(QuotationSpecification.assignedTo(decodedUserId));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid assigned to ID", "INVALID_ASSIGNED_TO_ID")
                    );
                }
            }

            // Created by filter
            if (createdById != null && !createdById.isEmpty()) {
                try {
                    Long decodedUserId = idObfuscator.decodeId(createdById);
                    spec = spec.and(QuotationSpecification.createdBy(decodedUserId));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid created by ID", "INVALID_CREATED_BY_ID")
                    );
                }
            }

            // Other filters
            if (name != null && !name.isEmpty()) {
                spec = spec.and(QuotationSpecification.nameLike(name));
            }
            if (code != null && !code.isEmpty()) {
                spec = spec.and(QuotationSpecification.codeLike(code));
            }
            if (startDateFrom != null || startDateTo != null) {
                spec = spec.and(QuotationSpecification.startDateBetween(startDateFrom, startDateTo));
            }
            if (createdFrom != null || createdTo != null) {
                spec = spec.and(QuotationSpecification.createdBetween(createdFrom, createdTo));
            }
            if (expired != null) {
                spec = spec.and(QuotationSpecification.isExpired(expired));
            }
            if (currency != null && !currency.isEmpty()) {
                spec = spec.and(QuotationSpecification.hasCurrency(currency));
            }
            if (Boolean.TRUE.equals(originalOnly)) {
                spec = spec.and(QuotationSpecification.isOriginal());
            }
            if (keyword != null && !keyword.isEmpty()) {
                spec = spec.and(QuotationSpecification.searchKeyword(keyword));
            }

            Page<Quotation> quotationPage = quotationRepository.findAll(spec, pageable);
            Page<QuotationDTO> dtoPage = quotationPage.map(this::convertToDTO);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("quotations", dtoPage.getContent());
            responseData.put("currentPage", dtoPage.getNumber());
            responseData.put("totalItems", dtoPage.getTotalElements());
            responseData.put("totalPages", dtoPage.getTotalPages());
            responseData.put("pageSize", dtoPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Quotations retrieved successfully", responseData)
            );

        } catch (Exception e) {
            log.error("Error fetching quotations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch quotations", "QUOTATIONS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get quotations for a specific customer
     */
    public ResponseEntity<ApiResponse<?>> getCustomerQuotations(
        String customerId,
        QuotationStatus status,
        Pageable pageable
    ) {
        log.info("Fetching quotations for customer: {}", customerId);

        try {
            Long decodedCustomerId;
            try {
                decodedCustomerId = idObfuscator.decodeId(customerId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid customer ID", "INVALID_CUSTOMER_ID")
                );
            }

            Page<Quotation> quotationPage;
            if (status != null) {
                quotationPage = quotationRepository.findByCustomerIdAndStatus(decodedCustomerId, status, pageable);
            } else {
                quotationPage = quotationRepository.findByCustomerId(decodedCustomerId, pageable);
            }

            Page<QuotationDTO> dtoPage = quotationPage.map(this::convertToDTO);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("quotations", dtoPage.getContent());
            responseData.put("currentPage", dtoPage.getNumber());
            responseData.put("totalItems", dtoPage.getTotalElements());
            responseData.put("totalPages", dtoPage.getTotalPages());
            responseData.put("pageSize", dtoPage.getSize());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Customer quotations retrieved successfully", responseData)
            );

        } catch (Exception e) {
            log.error("Error fetching customer quotations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch customer quotations", "CUSTOMER_QUOTATIONS_FETCH_FAILED")
            );
        }
    }

    /**
     * Convert Quotation entity to DTO
     */
    public QuotationDTO convertToDTO(Quotation quotation) {
        QuotationDTO.QuotationDTOBuilder builder = QuotationDTO.builder()
            .id(idObfuscator.encodeId(quotation.getId()))
            .code(quotation.getCode())
            .name(quotation.getName())
            .status(quotation.getStatus())
            .statusDisplayName(quotation.getStatus() != null ? quotation.getStatus().getDisplayName() : null)
            .statusDescription(quotation.getStatus() != null ? quotation.getStatus().getDescription() : null)
            .version(quotation.getVersion())
            .startDate(quotation.getStartDate())
            .endDate(quotation.getEndDate())
            .totalDays(quotation.getTotalDays())
            .totalNights(quotation.getTotalNights())
            .daysNightsDisplay(quotation.getDaysNightsDisplay())
            .totalPax(quotation.calculateTotalPax())
            .currency(quotation.getCurrency())
            .subtotal(quotation.getSubtotal())
            .discountType(quotation.getDiscountType())
            .discountTypeDisplayName(quotation.getDiscountType() != null ? quotation.getDiscountType().getDisplayName() : null)
            .discountValue(quotation.getDiscountValue())
            .taxRate(quotation.getTaxRate())
            .taxAmount(quotation.getTaxAmount())
            .totalAmount(quotation.getTotalAmount())
            .depositRequired(quotation.getDepositRequired())
            .depositPercentage(quotation.getDepositPercentage())
            .perPersonCost(quotation.getPerPersonCost())
            .validUntil(quotation.getValidUntil())
            .isExpired(quotation.isExpired())
            .sentAt(quotation.getSentAt())
            .viewedAt(quotation.getViewedAt())
            .acceptedAt(quotation.getAcceptedAt())
            .rejectedAt(quotation.getRejectedAt())
            .paxCount(quotation.getPaxList() != null ? quotation.getPaxList().size() : 0)
            .lineItemCount(quotation.getLineItems() != null ? quotation.getLineItems().size() : 0)
            .createdAt(quotation.getCreatedAt())
            .updatedAt(quotation.getUpdatedAt())
            .canSend(quotation.canSend())
            .canRevise(quotation.canRevise())
            .canAccept(quotation.canAccept())
            .canConvertToSafari(quotation.canConvertToSafari());

        // Days until expiry
        if (quotation.getValidUntil() != null && !quotation.isExpired()) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), quotation.getValidUntil());
            builder.daysUntilExpiry((int) days);
        }

        // Customer
        if (quotation.getCustomer() != null) {
            builder.customerId(idObfuscator.encodeId(quotation.getCustomer().getId()))
                   .customerDisplayName(quotation.getCustomer().getDisplayName())
                   .customerEmail(quotation.getCustomer().getPrimaryEmail());
        }

        // Itinerary
        if (quotation.getItinerary() != null) {
            builder.itineraryId(idObfuscator.encodeId(quotation.getItinerary().getId()))
                   .itineraryCode(quotation.getItinerary().getCode())
                   .itineraryName(quotation.getItinerary().getName());
        }

        // Parent quotation
        if (quotation.getParentQuotation() != null) {
            builder.parentQuotationId(idObfuscator.encodeId(quotation.getParentQuotation().getId()))
                   .parentQuotationCode(quotation.getParentQuotation().getCode());
        }

        // Assigned to
        if (quotation.getAssignedTo() != null) {
            builder.assignedToId(idObfuscator.encodeId(quotation.getAssignedTo()));
            userRepository.findById(quotation.getAssignedTo()).ifPresent(user ->
                builder.assignedToName(user.getFirstName() + " " + user.getLastName())
            );
        }

        // Created by
        if (quotation.getCreatedBy() != null) {
            builder.createdById(idObfuscator.encodeId(quotation.getCreatedBy()));
            userRepository.findById(quotation.getCreatedBy()).ifPresent(user ->
                builder.createdByName(user.getFirstName() + " " + user.getLastName())
            );
        }

        return builder.build();
    }

    /**
     * Convert QuotationPax entity to DTO
     */
    public QuotationPaxDTO convertPaxToDTO(QuotationPax pax) {
        return QuotationPaxDTO.builder()
            .id(idObfuscator.encodeId(pax.getId()))
            .quotationId(idObfuscator.encodeId(pax.getQuotation().getId()))
            .nationCategoryId(idObfuscator.encodeId(pax.getNationCategory().getId()))
            .nationCategoryName(pax.getNationCategory().getName())
            .ageCategoryId(idObfuscator.encodeId(pax.getAgeCategory().getId()))
            .ageCategoryName(pax.getAgeCategory().getName())
            .ageCategoryMinAge(pax.getAgeCategory().getMinAge())
            .ageCategoryMaxAge(pax.getAgeCategory().getMaxAge())
            .count(pax.getCount())
            .unitPrice(pax.getUnitPrice())
            .totalPrice(pax.getTotalPrice())
            .displayName(pax.getDisplayName())
            .notes(pax.getNotes())
            .createdAt(pax.getCreatedAt())
            .updatedAt(pax.getUpdatedAt())
            .build();
    }

    /**
     * Convert QuotationLineItem entity to DTO
     */
    public QuotationLineItemDTO convertLineItemToDTO(QuotationLineItem lineItem) {
        return QuotationLineItemDTO.builder()
            .id(idObfuscator.encodeId(lineItem.getId()))
            .quotationId(idObfuscator.encodeId(lineItem.getQuotation().getId()))
            .dayNumber(lineItem.getDayNumber())
            .sortOrder(lineItem.getSortOrder())
            .itemType(lineItem.getItemType())
            .itemTypeDisplayName(lineItem.getItemType() != null ? lineItem.getItemType().getDisplayName() : null)
            .itemTypeDescription(lineItem.getItemType() != null ? lineItem.getItemType().getDescription() : null)
            .itemName(lineItem.getItemName())
            .description(lineItem.getDescription())
            .referenceId(lineItem.getReferenceId() != null ? idObfuscator.encodeId(lineItem.getReferenceId()) : null)
            .referenceType(lineItem.getReferenceType())
            .quantity(lineItem.getQuantity())
            .unitOfMeasure(lineItem.getUnitOfMeasure())
            .unitPrice(lineItem.getUnitPrice())
            .totalPrice(lineItem.getTotalPrice())
            .currency(lineItem.getCurrency())
            .taxable(lineItem.getTaxable())
            .isIncluded(lineItem.getIsIncluded())
            .isOptional(lineItem.getIsOptional())
            .displayLine(lineItem.getDisplayLine())
            .notes(lineItem.getNotes())
            .createdAt(lineItem.getCreatedAt())
            .updatedAt(lineItem.getUpdatedAt())
            .build();
    }
}
