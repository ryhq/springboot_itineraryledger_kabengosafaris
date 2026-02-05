package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceDocumentServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.CreateInvoiceDocumentDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs.InvoiceDocumentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceDocument.DocumentType;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceDocumentRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for creating invoice documents.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceDocumentCreateService {

    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDocumentStorageService storageService;
    private final InvoiceDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "CREATE_INVOICE_DOCUMENTS",
        description = "Uploading invoice documents",
        entityType = "InvoiceDocument"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocuments(List<CreateInvoiceDocumentDTO> documentDTOs) {
        log.info("Uploading {} invoice documents", documentDTOs != null ? documentDTOs.size() : 0);

        try {
            if (documentDTOs == null || documentDTOs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No documents provided", "NO_DOCUMENTS_PROVIDED")
                );
            }

            long totalSize = 0;
            for (CreateInvoiceDocumentDTO dto : documentDTOs) {
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
                CreateInvoiceDocumentDTO dto = documentDTOs.get(i);

                if (dto.getInvoiceId() == null || dto.getInvoiceId().isBlank()) {
                    validationErrors.add(String.format("Document %d: Invoice ID is required", i + 1));
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
                    Long invoiceId = idObfuscator.decodeId(dto.getInvoiceId());
                    if (!invoiceRepository.existsById(invoiceId)) {
                        validationErrors.add(String.format("Document %d: Invoice not found", i + 1));
                    }
                } catch (Exception e) {
                    validationErrors.add(String.format("Document %d: Invalid Invoice ID", i + 1));
                }
            }

            if (!validationErrors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", validationErrors);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, errorMessage, "VALIDATION_ERROR")
                );
            }

            List<InvoiceDocumentDTO> createdDocuments = new ArrayList<>();
            List<String> savedFileNames = new ArrayList<>();

            try {
                for (CreateInvoiceDocumentDTO dto : documentDTOs) {
                    Long invoiceId = idObfuscator.decodeId(dto.getInvoiceId());
                    Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);

                    if (invoice == null) {
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

                    InvoiceDocument document = InvoiceDocument.builder()
                        .invoice(invoice)
                        .title(dto.getTitle())
                        .documentType(dto.getDocumentType() != null ? dto.getDocumentType() : DocumentType.OTHER)
                        .fileUrl(storageService.constructFileDocumentUrl(savedFileName))
                        .fileName(savedFileName)
                        .originalFileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .fileType(storageService.getMimeType(savedFileName))
                        .description(dto.getDescription())
                        .version(dto.getVersion())
                        .validFrom(dto.getValidFrom())
                        .validTo(dto.getValidTo())
                        .notes(dto.getNotes())
                        .isActive(true)
                        .isGenerated(false) // User uploads are never system-generated
                        .build();

                    document = invoiceDocumentRepository.save(document);
                    createdDocuments.add(getService.toDTO(document));
                }

                log.info("{} invoice documents created successfully", createdDocuments.size());

                return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, createdDocuments.size() + " document(s) uploaded successfully", createdDocuments)
                );

            } catch (Exception e) {
                rollbackSavedFiles(savedFileNames);
                log.error("Failed to create invoice document records, rolled back {} files", savedFileNames.size(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create document records", "DATABASE_ERROR")
                );
            }

        } catch (Exception e) {
            log.error("Error uploading invoice documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload invoice documents", "INVOICE_DOCUMENT_UPLOAD_FAILED")
            );
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

    // =========================================================================
    // SYSTEM-GENERATED DOCUMENT METHODS
    // =========================================================================

    /**
     * Save a system-generated PDF document (e.g., invoice, receipt, credit note).
     * This method is intended for internal use by PDF generation services.
     *
     * @param invoiceId The invoice ID (not obfuscated)
     * @param pdfBytes The generated PDF content
     * @param originalFileName The display filename (e.g., "invoice-INV-001.pdf")
     * @param documentType The type of document
     * @param title The document title
     * @param description Optional description
     * @param version Optional version string
     * @param notes Optional notes
     * @return The created InvoiceDocumentDTO, or null if failed
     */
    @AuditLogAnnotation(
        action = "CREATE_GENERATED_INVOICE_DOCUMENT",
        description = "Saving system-generated invoice document",
        entityType = "InvoiceDocument"
    )
    public InvoiceDocumentDTO saveGeneratedDocument(
            Long invoiceId,
            byte[] pdfBytes,
            String originalFileName,
            DocumentType documentType,
            String title,
            String description,
            String version,
            String notes
    ) {
        log.info("Saving generated document for invoice {}: {}", invoiceId, title);

        try {
            if (invoiceId == null) {
                log.error("Invoice ID is required for generated document");
                return null;
            }

            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("PDF content is required for generated document");
                return null;
            }

            if (title == null || title.isBlank()) {
                log.error("Title is required for generated document");
                return null;
            }

            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                log.error("Invoice not found: {}", invoiceId);
                return null;
            }

            // Save the PDF bytes to storage
            String savedFileName = storageService.saveDocumentBytes(pdfBytes, originalFileName);
            if (savedFileName == null) {
                log.error("Failed to save generated PDF file: {}", originalFileName);
                return null;
            }

            // Create the document record
            InvoiceDocument document = InvoiceDocument.builder()
                .invoice(invoice)
                .title(title)
                .documentType(documentType != null ? documentType : DocumentType.OTHER)
                .fileUrl(storageService.constructFileDocumentUrl(savedFileName))
                .fileName(savedFileName)
                .originalFileName(originalFileName != null ? originalFileName : "generated-document.pdf")
                .fileSize((long) pdfBytes.length)
                .fileType("application/pdf")
                .description(description)
                .version(version)
                .notes(notes)
                .isActive(true)
                .isGenerated(true) // System-generated documents
                .build();

            document = invoiceDocumentRepository.save(document);
            log.info("Generated document saved successfully: {} (ID: {})", title, document.getId());

            return getService.toDTO(document);

        } catch (Exception e) {
            log.error("Error saving generated document for invoice {}", invoiceId, e);
            return null;
        }
    }

    /**
     * Simplified overload for saving a generated document with minimal parameters.
     */
    public InvoiceDocumentDTO saveGeneratedDocument(
            Long invoiceId,
            byte[] pdfBytes,
            String originalFileName,
            DocumentType documentType,
            String title
    ) {
        return saveGeneratedDocument(invoiceId, pdfBytes, originalFileName, documentType, title, null, null, null);
    }

    /**
     * Save a generated document using obfuscated invoice ID.
     * Useful when called from controllers or external services.
     */
    public InvoiceDocumentDTO saveGeneratedDocumentByObfuscatedId(
            String obfuscatedInvoiceId,
            byte[] pdfBytes,
            String originalFileName,
            DocumentType documentType,
            String title,
            String description,
            String version,
            String notes
    ) {
        try {
            Long invoiceId = idObfuscator.decodeId(obfuscatedInvoiceId);
            return saveGeneratedDocument(invoiceId, pdfBytes, originalFileName, documentType, title, description, version, notes);
        } catch (Exception e) {
            log.error("Failed to decode invoice ID: {}", obfuscatedInvoiceId, e);
            return null;
        }
    }
}
