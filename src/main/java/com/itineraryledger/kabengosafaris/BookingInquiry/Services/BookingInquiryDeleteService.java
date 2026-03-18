package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import java.util.ArrayList;
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
            List<Long> ids = new ArrayList<>();
            for (String idObfuscated : idObfuscatedList) {
                try {
                    Long id = idObfuscator.decodeId(idObfuscated);
                    ids.add(id);
                } catch (Exception e) {
                    log.warn("Failed to decode ID: {}", idObfuscated, e);
                }
            }

            int deletedCount = 0;
            for (Long id : ids) {
                try {
                    if (repository.existsById(id)) {
                        ((BookingInquiryDeleteService) AopContext.currentProxy()).deleteInquiry(id);
                        deletedCount++;
                        log.info("Booking inquiry deleted successfully: {}", id);
                    } else {
                        log.warn("Booking inquiry not found: {}", id);
                    }
                } catch (Exception e) {
                    log.error("Error deleting booking inquiry: {}", id, e);
                }
            }

            return ResponseEntity.ok().body(
                ApiResponse.success(200, deletedCount + " inquiry(ies) deleted successfully", null)
            );

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
}
