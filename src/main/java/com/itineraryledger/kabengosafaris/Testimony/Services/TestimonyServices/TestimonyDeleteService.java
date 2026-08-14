package com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyServices;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Entity.TestimonyImage;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyImageRepository;
import com.itineraryledger.kabengosafaris.Testimony.Repository.TestimonyRepository;
import com.itineraryledger.kabengosafaris.Testimony.Services.TestimonyImageServices.TestimonyImageStorageService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class TestimonyDeleteService {

    private final TestimonyRepository testimonyRepository;
    private final TestimonyImageRepository testimonyImageRepository;
    private final TestimonyImageStorageService storageService;
    private final IdObfuscator idObfuscator;

    @Autowired
    public TestimonyDeleteService(
        TestimonyRepository testimonyRepository,
        TestimonyImageRepository testimonyImageRepository,
        TestimonyImageStorageService storageService,
        IdObfuscator idObfuscator
    ) {
        this.testimonyRepository = testimonyRepository;
        this.testimonyImageRepository = testimonyImageRepository;
        this.storageService = storageService;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> deleteTestimonies(List<String> idObfuscatedList) {
        log.info("Deleting {} testimonies", idObfuscatedList.size());

        try {
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteTestimoniesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting testimonies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete testimonies", "TESTIMONIES_DELETE_FAILED")
            );
        }
    }

    private ResponseEntity<ApiResponse<?>> deleteTestimoniesInternal(List<Long> ids) {
        int deletedCount = 0;
        /*
         * Per-row outcomes, because this used to answer "200, none deleted" in the same
         * words as "200, all deleted" — a row that was missing or that threw was logged
         * here and never mentioned to the caller.
         */
        List<String> deletedIds = new java.util.ArrayList<>();
        List<java.util.Map<String, Object>> skipped = new java.util.ArrayList<>();

        for (Long id : ids) {
            try {
                Testimony testimony = testimonyRepository.findById(id).orElse(null);

                if (testimony == null) {
                    log.warn("Testimony not found: {}", id);
                    skipped.add(skip(id, null, "No such review — it may already have been deleted"));
                    continue;
                }

                // Delete all associated images from filesystem
                List<TestimonyImage> images = testimonyImageRepository.findByTestimonyId(id);
                for (TestimonyImage image : images) {
                    if (image.getFileName() != null) {
                        storageService.deleteImage(image.getFileName());
                    }
                }

                String author = testimony.getAuthorName();
                ((TestimonyDeleteService) AopContext.currentProxy()).deleteTestimony(id);
                deletedCount++;
                deletedIds.add(idObfuscator.encodeId(id));
                log.info("Testimony deleted successfully: {} ({})", id, author);

            } catch (Exception e) {
                log.error("Error deleting testimony: {}", id, e);
                skipped.add(skip(id, null, e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
            }
        }

        java.util.Map<String, Object> report = new java.util.HashMap<>();
        report.put("deletedCount", deletedCount);
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " testimony(ies) deleted successfully", report)
        );
    }

    /** One skipped row, named the way the caller can show it. */
    private java.util.Map<String, Object> skip(Long id, String label, String reason) {
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("id", idObfuscator.encodeId(id));
        row.put("code", label);
        row.put("reason", reason);
        return row;
    }

    @AuditLogAnnotation(action = "DELETE_TESTIMONY", description = "Deleting testimony", entityType = "Testimony", entityIdParamName = "id")
    public void deleteTestimony(Long id) {
        testimonyRepository.deleteById(id);
    }
}
