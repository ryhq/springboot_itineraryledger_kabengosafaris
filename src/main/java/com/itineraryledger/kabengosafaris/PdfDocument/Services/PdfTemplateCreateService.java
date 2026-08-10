package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.CreatePdfTemplateDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.PdfTemplateDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfDocument;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfDocumentRepository;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating PDF templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfTemplateCreateService {

    private final PdfTemplateRepository pdfTemplateRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTemplateStorageService storageService;
    private final IdObfuscator idObfuscator;

    /**
     * Create a new PDF template for a document type
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> createTemplate(String documentIdObfuscated, CreatePdfTemplateDTO createDTO) {
        try {
            // Decode document ID
            Long documentId = idObfuscator.decodeId(documentIdObfuscated);
            if (documentId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid document ID", "INVALID_DOCUMENT_ID")
                );
            }

            // Find document type
            PdfDocument document = pdfDocumentRepository.findById(documentId).orElse(null);
            if (document == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "PDF document type not found", "DOCUMENT_NOT_FOUND")
                );
            }

            // Check for duplicate name
            if (pdfTemplateRepository.existsByPdfDocumentIdAndName(documentId, createDTO.getName())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Template with this name already exists for this document type",
                        "DUPLICATE_TEMPLATE_NAME")
                );
            }

            // Generate unique filename
            String fileName = storageService.generateFileName(document.getName(), createDTO.getName());

            // Save template file
            boolean fileSaved = storageService.saveTemplateFile(createDTO.getContent(), fileName);
            if (!fileSaved) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to save template file", "FILE_SAVE_FAILED")
                );
            }

            // Create template entity
            PdfTemplate template = PdfTemplate.builder()
                .pdfDocument(document)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .fileName(fileName)
                .paperSize(createDTO.getPaperSize() != null ? createDTO.getPaperSize() : PaperSize.A4)
                .orientation(createDTO.getOrientation() != null ? createDTO.getOrientation() : Orientation.PORTRAIT)
                .marginTop(createDTO.getMarginTop() != null ? createDTO.getMarginTop() : 20)
                .marginBottom(createDTO.getMarginBottom() != null ? createDTO.getMarginBottom() : 20)
                .marginLeft(createDTO.getMarginLeft() != null ? createDTO.getMarginLeft() : 15)
                .marginRight(createDTO.getMarginRight() != null ? createDTO.getMarginRight() : 15)
                .isDefault(createDTO.getIsDefault() != null && createDTO.getIsDefault())
                .isSystemDefault(false)
                .enabled(createDTO.getEnabled() != null ? createDTO.getEnabled() : true)
                .fileSize((long) createDTO.getContent().getBytes().length)
                .version(createDTO.getVersion())
                .build();

            // If setting as default, unset other defaults
            if (template.getIsDefault()) {
                unsetOtherDefaults(documentId);
            }

            PdfTemplate savedTemplate = pdfTemplateRepository.save(template);

            PdfTemplateDTO responseDTO = mapToDTO(savedTemplate);
            log.info("Created PDF template: {} for document: {}", savedTemplate.getName(), document.getName());

            return ResponseEntity.status(201).body(
                ApiResponse.success(201, "PDF template created successfully", responseDTO)
            );

        } catch (Exception e) {
            log.error("Error creating PDF template", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to create PDF template", "TEMPLATE_CREATE_FAILED")
            );
        }
    }

    /**
     * Create system default template for a document type (used by initializer)
     */
    @Transactional
    public boolean createSystemDefaultTemplate(PdfDocument document) {
        try {
            // Check if system default already exists
            if (pdfTemplateRepository.hasSystemDefaultTemplate(document.getId())) {
                log.debug("System default template already exists for: {}", document.getName());
                return true;
            }

            // Load template content from resources
            String content = storageService.loadSystemDefaultTemplate(document.getName());

            // Generate filename
            String fileName = storageService.generateFileName(document.getName(), "default");

            // Save to disk
            boolean fileSaved = storageService.saveTemplateFile(content, fileName);
            if (!fileSaved) {
                // File might already exist, try reading to confirm
                if (!storageService.templateFileExists(fileName)) {
                    log.error("Failed to save system default template file for: {}", document.getName());
                    return false;
                }
            }

            // Create template entity
            PdfTemplate template = PdfTemplate.builder()
                .pdfDocument(document)
                .name("Default")
                .description("System default template for " + document.getDisplayName())
                .fileName(fileName)
                .paperSize(PaperSize.A4)
                .orientation(Orientation.PORTRAIT)
                .marginTop(20)
                .marginBottom(20)
                .marginLeft(15)
                .marginRight(15)
                .isDefault(true)
                .isSystemDefault(true)
                .enabled(true)
                .fileSize((long) content.getBytes().length)
                .version("1.0")
                .build();

            pdfTemplateRepository.save(template);
            log.info("Created system default PDF template for: {}", document.getName());
            return true;

        } catch (Exception e) {
            log.error("Error creating system default template for: {}", document.getName(), e);
            return false;
        }
    }

    /**
     * Registers a shipped template that is NOT the default.
     *
     * Idempotent by name, so a restart does not accumulate copies, and silent
     * when the file is absent — an extra template that never shipped should
     * simply not exist, not break startup.
     */
    public boolean createSeededTemplate(PdfDocument document, String suffix, String name, String description) {
        try {
            boolean exists = pdfTemplateRepository.findByPdfDocumentId(document.getId()).stream()
                .anyMatch(t -> name.equalsIgnoreCase(t.getName()));
            if (exists) {
                log.debug("Seeded template already present: {} / {}", document.getName(), name);
                return true;
            }

            String content = storageService.loadSeedTemplate(document.getName(), suffix);
            if (content == null) return false;

            String fileName = storageService.generateFileName(document.getName(), suffix);
            if (!storageService.saveTemplateFile(content, fileName)
                    && !storageService.templateFileExists(fileName)) {
                log.error("Failed to save seeded template file: {}", fileName);
                return false;
            }

            pdfTemplateRepository.save(PdfTemplate.builder()
                .pdfDocument(document)
                .name(name)
                .description(description)
                .fileName(fileName)
                .paperSize(PaperSize.A4)
                .orientation(Orientation.PORTRAIT)
                .marginTop(20)
                .marginBottom(20)
                .marginLeft(15)
                .marginRight(15)
                // never the default: the default is what an unqualified request gets
                .isDefault(false)
                .isSystemDefault(false)
                .enabled(true)
                .fileSize((long) content.getBytes().length)
                .version("1.0")
                .build());

            log.info("Created seeded template '{}' for: {}", name, document.getName());
            return true;

        } catch (Exception e) {
            log.error("Error creating seeded template '{}' for: {}", name, document.getName(), e);
            return false;
        }
    }

    /**
     * Unset default flag on other templates for the same document type
     */
    private void unsetOtherDefaults(Long documentId) {
        pdfTemplateRepository.findByPdfDocumentId(documentId).stream()
            .filter(PdfTemplate::getIsDefault)
            .forEach(t -> {
                t.setIsDefault(false);
                pdfTemplateRepository.save(t);
            });
    }

    /**
     * Map entity to DTO
     */
    private PdfTemplateDTO mapToDTO(PdfTemplate template) {
        return PdfTemplateDTO.builder()
            .id(idObfuscator.encodeId(template.getId()))
            .pdfDocumentId(idObfuscator.encodeId(template.getPdfDocument().getId()))
            .pdfDocumentName(template.getPdfDocument().getName())
            .pdfDocumentDisplayName(template.getPdfDocument().getDisplayName())
            .name(template.getName())
            .description(template.getDescription())
            .fileName(template.getFileName())
            .paperSize(template.getPaperSize())
            .paperSizeDisplayName(template.getPaperSize().getDisplayName())
            .orientation(template.getOrientation())
            .orientationDisplayName(template.getOrientation().getDisplayName())
            .marginTop(template.getMarginTop())
            .marginBottom(template.getMarginBottom())
            .marginLeft(template.getMarginLeft())
            .marginRight(template.getMarginRight())
            .isDefault(template.getIsDefault())
            .isSystemDefault(template.getIsSystemDefault())
            .enabled(template.getEnabled())
            .fileSize(template.getFileSize())
            .fileSizeFormatted(storageService.formatFileSize(template.getFileSize()))
            .version(template.getVersion())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }
}
