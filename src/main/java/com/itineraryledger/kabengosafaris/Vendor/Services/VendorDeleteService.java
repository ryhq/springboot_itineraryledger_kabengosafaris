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
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VendorDeleteService {

    private final VendorRepository vendorRepository;
    private final IdObfuscator idObfuscator;

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

        List<Long> ids = new ArrayList<>();
        for (String s : idsObfuscated) {
            try { ids.add(idObfuscator.decodeId(s)); }
            catch (Exception e) { log.warn("Failed to decode vendor id: {}", s); }
        }

        try {
            int deleted = 0;
            for (Long id : ids) {
                Vendor vendor = vendorRepository.findById(id).orElse(null);
                if (vendor == null) continue;
                vendorRepository.deleteById(id);
                deleted++;
            }
            return ResponseEntity.ok(ApiResponse.success(200,
                deleted + " vendor(s) deleted successfully", null));
        } catch (DataIntegrityViolationException e) {
            log.warn("Vendor delete blocked by FK: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(409,
                    "One or more vendors are referenced by expenses and cannot be deleted. "
                        + "Mark them inactive instead, or delete the expenses first.",
                    "VENDOR_REFERENCED"));
        } catch (Exception e) {
            log.error("Error deleting vendors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete vendors: " + e.getMessage(),
                    "VENDOR_DELETE_FAILED"));
        }
    }
}
