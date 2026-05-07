package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseServices;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Expense.DTOs.CreateExpenseDTO;
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
public class ExpenseCreateService {

    private final ExpenseRepository expenseRepository;
    private final VendorRepository vendorRepository;
    private final SafariRepository safariRepository;
    private final UserRepository userRepository;
    private final IdObfuscator idObfuscator;
    private final ExpenseGetService expenseGetService;

    @AuditLogAnnotation(
        action = "CREATE_EXPENSE",
        entityType = "EXPENSE",
        description = "Record a new expense"
    )
    public ResponseEntity<ApiResponse<?>> createExpense(CreateExpenseDTO dto) {
        try {
            Long vendorId = idObfuscator.decodeId(dto.getVendorId());
            Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
            if (vendor == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Vendor not found", "VENDOR_NOT_FOUND"));
            }

            Safari safari = null;
            if (dto.getSafariId() != null && !dto.getSafariId().isBlank()) {
                Long safariId = idObfuscator.decodeId(dto.getSafariId());
                safari = safariRepository.findById(safariId).orElse(null);
                if (safari == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Safari not found", "SAFARI_NOT_FOUND"));
                }
            }

            User currentUser = getCurrentUser();

            Expense expense = Expense.builder()
                .expenseCode("TEMP")
                .title(dto.getTitle().trim())
                .description(trimToNull(dto.getDescription()))
                .vendor(vendor)
                .safari(safari)
                .expenseDate(dto.getExpenseDate())
                .dueDate(dto.getDueDate())
                .referenceNumber(trimToNull(dto.getReferenceNumber()))
                .taxPercentage(dto.getTaxPercentage())
                .status(ExpenseStatus.DRAFT)
                .internalNotes(trimToNull(dto.getInternalNotes()))
                .isActive(dto.getIsActive() == null || dto.getIsActive())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

            expense = expenseRepository.save(expense);
            expense.setExpenseCode(expense.generateCode());
            expense = expenseRepository.save(expense);

            log.info("Expense created: {} (vendor={}, safari={})",
                    expense.getExpenseCode(), vendor.getName(),
                    safari != null ? safari.getCode() : "<none>");

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Expense created successfully",
                        expenseGetService.toDTO(expense)));
        } catch (Exception e) {
            log.error("Error creating expense", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create expense: " + e.getMessage(),
                        "EXPENSE_CREATE_FAILED"));
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
