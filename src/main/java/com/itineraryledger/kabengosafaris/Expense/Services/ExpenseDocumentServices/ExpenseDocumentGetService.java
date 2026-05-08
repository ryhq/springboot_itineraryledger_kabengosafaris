package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseDocumentServices;

import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDocumentDTOs.ExpenseDocumentDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseDocumentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseDocumentGetService {

    private final ExpenseDocumentRepository repository;
    private final ExpenseDocumentStorageService storageService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> listForExpense(String expenseIdObfuscated) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            List<ExpenseDocument> docs = repository.findByExpenseIdOrderByCreatedAtDesc(expenseId);
            return ResponseEntity.ok(ApiResponse.success(200, "Documents retrieved",
                    docs.stream().map(this::toDTO).toList()));
        } catch (Exception e) {
            log.error("Error listing expense documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch documents", "EXPENSE_DOCUMENTS_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            ExpenseDocument doc = repository.findById(id).orElse(null);
            if (doc == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Document not found", "EXPENSE_DOCUMENT_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Document retrieved", toDTO(doc)));
        } catch (Exception e) {
            log.error("Error fetching expense document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch document", "EXPENSE_DOCUMENT_FETCH_FAILED"));
        }
    }

    /** Stream the file bytes back. Inline disposition so PDF/images can preview in-browser. */
    public ResponseEntity<?> downloadFile(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            ExpenseDocument doc = repository.findById(id).orElse(null);
            if (doc == null || doc.getFileName() == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Document not found", "EXPENSE_DOCUMENT_NOT_FOUND"));
            }
            byte[] bytes = storageService.readDocumentBytes(doc.getFileName());
            if (bytes == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "File missing on disk", "EXPENSE_DOCUMENT_FILE_MISSING"));
            }
            String mime = doc.getFileType() != null ? doc.getFileType()
                    : storageService.getMimeType(doc.getFileName());
            String safeName = doc.getOriginalFileName() != null ? doc.getOriginalFileName() : doc.getFileName();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mime))
                    .header("Content-Disposition", "inline; filename=\"" + safeName + "\"")
                    .body(bytes);
        } catch (Exception e) {
            log.error("Error downloading expense document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to download document", "EXPENSE_DOCUMENT_DOWNLOAD_FAILED"));
        }
    }

    public ExpenseDocumentDTO toDTO(ExpenseDocument d) {
        return ExpenseDocumentDTO.builder()
                .id(idObfuscator.encodeId(d.getId()))
                .expenseId(d.getExpense() != null ? idObfuscator.encodeId(d.getExpense().getId()) : null)
                .expenseCode(d.getExpense() != null ? d.getExpense().getExpenseCode() : null)
                .expensePaymentId(d.getExpensePayment() != null
                        ? idObfuscator.encodeId(d.getExpensePayment().getId()) : null)
                .title(d.getTitle())
                .documentType(d.getDocumentType())
                .documentTypeDisplayName(d.getDocumentType() != null ? d.getDocumentType().getDisplayName() : null)
                .fileUrl(d.getFileUrl())
                .fileName(d.getFileName())
                .originalFileName(d.getOriginalFileName())
                .fileSize(d.getFileSize())
                .fileType(d.getFileType())
                .description(d.getDescription())
                .documentNumber(d.getDocumentNumber())
                .version(d.getVersion())
                .validFrom(d.getValidFrom())
                .validTo(d.getValidTo())
                .isActive(d.getIsActive())
                .isCurrentlyValid(d.isCurrentlyValid())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
