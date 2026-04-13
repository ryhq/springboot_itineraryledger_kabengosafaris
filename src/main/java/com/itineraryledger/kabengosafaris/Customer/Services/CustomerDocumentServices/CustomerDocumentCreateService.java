package com.itineraryledger.kabengosafaris.Customer.Services.CustomerDocumentServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs.CreateCustomerDocumentDTO;
import com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerDocumentDTOs.CustomerDocumentDTO;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerDocumentRepository;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating customer documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CustomerDocumentCreateService {

    private final CustomerDocumentRepository customerDocumentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDocumentStorageService storageService;
    private final CustomerDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "CREATE_CUSTOMER_DOCUMENTS",
        description = "Uploading customer documents",
        entityType = "CustomerDocument"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocuments(List<CreateCustomerDocumentDTO> documentDTOs) {
        log.info("Uploading {} customer documents", documentDTOs != null ? documentDTOs.size() : 0);

        try {
            if (documentDTOs == null || documentDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No documents provided", "NO_DOCUMENTS_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateCustomerDocumentDTO dto : documentDTOs) {
                if (dto.getDocument() != null) {
                    totalSize += dto.getDocument().getSize();
                }
            }

            String requestSizeError = storageService.validateRequestSize(totalSize);
            if (requestSizeError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, requestSizeError, "REQUEST_SIZE_EXCEEDED")
                );
            }

            List<String> validationErrors = new ArrayList<>();
            for (int i = 0; i < documentDTOs.size(); i++) {
                CreateCustomerDocumentDTO dto = documentDTOs.get(i);

                if (dto.getCustomerId() == null || dto.getCustomerId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Customer ID is required", i + 1));
                    continue;
                }

                if (dto.getTitle() == null || dto.getTitle().isBlank()) {
                    validationErrors.add(String.format("Document %d: Title is required", i + 1));
                    continue;
                }

                if (dto.getDocument() == null || dto.getDocument().isEmpty()) {
                    validationErrors.add(String.format("Document %d: Document file is required", i + 1));
                    continue;
                }

                String documentError = storageService.validateDocument(dto.getDocument());
                if (documentError != null) {
                    String filename = dto.getDocument().getOriginalFilename() != null
                        ? dto.getDocument().getOriginalFilename()
                        : "unknown";
                    validationErrors.add(String.format("Document %d (%s): %s", i + 1, filename, documentError));
                }

                try {
                    Long customerId = idObfuscator.decodeId(dto.getCustomerId());
                    if (!customerRepository.existsById(customerId)) {
                        validationErrors.add(String.format("Document %d: Customer not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Document %d: Invalid Customer ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<CustomerDocumentDTO> createdDocuments = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateCustomerDocumentDTO dto : documentDTOs) {
                    Long customerId = idObfuscator.decodeId(dto.getCustomerId());
                    Customer customer = customerRepository.findById(customerId).orElse(null);

                    if (customer == null) {
                        continue;
                    }

                    MultipartFile file = dto.getDocument();
                    String savedFileName = storageService.saveDocument(file);
                    if (savedFileName == null) {
                        rollbackSavedFiles(savedFileNames);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                            ApiResponse.error(500, "Failed to save document file: " + file.getOriginalFilename(), "STORAGE_ERROR")
                        );
                    }
                    savedFileNames.add(savedFileName);

                    CustomerDocument document = CustomerDocument.builder()
                        .customer(customer)
                        .title(dto.getTitle())
                        .documentType(dto.getDocumentType() != null ? dto.getDocumentType() : DocumentType.OTHER)
                        .fileUrl(storageService.constructFileDocumentUrl(savedFileName))
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .fileType(storageService.getMimeType(savedFileName))
                        .description(dto.getDescription())
                        .documentNumber(dto.getDocumentNumber())
                        .version(dto.getVersion())
                        .validFrom(dto.getValidFrom())
                        .validTo(dto.getValidTo())
                        .notes(dto.getNotes())
                        .isActive(true)
                        .build();

                    document = customerDocumentRepository.save(document);
                    createdDocuments.add(getService.toDTO(document));
                }

                log.info("{} customer documents created successfully", createdDocuments.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdDocuments.size() + " document(s) uploaded successfully", createdDocuments)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create customer document records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create document records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading customer documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload customer documents", "CUSTOMER_DOCUMENT_UPLOAD_FAILED")
            );
        }
    }

    /**
     * Save a system-generated document (e.g., quote PDF) to a customer's documents.
     *
     * @param customerId The customer database ID
     * @param pdfBytes The generated PDF content
     * @param originalFileName The display filename (e.g., "Quote_QT-1001-0426-1.pdf")
     * @param documentType The type of customer document
     * @param title The document title
     * @return The created CustomerDocumentDTO, or null if failed
     */
    public CustomerDocumentDTO saveGeneratedDocument(
            Long customerId,
            byte[] pdfBytes,
            String originalFileName,
            CustomerDocument.DocumentType documentType,
            String title
    ) {
        log.info("Saving generated document for customer {}: {}", customerId, title);

        try {
            if (customerId == null) {
                log.error("Customer ID is required for generated document");
                return null;
            }
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("PDF content is required for generated document");
                return null;
            }

            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                log.error("Customer not found: {}", customerId);
                return null;
            }

            String savedFileName = storageService.saveDocumentBytes(pdfBytes, originalFileName);
            if (savedFileName == null) {
                log.error("Failed to save generated PDF file: {}", originalFileName);
                return null;
            }

            CustomerDocument document = CustomerDocument.builder()
                .customer(customer)
                .title(title)
                .documentType(documentType != null ? documentType : CustomerDocument.DocumentType.OTHER)
                .fileUrl(storageService.constructFileDocumentUrl(savedFileName))
                .fileName(savedFileName)
                .originalFileName(originalFileName != null ? originalFileName : "generated-document.pdf")
                .fileSize((long) pdfBytes.length)
                .fileType("application/pdf")
                .isActive(true)
                .build();

            document = customerDocumentRepository.save(document);
            log.info("Generated customer document saved successfully: {} (ID: {})", title, document.getId());

            return getService.toDTO(document);

        } catch (Exception e) {
            log.error("Error saving generated document for customer {}", customerId, e);
            return null;
        }
    }

    private void rollbackSavedFiles(List<String> fileNames) {
        for (String fileName : fileNames) {
            try {
                storageService.deleteDocument(fileName);
                log.debug("Rolled back file: {}", fileName);
            } catch (Exception e) {
                log.warn("Failed to rollback file: {}", fileName, e);
            }
        }
    }
}
