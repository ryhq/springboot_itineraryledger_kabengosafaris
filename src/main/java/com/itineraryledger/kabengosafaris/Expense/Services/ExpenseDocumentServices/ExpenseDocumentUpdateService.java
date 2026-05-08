package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseDocumentServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDocumentDTOs.UpdateExpenseDocumentDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseDocumentRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpenseDocumentUpdateService {

    private final ExpenseDocumentRepository repository;
    private final ExpensePaymentRepository expensePaymentRepository;
    private final ExpenseDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPDATE_EXPENSE_DOCUMENT",
        entityType = "EXPENSE_DOCUMENT",
        entityIdParamName = "idObfuscated",
        description = "Update metadata on an expense document"
    )
    public ResponseEntity<ApiResponse<?>> updateDocument(String idObfuscated, UpdateExpenseDocumentDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            ExpenseDocument doc = repository.findById(id).orElse(null);
            if (doc == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Document not found", "EXPENSE_DOCUMENT_NOT_FOUND"));
            }

            if (dto.getTitle() != null) doc.setTitle(dto.getTitle().trim());
            if (dto.getDocumentType() != null) doc.setDocumentType(dto.getDocumentType());
            if (dto.getDescription() != null) doc.setDescription(dto.getDescription());
            if (dto.getDocumentNumber() != null) doc.setDocumentNumber(dto.getDocumentNumber());
            if (dto.getVersion() != null) doc.setVersion(dto.getVersion());
            if (dto.getNotes() != null) doc.setNotes(dto.getNotes());
            if (dto.getValidFrom() != null) doc.setValidFrom(dto.getValidFrom());
            if (dto.getValidTo() != null) doc.setValidTo(dto.getValidTo());
            if (dto.getIsActive() != null) doc.setIsActive(dto.getIsActive());

            if (dto.getExpensePaymentId() != null) {
                if (dto.getExpensePaymentId().isBlank()) {
                    doc.setExpensePayment(null);
                } else {
                    Long paymentId = idObfuscator.decodeId(dto.getExpensePaymentId());
                    ExpensePayment p = expensePaymentRepository.findById(paymentId).orElse(null);
                    if (p == null || !p.getExpense().getId().equals(doc.getExpense().getId())) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Payment does not belong to this expense",
                                    "PAYMENT_EXPENSE_MISMATCH"));
                    }
                    doc.setExpensePayment(p);
                }
            }

            doc = repository.save(doc);
            return ResponseEntity.ok(ApiResponse.success(200, "Document updated", getService.toDTO(doc)));
        } catch (Exception e) {
            log.error("Error updating expense document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update document: " + e.getMessage(),
                        "EXPENSE_DOCUMENT_UPDATE_FAILED"));
        }
    }
}
