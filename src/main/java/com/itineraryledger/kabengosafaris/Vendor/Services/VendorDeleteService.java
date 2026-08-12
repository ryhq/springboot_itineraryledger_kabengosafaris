package com.itineraryledger.kabengosafaris.Vendor.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VendorDeleteService {

    private final VendorRepository vendorRepository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository expenseRepository;
    private final com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository accommodationRepository;

    @AuditLogAnnotation(
        action = "DELETE_VENDOR",
        entityType = "VENDOR",
        description = "Delete one or more vendors"
    )
    public ResponseEntity<ApiResponse<?>> deleteVendors(List<String> idsObfuscated) {
        if (idsObfuscated == null || idsObfuscated.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No vendor IDs provided", "INVALID_IDS"));
        }

        List<String> deletedIds = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();

        for (String obfuscated : idsObfuscated) {
            Long id;
            try {
                id = idObfuscator.decodeId(obfuscated);
            } catch (Exception e) {
                skipped.add(skip(obfuscated, null, "Unreadable id"));
                continue;
            }

            Vendor vendor = vendorRepository.findById(id).orElse(null);
            if (vendor == null) {
                skipped.add(skip(obfuscated, null, "No longer exists"));
                continue;
            }

            /*
             * Reference-checked per vendor, before anything is removed.
             *
             * A DataIntegrityViolation would have told us "one or more" failed
             * and rolled the batch back, which is no help to somebody who asked
             * about six. Counting first says which, and why.
             */
            long bills = expenseRepository.countByVendorId(id);
            if (bills > 0) {
                skipped.add(skip(obfuscated, vendor.getCode(),
                    bills + (bills == 1 ? " bill is" : " bills are") + " recorded against them."
                        + " They are the record of what we paid — deactivate the vendor instead."));
                continue;
            }

            long properties = accommodationRepository.countByVendorId(id);
            if (properties > 0) {
                skipped.add(skip(obfuscated, vendor.getCode(),
                    properties + (properties == 1 ? " property settles" : " properties settle")
                        + " with them. Point those elsewhere first, or deactivate the vendor."));
                continue;
            }

            try {
                vendorRepository.deleteById(id);
                deletedIds.add(obfuscated);
            } catch (Exception e) {
                log.warn("Vendor delete failed for {}", obfuscated, e);
                skipped.add(skip(obfuscated, vendor.getCode(),
                    e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deletedIds.size());
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        String message = deletedIds.size() + (deletedIds.size() == 1 ? " vendor deleted" : " vendors deleted")
            + (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped");

        return ResponseEntity.ok(ApiResponse.success(200, message, report));
    }

    private Map<String, Object> skip(String id, String code, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("code", code);
        entry.put("reason", reason);
        return entry;
    }
}
