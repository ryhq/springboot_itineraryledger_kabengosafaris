package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceFromSafariDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * InvoiceFromSafariGenerationService - Generates an invoice from a Safari
 *
 * This service bridges the Safari and Invoice modules by:
 * 1. Extracting Safari details (customer, dates, description)
 * 2. Creating a new Invoice entity
 * 3. Linking the invoice to the Safari
 * 4. Setting up proper relationships
 */
@Service
@Slf4j
@Transactional
public class InvoiceFromSafariGenerationService {

    private final InvoiceCreateService invoiceCreateService;
    private final SafariRepository safariRepository;
    private final CustomerRepository customerRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public InvoiceFromSafariGenerationService(
            InvoiceCreateService invoiceCreateService,
            SafariRepository safariRepository,
            CustomerRepository customerRepository,
            IdObfuscator idObfuscator
    ) {
        this.invoiceCreateService = invoiceCreateService;
        this.safariRepository = safariRepository;
        this.customerRepository = customerRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Generate an invoice from a Safari
     *
     * @param dto CreateInvoiceFromSafariDTO containing safari ID and invoice details
     * @return ResponseEntity with the created invoice
     */
    public ResponseEntity<ApiResponse<?>> generateInvoiceFromSafari(CreateInvoiceFromSafariDTO dto) {
        log.info("Generating invoice for safari: {}", dto.getSafariId());

        try {
            // 1. Validate inputs and decode Safari ID
            Long safariId;
            try {
                safariId = idObfuscator.decodeId(dto.getSafariId());
            } catch (Exception e) {
                log.warn("Failed to decode safari ID: {}", dto.getSafariId(), e);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // Verify Safari exists
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // Verify customer exists
            if (safari.getCustomer() == null) {
                return ResponseEntity.status(400).body(
                        ApiResponse.error(400, "Safari has no associated customer", "NO_CUSTOMER")
                );
            }

            // 2. Build CreateInvoiceDTO from Safari details
            CreateInvoiceDTO createInvoiceDTO = buildCreateInvoiceDTO(dto, safari);

            // 3. Create the Invoice
            ResponseEntity<ApiResponse<?>> invoiceResponse = invoiceCreateService.createInvoice(createInvoiceDTO);
            if (!invoiceResponse.getStatusCode().is2xxSuccessful() || invoiceResponse.getBody() == null) {
                log.error("Failed to create invoice");
                return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Failed to create invoice", "INVOICE_CREATION_FAILED")
                );
            }

            InvoiceDTO invoiceDTO = (InvoiceDTO) invoiceResponse.getBody().getData();

            log.info("Successfully generated invoice: {} for safari: {}",
                    invoiceDTO.getInvoiceCode(), safari.getCode());

            // 4. Return the created invoice
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201,
                            "Invoice generated successfully from safari",
                            invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error generating invoice from safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500,
                            "Failed to generate invoice: " + e.getMessage(),
                            "INVOICE_GENERATION_FAILED")
            );
        }
    }

    /**
     * Build CreateInvoiceDTO from Safari and input DTO
     */
    private CreateInvoiceDTO buildCreateInvoiceDTO(CreateInvoiceFromSafariDTO inputDTO, Safari safari) {
        CreateInvoiceDTO dto = new CreateInvoiceDTO();

        // Basic information from input or Safari
        dto.setTitle(inputDTO.getTitle());
        dto.setDescription(inputDTO.getDescription() != null
                ? inputDTO.getDescription()
                : safari.getDescription());

        // Link to customer and safari
        dto.setCustomerId(idObfuscator.encodeId(safari.getCustomer().getId()));
        dto.setSafariId(idObfuscator.encodeId(safari.getId()));

        // Pricing details from input
        dto.setTaxPercentage(inputDTO.getTaxPercentage());
        dto.setDiscountPercentage(inputDTO.getDiscountPercentage());
        dto.setDiscountReason(inputDTO.getDiscountReason());

        // Dates from input
        dto.setIssueDate(inputDTO.getIssueDate());
        dto.setDueDate(inputDTO.getDueDate());

        // Notes from input
        dto.setInternalNotes(inputDTO.getInternalNotes());
        dto.setCustomerNotes(inputDTO.getCustomerNotes());
        dto.setPaymentTerms(inputDTO.getPaymentTerms());

        // Default: active
        dto.setIsActive(inputDTO.getIsActive() != null ? inputDTO.getIsActive() : true);

        return dto;
    }
}
