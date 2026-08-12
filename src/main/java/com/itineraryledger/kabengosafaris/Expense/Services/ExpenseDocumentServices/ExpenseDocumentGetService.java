package com.itineraryledger.kabengosafaris.Expense.Services.ExpenseDocumentServices;

import com.itineraryledger.kabengosafaris.Expense.DTOs.ExpenseDocumentDTOs.ExpenseDocumentDTO;
import com.itineraryledger.kabengosafaris.Expense.Entity.ExpenseDocument;
import com.itineraryledger.kabengosafaris.Expense.Repository.ExpenseDocumentRepository;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpenseDocumentFilter;
import com.itineraryledger.kabengosafaris.Expense.Specifications.ExpenseDocumentSpecification;
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
    private final com.itineraryledger.kabengosafaris.Response.ListStats listStats;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

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

    private static final List<String> VALID_SORT_FIELDS = List.of(
        "title", "documentType", "fileName", "fileSize", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    /**
     * Every expense document, filtered — the same page shape as every other
     * document module.
     *
     * It exists because the question "where is the slip for that transfer" is
     * asked without knowing which bill it hangs off, and a per-bill endpoint
     * cannot answer that.
     */
    public ResponseEntity<ApiResponse<?>> getAllDocuments(
            ExpenseDocumentFilter filter,
            Boolean includeStats,
            String sortBy,
            String sortDirection,
            int page,
            int size
    ) {
        try {
            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            org.springframework.data.domain.Sort.Direction direction =
                "asc".equalsIgnoreCase(sortDirection)
                    ? org.springframework.data.domain.Sort.Direction.ASC
                    : org.springframework.data.domain.Sort.Direction.DESC;
            // clamp: an unbounded size is a way to ask for the whole table by accident
            int pageSize = size <= 0 ? 20 : Math.min(size, 100);

            var pageable = org.springframework.data.domain.PageRequest.of(
                Math.max(page, 0), pageSize,
                org.springframework.data.domain.Sort.by(direction, resolvedSort));

            var spec = buildSpec(filter != null ? filter : new ExpenseDocumentFilter());
            var found = repository.findAll(spec, pageable);

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("documents", found.getContent().stream().map(this::toDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalElements", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            /*
             * Counters for the WHOLE filtered set, from the same specification as
             * the rows — without them the page can only summarise what it loaded,
             * and the "All filtered / This page" toggle has to stay hidden.
             */
            if (includeStats == null || includeStats) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Documents retrieved", response));
        } catch (Exception e) {
            log.error("Error listing expense documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch documents", "EXPENSE_DOCUMENTS_FETCH_FAILED"));
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private org.springframework.data.jpa.domain.Specification<ExpenseDocument> buildSpec(
            ExpenseDocumentFilter filter) {
        var spec = org.springframework.data.jpa.domain.Specification.<ExpenseDocument>unrestricted()
            .and(ExpenseDocumentSpecification.byExpenseId(decode(filter.getExpenseId())))
            .and(ExpenseDocumentSpecification.byVendorId(decode(filter.getVendorId())))
            .and(ExpenseDocumentSpecification.bySafariId(decode(filter.getSafariId())))
            .and(ExpenseDocumentSpecification.byDocumentType(filter.getDocumentType()))
            .and(ExpenseDocumentSpecification.byDocumentTypes(filter.getDocumentTypes()))
            .and(ExpenseDocumentSpecification.byTitleContains(filter.getTitle()))
            .and(ExpenseDocumentSpecification.byVersion(filter.getVersion()))
            .and(ExpenseDocumentSpecification.byIsProofOfPayment(filter.getIsProofOfPayment()))
            .and(ExpenseDocumentSpecification.createdAfter(filter.getCreatedAfter()))
            .and(ExpenseDocumentSpecification.searchKeyword(filter.getKeyword()));

        if (filter.getIsActive() != null) {
            spec = spec.and(ExpenseDocumentSpecification.byIsActive(filter.getIsActive()));
        }
        if (filter.getStatuses() != null && !filter.getStatuses().isEmpty()) {
            var states = filter.getStatuses().stream()
                .map(state -> "active".equalsIgnoreCase(state) ? Boolean.TRUE
                    : "inactive".equalsIgnoreCase(state) ? Boolean.FALSE : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
            // active AND inactive is every document: a contradiction cancels
            if (states.size() == 1) {
                spec = spec.and(ExpenseDocumentSpecification.byIsActive(states.get(0)));
            }
        }
        if (Boolean.TRUE.equals(filter.getCurrentlyValid())) {
            spec = spec.and(ExpenseDocumentSpecification.byCurrentlyValid(
                java.time.LocalDateTime.now()));
        }
        if (filter.getValidity() != null && !filter.getValidity().isEmpty()) {
            var any = new java.util.ArrayList<
                org.springframework.data.jpa.domain.Specification<ExpenseDocument>>();
            if (filter.getValidity().contains("expired")) any.add(ExpenseDocumentSpecification.expired());
            if (filter.getValidity().contains("expiring")) any.add(ExpenseDocumentSpecification.expiringWithin(30));
            if (filter.getValidity().contains("no-expiry")) any.add(ExpenseDocumentSpecification.noExpiry());
            if (!any.isEmpty()) {
                var combined = any.get(0);
                for (int i = 1; i < any.size(); i++) combined = combined.or(any.get(i));
                spec = spec.and(combined);
            }
        }
        return spec;
    }

    private java.util.Map<String, Object> buildStats(
            org.springframework.data.jpa.domain.Specification<ExpenseDocument> base) {
        return listStats.of(ExpenseDocument.class, base)
            .total()
            .count("active", ExpenseDocumentSpecification.byIsActive(true))
            .complement("inactive", "active")
            .count("proofOfPayment", ExpenseDocumentSpecification.byIsProofOfPayment(true))
            .count("expired", ExpenseDocumentSpecification.expired())
            .count("expiringSoon", ExpenseDocumentSpecification.expiringWithin(30))
            .count("noExpiry", ExpenseDocumentSpecification.noExpiry())
            .recency(ExpenseDocumentSpecification::createdAfter)
            .build();
    }

    private Long decode(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            return null;
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String idObfuscated) {
        return getById(idObfuscated, null);
    }

    /**
     * One document, and where it sits in the set the caller came from.
     *
     * @param scopeParentId the bill, when opened from inside one — paging then
     *                      stays among that bill's documents
     */
    public ResponseEntity<ApiResponse<?>> getById(String idObfuscated, String scopeParentId) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            ExpenseDocument doc = repository.findById(id).orElse(null);
            if (doc == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Document not found", "EXPENSE_DOCUMENT_NOT_FOUND"));
            }

            Long scopedParentId = decode(scopeParentId);
            var navSpec = scopedParentId != null
                ? ExpenseDocumentSpecification.byExpenseId(scopedParentId)
                : org.springframework.data.jpa.domain.Specification.<ExpenseDocument>unrestricted();

            var nav = recordNavigation.navigate(
                ExpenseDocument.class, navSpec, DEFAULT_SORT_FIELD, false, id);
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("document", toDTO(doc));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));
            response.put("scopeParentId", scopeParentId);

            return ResponseEntity.ok(ApiResponse.success(200, "Document retrieved", response));
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
                .expenseTitle(d.getExpense() != null ? d.getExpense().getTitle() : null)
                .vendorId(d.getExpense() != null && d.getExpense().getVendor() != null
                        ? idObfuscator.encodeId(d.getExpense().getVendor().getId()) : null)
                .vendorName(d.getExpense() != null && d.getExpense().getVendor() != null
                        ? d.getExpense().getVendor().getName() : null)
                .safariId(d.getExpense() != null && d.getExpense().getSafari() != null
                        ? idObfuscator.encodeId(d.getExpense().getSafari().getId()) : null)
                .safariName(d.getExpense() != null && d.getExpense().getSafari() != null
                        ? d.getExpense().getSafari().getName() : null)
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
