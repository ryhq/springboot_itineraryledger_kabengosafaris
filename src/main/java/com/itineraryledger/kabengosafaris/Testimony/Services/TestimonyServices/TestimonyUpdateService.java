package com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices;

import java.time.LocalDateTime;

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
import com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyDTO;
import com.itineraryledger.kabengosafaris.Testimony.DTOs.UpdateTestimonyDTO;
import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class TestimonyUpdateService {

    private final TestimonyRepository testimonyRepository;
    private final CustomerRepository customerRepository;
    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;
    private final TestimonyGetService getService;

    @Autowired
    public TestimonyUpdateService(
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

    @AuditLogAnnotation(action = "UPDATE_TESTIMONY", description = "Updating testimony", entityType = "Testimony", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateTestimony(String idObfuscated, UpdateTestimonyDTO updateDTO) {
        log.info("Updating testimony with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode testimony ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid testimony ID", "INVALID_TESTIMONY_ID")
                );
            }

            Testimony testimony = testimonyRepository.findById(id).orElse(null);
            if (testimony == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Testimony not found", "TESTIMONY_NOT_FOUND")
                );
            }

            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (updateDTO.getAuthorName() != null) testimony.setAuthorName(updateDTO.getAuthorName());
            if (updateDTO.getAuthorTitle() != null) testimony.setAuthorTitle(updateDTO.getAuthorTitle());
            if (updateDTO.getAuthorCountry() != null) testimony.setAuthorCountry(updateDTO.getAuthorCountry());
            if (updateDTO.getMessage() != null) testimony.setMessage(updateDTO.getMessage());
            if (updateDTO.getRating() != null) testimony.setRating(updateDTO.getRating());
            if (updateDTO.getSource() != null) testimony.setSource(updateDTO.getSource());
            if (updateDTO.getReviewDate() != null) testimony.setReviewDate(updateDTO.getReviewDate());
            if (updateDTO.getIsVerifiedBooking() != null) testimony.setIsVerifiedBooking(updateDTO.getIsVerifiedBooking());
            if (updateDTO.getIsApproved() != null) testimony.setIsApproved(updateDTO.getIsApproved());
            if (updateDTO.getIsFeatured() != null) testimony.setIsFeatured(updateDTO.getIsFeatured());
            if (updateDTO.getIsActive() != null) testimony.setIsActive(updateDTO.getIsActive());
            if (updateDTO.getDisplayOrder() != null) testimony.setDisplayOrder(updateDTO.getDisplayOrder());
            if (updateDTO.getSentimentTags() != null) testimony.setSentimentTags(updateDTO.getSentimentTags());

            // Resolve optional customer
            if (updateDTO.getCustomerId() != null) {
                if (updateDTO.getCustomerId().isBlank()) {
                    testimony.setCustomer(null);
                } else {
                    try {
                        Long customerId = idObfuscator.decodeId(updateDTO.getCustomerId());
                        Customer customer = customerRepository.findById(customerId).orElse(null);
                        if (customer != null) testimony.setCustomer(customer);
                    } catch (Exception e) {
                        log.warn("Invalid customer ID: {}", updateDTO.getCustomerId());
                    }
                }
            }

            // Resolve optional safari
            if (updateDTO.getSafariId() != null) {
                if (updateDTO.getSafariId().isBlank()) {
                    testimony.setSafari(null);
                } else {
                    try {
                        Long safariId = idObfuscator.decodeId(updateDTO.getSafariId());
                        Safari safari = safariRepository.findById(safariId).orElse(null);
                        if (safari != null) testimony.setSafari(safari);
                    } catch (Exception e) {
                        log.warn("Invalid safari ID: {}", updateDTO.getSafariId());
                    }
                }
            }

            testimony.setUpdatedBy(currentUser);
            testimony = testimonyRepository.save(testimony);

            TestimonyDTO dto = getService.convertToDTO(testimony);

            log.info("Testimony updated successfully: {}", testimony.getAuthorName());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Testimony updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating testimony", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update testimony", "TESTIMONY_UPDATE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "APPROVE_TESTIMONY", description = "Approving testimony", entityType = "Testimony", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> approveTestimony(String idObfuscated, Boolean approved) {
        log.info("Approving testimony with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid testimony ID", "INVALID_TESTIMONY_ID")
                );
            }

            Testimony testimony = testimonyRepository.findById(id).orElse(null);
            if (testimony == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Testimony not found", "TESTIMONY_NOT_FOUND")
                );
            }

            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            testimony.setIsApproved(approved != null ? approved : true);
            testimony.setUpdatedBy(currentUser);
            testimony = testimonyRepository.save(testimony);

            TestimonyDTO dto = getService.convertToDTO(testimony);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Testimony " + (testimony.getIsApproved() ? "approved" : "unapproved") + " successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error approving testimony", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to approve testimony", "TESTIMONY_APPROVE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "RESPOND_TO_TESTIMONY", description = "Adding admin response to testimony", entityType = "Testimony", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> respondToTestimony(String idObfuscated, String adminResponse) {
        log.info("Adding admin response to testimony with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid testimony ID", "INVALID_TESTIMONY_ID")
                );
            }

            Testimony testimony = testimonyRepository.findById(id).orElse(null);
            if (testimony == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Testimony not found", "TESTIMONY_NOT_FOUND")
                );
            }

            if (adminResponse == null || adminResponse.isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Admin response is required", "ADMIN_RESPONSE_REQUIRED")
                );
            }

            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            testimony.setAdminResponse(adminResponse);
            testimony.setAdminResponseDate(LocalDateTime.now());
            testimony.setUpdatedBy(currentUser);
            testimony = testimonyRepository.save(testimony);

            TestimonyDTO dto = getService.convertToDTO(testimony);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Admin response added successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error responding to testimony", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to add admin response", "TESTIMONY_RESPOND_FAILED")
            );
        }
    }
}
