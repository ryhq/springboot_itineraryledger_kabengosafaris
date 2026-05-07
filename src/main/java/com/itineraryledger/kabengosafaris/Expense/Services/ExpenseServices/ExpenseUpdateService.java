package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.DTOs.UpdateExpenseDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.Expense;
import com.itineraryledger.kabengosafaris.Expense.Enums.ExpenseStatus;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;
import com.itineraryledger.kabengosafaris.User.UserRepository;
import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ExpenseUpdateService {

    private final ExpenseRepository expenseRepository;
    private final VendorRepository vendorRepository;
    private final SafariRepository safariRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final ExpenseGetService expenseGetService;
    private final ExpenseTotalsCalculationService totalsService;

    @AuditLogAnnotation(
        action = "UPDATE_EXPENSE",
        entityType = "EXPENSE",
        entityIdParamName = "idObfuscated",
        description = "Update expense details"
    )
    public ResponseEntity<ApiResponse<?>> updateExpense(String idObfuscated, UpdateExpenseDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            Expense expense = expenseRepository.findById(id).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Expense not found", "EXPENSE_NOT_FOUND"));
            }

            // Status transition handling first
            if (dto.getStatus() != null && dto.getStatus() != expense.getStatus()) {
                if (expense.getStatus() != null && !expense.getStatus().canTransitionTo(dto.getStatus())) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                            "Invalid status transition: " + expense.getStatus() + " → " + dto.getStatus(),
                            "INVALID_STATUS_TRANSITION"));
                }
                expense.setStatus(dto.getStatus());
            }

            boolean lockedForEdit = !expense.isEditable() && dto.getStatus() == null;
            // Once not in DRAFT, only safe metadata fields are editable
            if (!lockedForEdit) {
                if (dto.getTitle() != null) expense.setTitle(dto.getTitle().trim());
                if (dto.getDescription() != null) expense.setDescription(trimToNull(dto.getDescription()));
                if (dto.getExpenseDate() != null) expense.setExpenseDate(dto.getExpenseDate());
                if (dto.getDueDate() != null) expense.setDueDate(dto.getDueDate());
                if (dto.getReferenceNumber() != null) expense.setReferenceNumber(trimToNull(dto.getReferenceNumber()));
                if (dto.getTaxPercentage() != null) expense.setTaxPercentage(dto.getTaxPercentage());

                if (dto.getVendorId() != null && !dto.getVendorId().isBlank()) {
                    Long vid = idObfuscator.decodeId(dto.getVendorId());
                    Vendor vendor = vendorRepository.findById(vid).orElse(null);
                    if (vendor == null) {
                        return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Vendor not found", "VENDOR_NOT_FOUND"));
                    }
                    expense.setVendor(vendor);
                }

                if (dto.getSafariId() != null) {
                    if (dto.getSafariId().isBlank()) {
                        expense.setSafari(null);   // detach
                    } else {
                        Long sid = idObfuscator.decodeId(dto.getSafariId());
                        Safari safari = safariRepository.findById(sid).orElse(null);
                        if (safari == null) {
                            return ResponseEntity.badRequest().body(
                                ApiResponse.error(400, "Safari not found", "SAFARI_NOT_FOUND"));
                        }
                        expense.setSafari(safari);
                    }
                }
            }

            // Always-editable fields (regardless of status)
            if (dto.getInternalNotes() != null) expense.setInternalNotes(trimToNull(dto.getInternalNotes()));
            if (dto.getIsActive() != null) expense.setIsActive(dto.getIsActive());

            expense.setUpdatedBy(getCurrentUser());
            expense = expenseRepository.save(expense);

            // Recalculate when tax % changes (line items unchanged here)
            if (dto.getTaxPercentage() != null) {
                totalsService.recalculateTotals(expense);
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Expense updated successfully",
                    expenseGetService.toDTO(expense)));
        } catch (Exception e) {
            log.error("Error updating expense", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update expense: " + e.getMessage(),
                        "EXPENSE_UPDATE_FAILED"));
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }
}
