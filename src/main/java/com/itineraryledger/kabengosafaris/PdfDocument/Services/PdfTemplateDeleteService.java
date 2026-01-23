package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Entity.PdfTemplate;
import com.itineraryledger.kabengosafaris.PdfDocument.Repository.PdfTemplateRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for deleting PDF templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfTemplateDeleteService {

    private final PdfTemplateRepository pdfTemplateRepository;
    private final PdfTemplateStorageService storageService;
    private final IdObfuscator idObfuscator;

    /**
     * Delete multiple PDF templates
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteTemplates(List<String> idsObfuscated) {
        try {
            if (idsObfuscated == null || idsObfuscated.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No template IDs provided", "NO_IDS_PROVIDED")
                );
            }

            List<String> deleted = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            List<String> skipped = new ArrayList<>();

            for (String idObfuscated : idsObfuscated) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    if (id == null) {
                        failed.add(idObfuscated + " (invalid ID)");
                        continue;
                    }

                    PdfTemplate template = pdfTemplateRepository.findById(id).orElse(null);
                    if (template == null) {
                        failed.add(idObfuscated + " (not found)");
                        continue;
                    }

                    // Cannot delete system default templates
                    if (template.getIsSystemDefault()) {
                        skipped.add(template.getName() + " (system default cannot be deleted)");
                        continue;
                    }

                    // Delete the file
                    storageService.deleteTemplateFile(template.getFileName());

                    // Delete from database
                    pdfTemplateRepository.delete(template);
                    deleted.add(template.getName());

                    log.info("Deleted PDF template: {}", template.getName());

                } catch (Exception e) {
                    log.error("Error deleting template: {}", idObfuscated, e);
                    failed.add(idObfuscated + " (error: " + e.getMessage() + ")");
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("deleted", deleted);
            result.put("deletedCount", deleted.size());
            result.put("failed", failed);
            result.put("failedCount", failed.size());
            result.put("skipped", skipped);
            result.put("skippedCount", skipped.size());

            if (deleted.isEmpty() && !failed.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Failed to delete templates", "DELETE_FAILED", null)
                );
            }

            String message = String.format("Deleted %d template(s)", deleted.size());
            if (!skipped.isEmpty()) {
                message += String.format(", skipped %d (system defaults)", skipped.size());
            }
            if (!failed.isEmpty()) {
                message += String.format(", failed %d", failed.size());
            }

            return ResponseEntity.ok(ApiResponse.success(200, message, result));

        } catch (Exception e) {
            log.error("Error deleting templates", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to delete templates", "DELETE_FAILED")
            );
        }
    }

    /**
     * Delete a single PDF template
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteTemplate(String idObfuscated) {
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

            // Cannot delete system default templates
            if (template.getIsSystemDefault()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "System default templates cannot be deleted", "SYSTEM_DEFAULT_PROTECTED")
                );
            }

            String templateName = template.getName();

            // Delete the file
            storageService.deleteTemplateFile(template.getFileName());

            // Delete from database
            pdfTemplateRepository.delete(template);

            log.info("Deleted PDF template: {}", templateName);
            return ResponseEntity.ok(ApiResponse.success(200, "Template deleted successfully", null));

        } catch (Exception e) {
            log.error("Error deleting template: {}", idObfuscated, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(500, "Failed to delete template", "DELETE_FAILED")
            );
        }
    }
}
