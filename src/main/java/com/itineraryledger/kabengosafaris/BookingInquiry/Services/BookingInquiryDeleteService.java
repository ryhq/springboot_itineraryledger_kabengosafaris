package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class BookingInquiryDeleteService {

    private final BookingInquiryRepository repository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public BookingInquiryDeleteService(BookingInquiryRepository repository, IdObfuscator idObfuscator) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> deleteInquiries(List<String> idObfuscatedList) {
        log.info("Deleting {} booking inquiries", idObfuscatedList.size());

        try {
            List<String> deletedIds = new ArrayList<>();
            List<Map<String, Object>> skipped = new ArrayList<>();

            for (String obfuscated : idObfuscatedList) {
                Long id;
                try {
                    id = idObfuscator.decodeId(obfuscated);
                } catch (Exception e) {
                    skipped.add(skip(obfuscated, "Unreadable id"));
                    continue;
                }

                if (!repository.existsById(id)) {
                    skipped.add(skip(obfuscated, "No longer exists"));
                    continue;
                }

                try {
                    ((BookingInquiryDeleteService) AopContext.currentProxy()).deleteInquiry(id);
                    deletedIds.add(obfuscated);
                } catch (Exception e) {
                    log.error("Error deleting inquiry: {}", id, e);
                    skipped.add(skip(obfuscated,
                        e.getMessage() != null ? e.getMessage() : "Could not be deleted"));
                }
            }

            Map<String, Object> report = new HashMap<>();
            report.put("deletedCount", deletedIds.size());
            report.put("deletedIds", deletedIds);
            report.put("skipped", skipped);

            String message = deletedIds.size()
                + (deletedIds.size() == 1 ? " inquiry deleted" : " inquiries deleted")
                + (skipped.isEmpty() ? "" : ", " + skipped.size() + " skipped");

            return ResponseEntity.ok().body(ApiResponse.success(200, message, report));

        } catch (Exception e) {
            log.error("Error deleting booking inquiries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete booking inquiries", "INQUIRIES_DELETE_FAILED")
            );
        }
    }

    @AuditLogAnnotation(action = "DELETE_BOOKING_INQUIRY", description = "Deleting booking inquiry", entityType = "BookingInquiry", entityIdParamName = "id")
    public void deleteInquiry(Long id) {
        repository.deleteById(id);
    }

    /**
     * Why one id did not go.
     *
     * A caller that asked about six needs to know which four survived and why —
     * a bare count leaves them guessing, and guessing about deletions is worse
     * than being told.
     */
    private Map<String, Object> skip(String id, String reason) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("reason", reason);
        return entry;
    }
}
