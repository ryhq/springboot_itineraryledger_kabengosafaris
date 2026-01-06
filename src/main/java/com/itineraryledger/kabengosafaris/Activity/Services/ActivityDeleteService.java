package com.itineraryledger.kabengosafaris.Activity.Services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * ActivityDeleteService - Service for deleting activities
 */
@Service
@Slf4j
@Transactional
public class ActivityDeleteService {

    private final ActivityRepository activityRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public ActivityDeleteService(
        ActivityRepository activityRepository,
        IdObfuscator idObfuscator
    ) {
        this.activityRepository = activityRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Delete activities by list of obfuscated IDs
     *
     * @param idObfuscatedList List of obfuscated activity IDs
     * @return ResponseEntity with ApiResponse containing success or error
     */
    public ResponseEntity<ApiResponse<?>> deleteActivities(List<String> idObfuscatedList) {
        log.info("Deleting {} activities", idObfuscatedList.size());

        try {
            // Decode all obfuscated IDs
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            return deleteActivitiesInternal(ids);

        } catch (Exception e) {
            log.error("Error deleting activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(
                    500,
                    "Failed to delete activities",
                    "ACTIVITIES_DELETE_FAILED"
                )
            );
        }
    }

    /**
     * Delete activities by list of IDs (internal method)
     */
    private ResponseEntity<ApiResponse<?>> deleteActivitiesInternal(List<Long> ids) {
        int deletedCount = 0;

        for (Long id : ids) {
            try {
                Activity activity = activityRepository.findById(id).orElse(null);

                if (activity == null) {
                    log.warn("Activity not found: {}", id);
                    continue;
                }

                // Use AopContext to get proxy and trigger AOP aspect
                ((ActivityDeleteService) AopContext.currentProxy()).deleteActivity(id);
                deletedCount++;
                log.info("Activity deleted successfully: {}", id);

            } catch (Exception e) {
                log.error("Error deleting activity: {}", id, e);
            }
        }

        return ResponseEntity.ok().body(
            ApiResponse.success(
                200,
                deletedCount + " activity(ies) deleted successfully",
                null
            )
        );
    }

    @AuditLogAnnotation(action = "DELETE_ACTIVITY", description = "Deleting activity", entityType = "Activity", entityIdParamName = "id")
    public void deleteActivity(Long id) {
        activityRepository.deleteById(id);
    }
}
