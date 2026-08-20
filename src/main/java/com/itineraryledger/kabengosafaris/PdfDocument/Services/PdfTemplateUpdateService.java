package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.PdfTemplateDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.DTOs.UpdatePdfTemplateDTO;
import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.List;

/**
 * Service for updating PDF templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfTemplateUpdateService {

    private final PdfTemplateRepository pdfTemplateRepository;
    private final PdfTemplateStorageService storageService;
    private final IdObfuscator idObfuscator;

    /**
     * Update an existing PDF template
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateTemplate(String idObfuscated, UpdatePdfTemplateDTO updateDTO) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid template ID", "INVALID_TEMPLATE_ID")
                );
            }

            PdfTemplate template = pdfTemplateRepository.findById(id).orElse(null);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            // Check for duplicate name if name is being changed
            if (updateDTO.getName() != null && !updateDTO.getName().equals(template.getName())) {
                if (pdfTemplateRepository.existsByPdfDocumentIdAndName(
                    template.getPdfDocument().getId(), updateDTO.getName())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Template with this name already exists for this document type",
                            "DUPLICATE_TEMPLATE_NAME")
                    );
                }
                template.setName(updateDTO.getName());
            }

            // Update other fields if provided
            if (updateDTO.getDescription() != null) {
                template.setDescription(updateDTO.getDescription());
            }
            if (updateDTO.getPaperSize() != null) {
                template.setPaperSize(updateDTO.getPaperSize().isBlank() ? null : com.itineraryledger.kabengosafaris.PdfDocument.Entity.PaperSize.valueOf(updateDTO.getPaperSize().trim()));
            }
            if (updateDTO.getOrientation() != null) {
                template.setOrientation(updateDTO.getOrientation().isBlank() ? null : com.itineraryledger.kabengosafaris.PdfDocument.Entity.Orientation.valueOf(updateDTO.getOrientation().trim()));
            }
            if (updateDTO.getMarginTop() != null) {
                template.setMarginTop(updateDTO.getMarginTop());
            }
            if (updateDTO.getMarginBottom() != null) {
                template.setMarginBottom(updateDTO.getMarginBottom());
            }
            if (updateDTO.getMarginLeft() != null) {
                template.setMarginLeft(updateDTO.getMarginLeft());
            }
            if (updateDTO.getMarginRight() != null) {
                template.setMarginRight(updateDTO.getMarginRight());
            }
            if (updateDTO.getEnabled() != null) {
                template.setEnabled(updateDTO.getEnabled());
            }
            if (updateDTO.getVersion() != null) {
                template.setVersion(updateDTO.getVersion());
            }

            // Handle default flag
            if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault() && !template.getIsDefault()) {
                unsetOtherDefaults(template.getPdfDocument().getId());
                template.setIsDefault(true);
            } else if (updateDTO.getIsDefault() != null && !updateDTO.getIsDefault()) {
                template.setIsDefault(false);
            }

            // Update content if provided
            if (updateDTO.getContent() != null) {
                boolean contentUpdated = storageService.updateTemplateFile(template.getFileName(), updateDTO.getContent());
                if (!contentUpdated) {
                    return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Failed to update template content", "CONTENT_UPDATE_FAILED")
                    );
                }
                template.setFileSize((long) updateDTO.getContent().getBytes().length);
            }

            PdfTemplate savedTemplate = pdfTemplateRepository.save(template);
            PdfTemplateDTO responseDTO = mapToDTO(savedTemplate);

            log.info("Updated PDF template: {}", savedTemplate.getName());
            return ResponseEntity.ok(ApiResponse.success(200, "Template updated successfully", responseDTO));

        } catch (Exception e) {
            log.error("Error updating PDF template: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to update template", "TEMPLATE_UPDATE_FAILED")
            );
        }
    }

    /**
     * Restore EVERY system-default template in one call.
     *
     * Shipping a template fix is not a deploy: seeded files are never overwritten, which is what
     * protects somebody's edits — so a corrected default reaches an existing installation only when
     * somebody restores it. One template at a time across sixteen documents is a chore nobody
     * finishes, and a half-restored set is worse than an old one.
     *
     * Reports per template rather than failing on the first problem: one document whose resource is
     * missing must not stop the other fifteen.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> restoreAllSystemDefaults() {
        List<PdfTemplate> templates = pdfTemplateRepository.findAll().stream()
            .filter(t -> Boolean.TRUE.equals(t.getIsSystemDefault()))
            .toList();

        List<String> restored = new java.util.ArrayList<>();
        List<Map<String, String>> skipped = new java.util.ArrayList<>();

        for (PdfTemplate template : templates) {
            try {
                String original = storageService.loadSystemDefaultTemplate(template.getPdfDocument().getName());
                if (original == null || original.isBlank()) {
                    skipped.add(Map.of("template", template.getName(),
                        "reason", "no packaged default for " + template.getPdfDocument().getName()));
                    continue;
                }
                if (!storageService.updateTemplateFile(template.getFileName(), original)) {
                    skipped.add(Map.of("template", template.getName(), "reason", "could not write the file"));
                    continue;
                }
                template.setFileSize((long) original.getBytes().length);
                pdfTemplateRepository.save(template);
                restored.add(template.getPdfDocument().getName() + " / " + template.getName());
            } catch (Exception e) {
                log.error("Could not restore PDF template {}", template.getName(), e);
                skipped.add(Map.of("template", template.getName(),
                    "reason", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }

        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("restoredCount", restored.size());
        data.put("restored", restored);
        data.put("skipped", skipped);

        log.info("Restored {} of {} system-default PDF templates", restored.size(), templates.size());
        String message = skipped.isEmpty()
            ? restored.size() + " template(s) restored to the shipped default"
            : restored.size() + " restored, " + skipped.size() + " could not be";
        return ResponseEntity.ok(ApiResponse.success(200, message, data));
    }

    /**
     * Restore system default template to original content
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> restoreSystemDefault(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            if (id == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid template ID", "INVALID_TEMPLATE_ID")
                );
            }

            PdfTemplate template = pdfTemplateRepository.findById(id).orElse(null);
            if (template == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Template not found", "TEMPLATE_NOT_FOUND")
                );
            }

            if (!template.getIsSystemDefault()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Only system default templates can be restored", "NOT_SYSTEM_DEFAULT")
                );
            }

            // Load original content from resources
            String originalContent = storageService.loadSystemDefaultTemplate(template.getPdfDocument().getName());

            // Update the file
            boolean updated = storageService.updateTemplateFile(template.getFileName(), originalContent);
            if (!updated) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500, "Failed to restore template content", "RESTORE_FAILED")
                );
            }

            template.setFileSize((long) originalContent.getBytes().length);
            PdfTemplate savedTemplate = pdfTemplateRepository.save(template);

            PdfTemplateDTO responseDTO = mapToDTO(savedTemplate);
            log.info("Restored system default template: {}", savedTemplate.getName());

            return ResponseEntity.ok(ApiResponse.success(200, "Template restored to original", responseDTO));

        } catch (Exception e) {
            log.error("Error restoring system default template: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to restore template", "RESTORE_FAILED")
            );
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
