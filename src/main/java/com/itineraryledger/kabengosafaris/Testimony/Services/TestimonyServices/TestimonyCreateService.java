package com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.CreateTestimonyDTO;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyDTO;
import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class TestimonyCreateService {

    private final TestimonyRepository testimonyRepository;
    private final CustomerRepository customerRepository;
    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;
    private final TestimonyGetService getService;

    @Autowired
    public TestimonyCreateService(
        TestimonyRepository testimonyRepository,
        CustomerRepository customerRepository,
        SafariRepository safariRepository,
        IdObfuscator idObfuscator,
        TestimonyGetService getService
    ) {
        this.testimonyRepository = testimonyRepository;
        this.customerRepository = customerRepository;
        this.safariRepository = safariRepository;
        this.idObfuscator = idObfuscator;
        this.getService = getService;
    }

    @AuditLogAnnotation(action = "CREATE_TESTIMONY", description = "Creating a new testimony", entityType = "Testimony")
    public ResponseEntity<ApiResponse<?>> createTestimony(CreateTestimonyDTO createDTO) {
        log.info("Creating new testimony from: {}", createDTO.getAuthorName());

        try {
            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            Testimony testimony = Testimony.builder()
                .authorName(createDTO.getAuthorName())
                .authorTitle(createDTO.getAuthorTitle())
                .authorCountry(createDTO.getAuthorCountry())
                .message(createDTO.getMessage())
                .rating(createDTO.getRating())
                .source(createDTO.getSource())
                .reviewDate(createDTO.getReviewDate())
                .isVerifiedBooking(createDTO.getIsVerifiedBooking() != null ? createDTO.getIsVerifiedBooking() : false)
                .isApproved(createDTO.getIsApproved() != null ? createDTO.getIsApproved() : false)
                .isFeatured(createDTO.getIsFeatured() != null ? createDTO.getIsFeatured() : false)
                .isActive(createDTO.getIsActive() != null ? createDTO.getIsActive() : true)
                .displayOrder(createDTO.getDisplayOrder() != null ? createDTO.getDisplayOrder() : 0)
                .sentimentTags(createDTO.getSentimentTags())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

            // Resolve optional customer
            if (createDTO.getCustomerId() != null && !createDTO.getCustomerId().isBlank()) {
                try {
                    Long customerId = idObfuscator.decodeId(createDTO.getCustomerId());
                    Customer customer = customerRepository.findById(customerId).orElse(null);
                    if (customer != null) {
                        testimony.setCustomer(customer);
                    } else {
                        log.warn("Customer not found for ID: {}", createDTO.getCustomerId());
                    }
                } catch (Exception e) {
                    log.warn("Invalid customer ID: {}", createDTO.getCustomerId());
                }
            }

            // Resolve optional safari
            if (createDTO.getSafariId() != null && !createDTO.getSafariId().isBlank()) {
                try {
                    Long safariId = idObfuscator.decodeId(createDTO.getSafariId());
                    Safari safari = safariRepository.findById(safariId).orElse(null);
                    if (safari != null) {
                        testimony.setSafari(safari);
                    } else {
                        log.warn("Safari not found for ID: {}", createDTO.getSafariId());
                    }
                } catch (Exception e) {
                    log.warn("Invalid safari ID: {}", createDTO.getSafariId());
                }
            }

            testimony = testimonyRepository.save(testimony);

            TestimonyDTO dto = getService.convertToDTO(testimony);

            log.info("Testimony created successfully: {}", testimony.getAuthorName());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Testimony created successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error creating testimony", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create testimony", "TESTIMONY_CREATE_FAILED")
            );
        }
    }
}
