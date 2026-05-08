package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseDocumentServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDocumentDTOs.UploadExpenseDocumentDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpensePayment;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseDocumentRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpensePaymentRepository;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpenseDocumentUploadService {

    private final ExpenseRepository expenseRepository;
    private final ExpensePaymentRepository expensePaymentRepository;
    private final ExpenseDocumentRepository repository;
    private final ExpenseDocumentStorageService storageService;
    private final ExpenseDocumentGetService getService;
    private final IdObfuscator idObfuscator;

    @AuditLogAnnotation(
        action = "UPLOAD_EXPENSE_DOCUMENT",
        entityType = "EXPENSE_DOCUMENT",
        entityIdParamName = "expenseIdObfuscated",
        description = "Upload a proof-of-payment document for an expense"
    )
    public ResponseEntity<ApiResponse<?>> uploadDocument(String expenseIdObfuscated, UploadExpenseDocumentDTO dto) {
        try {
            Long expenseId = idObfuscator.decodeId(expenseIdObfuscated);
            Expense expense = expenseRepository.findById(expenseId).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Expense not found", "EXPENSE_NOT_FOUND"));
            }

            MultipartFile file = dto.getDocument();
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "No file provided", "FILE_REQUIRED"));
            }

            String validationError = storageService.validateDocument(file);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, validationError, "FILE_VALIDATION_FAILED"));
            }

            // Optional payment linkage
            ExpensePayment payment = null;
            if (dto.getExpensePaymentId() != null && !dto.getExpensePaymentId().isBlank()) {
                Long paymentId = idObfuscator.decodeId(dto.getExpensePaymentId());
                payment = expensePaymentRepository.findById(paymentId).orElse(null);
                if (payment != null && !payment.getExpense().getId().equals(expenseId)) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Payment does not belong to this expense",
                                "PAYMENT_EXPENSE_MISMATCH"));
                }
            }

            String savedFilename = storageService.saveDocument(file);
            if (savedFilename == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to save file to storage", "FILE_SAVE_FAILED"));
            }

            String title = dto.getTitle() != null && !dto.getTitle().isBlank()
                    ? dto.getTitle().trim()
                    : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "Expense document");

            ExpenseDocument.DocumentType type = dto.getDocumentType() != null
                    ? dto.getDocumentType() : ExpenseDocument.DocumentType.RECEIPT;

            ExpenseDocument doc = ExpenseDocument.builder()
                    .expense(expense)
                    .expensePayment(payment)
                    .title(title)
                    .documentType(type)
                    .fileUrl("")                                  // set after save (needs id)
                    .fileName(savedFilename)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(file.getContentType() != null
                            ? file.getContentType()
                            : storageService.getMimeType(file.getOriginalFilename()))
                    .description(dto.getDescription())
                    .documentNumber(dto.getDocumentNumber())
                    .version(dto.getVersion())
                    .validFrom(dto.getValidFrom())
                    .validTo(dto.getValidTo())
                    .notes(dto.getNotes())
                    .isActive(true)
                    .build();

            doc = repository.save(doc);
            doc.setFileUrl(storageService.constructDocumentUrl(idObfuscator.encodeId(doc.getId())));
            doc = repository.save(doc);

            log.info("ExpenseDocument uploaded: expense={} type={} file={}",
                    expense.getExpenseCode(), type, savedFilename);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Document uploaded", getService.toDTO(doc)));
        } catch (Exception e) {
            log.error("Error uploading expense document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to upload document: " + e.getMessage(),
                        "EXPENSE_DOCUMENT_UPLOAD_FAILED"));
        }
    }
}
