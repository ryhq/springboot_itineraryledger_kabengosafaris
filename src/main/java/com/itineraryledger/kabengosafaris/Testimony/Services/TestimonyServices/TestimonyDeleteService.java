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

        for (Long id : ids) {
            try {
                Testimony testimony = testimonyRepository.findById(id).orElse(null);

                if (testimony == null) {
                    log.warn("Testimony not found: {}", id);
                    continue;
                }

                // Delete all associated images from filesystem
                List<TestimonyImage> images = testimonyImageRepository.findByTestimonyId(id);
                for (TestimonyImage image : images) {
                    if (image.getFileName() != null) {
                        storageService.deleteImage(image.getFileName());
                    }
                }

                ((TestimonyDeleteService) AopContext.currentProxy()).deleteTestimony(id);
                deletedCount++;
                log.info("Testimony deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting testimony: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(200, deletedCount + " testimony(ies) deleted successfully", null)
        );
    }

    @AuditLogAnnotation(action = "DELETE_TESTIMONY", description = "Deleting testimony", entityType = "Testimony", entityIdParamName = "id")
    public void deleteTestimony(Long id) {
        testimonyRepository.deleteById(id);
    }
}
