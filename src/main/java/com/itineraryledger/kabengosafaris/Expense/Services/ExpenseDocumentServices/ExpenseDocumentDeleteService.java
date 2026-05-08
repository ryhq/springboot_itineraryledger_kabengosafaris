package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseDocumentServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class ExpenseDocumentDeleteService {

    private final ExpenseDocumentRepository repository;
    private final ExpenseDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "DELETE_EXPENSE_DOCUMENT",
        entityType = "EXPENSE_DOCUMENT",
        description = "Delete one or more expense documents"
    )
    public ResponseEntity<ApiResponse<?>> deleteDocuments(List<String> idsObfuscated) {
        if (idsObfuscated == null || idsObfuscated.isEmpty()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "No document IDs provided", "INVALID_IDS"));
        }

        List<Long> ids = new ArrayList<>();
        for (String s : idsObfuscated) {
            try { ids.add(idObfuscator.decodeId(s)); }
            catch (Exception e) { log.warn("Failed to decode document id: {}", s); }
        }

        try {
            int deleted = 0;
            for (Long id : ids) {
                ExpenseDocument doc = repository.findById(id).orElse(null);
                if (doc == null) continue;

                String fileName = doc.getFileName();
                repository.deleteById(id);
                if (fileName != null) {
                    storageService.deleteDocument(fileName);
                }
                deleted++;
            }
            return ResponseEntity.ok(ApiResponse.success(200,
                deleted + " document(s) deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting expense documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete documents: " + e.getMessage(),
                        "EXPENSE_DOCUMENTS_DELETE_FAILED"));
        }
    }
}
