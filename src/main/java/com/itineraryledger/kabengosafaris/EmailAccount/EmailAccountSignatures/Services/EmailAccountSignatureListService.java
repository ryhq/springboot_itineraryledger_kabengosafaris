package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.EmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.ListStats;
import com.itineraryledger.kabengosafaris.Response.RecordNavigation;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Every signature, across every mailbox.
 *
 * The nested list under an account answers "what can this mailbox sign with". This is the
 * list of the signatures themselves — a thing somebody maintains and reviews, not only a
 * detail of the account it hangs off. Without it there is no way to ask "which of these has
 * been rewritten" or "what is switched off" without opening each account in turn.
 *
 * Content is deliberately absent from the rows: a signature body is an HTML document, and
 * twenty of them in a list response is a lot of bytes for a table showing a name and a flag.
 * The record fetch carries it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EmailAccountSignatureListService {

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "name", "enabled", "isDefault", "isSystemDefault", "fileSize", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_FIELD = "name";

    private final EmailAccountSignatureRepository signatureRepository;
    private final EmailAccountSignatureGetService getService;
    private final EmailAccountSignatureService signatureService;
    private final IdObfuscator idObfuscator;
    private final ListStats listStats;
    private final RecordNavigation recordNavigation;

    public ResponseEntity<ApiResponse<?>> list(
        EmailAccountSignatureFilter filter,
        Boolean includeStats,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
    ) {
        try {
            EmailAccountSignatureFilter resolved =
                filter != null ? filter : new EmailAccountSignatureFilter();

            String resolvedSort = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            int pageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Pageable pageable = PageRequest.of(
                page == null || page < 0 ? 0 : page, pageSize, Sort.by(direction, resolvedSort));

            Specification<EmailAccountSignature> spec = buildSpec(resolved);
            Page<EmailAccountSignature> found = signatureRepository.findAll(spec, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("signatures", found.getContent().stream().map(getService::convertToDTO).toList());
            response.put("currentPage", found.getNumber());
            response.put("totalItems", found.getTotalElements());
            response.put("totalPages", found.getTotalPages());
            response.put("pageSize", found.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", resolvedSort);
            response.put("currentSortDirection", direction.name().toLowerCase());
            if (!Boolean.FALSE.equals(includeStats)) {
                response.put("stats", buildStats(spec));
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Signatures retrieved", response));
        } catch (Exception e) {
            log.error("Error listing signatures", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list signatures", "SIGNATURES_LIST_FAILED"));
        }
    }

    /** One signature WITH its body, and where it sits in the set the caller came from. */
    public ResponseEntity<ApiResponse<?>> getOne(
        String idObfuscated,
        EmailAccountSignatureFilter filter,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            EmailAccountSignature signature = signatureRepository.findById(id).orElse(null);
            if (signature == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Signature not found", "SIGNATURE_NOT_FOUND"));
            }

            /*
             * The body lives on disk, not in the row. A read failure is reported rather than
             * quietly returning a signature with no content — an empty editor looks exactly
             * like a signature somebody emptied, and one of those is safe to save over.
             */
            String content = signatureService.readSignatureFile(signature.getFileName());
            if (content == null) {
                return ResponseEntity.status(500).body(
                    ApiResponse.error(500,
                        "The signature's file could not be read, so its content is unavailable",
                        "SIGNATURE_CONTENT_READ_FAILED"));
            }

            Specification<EmailAccountSignature> navSpec =
                buildSpec(filter != null ? filter : new EmailAccountSignatureFilter());
            String navSortBy = sortBy != null && VALID_SORT_FIELDS.contains(sortBy)
                ? sortBy : DEFAULT_SORT_FIELD;
            Map<String, Object> nav = recordNavigation.navigate(
                EmailAccountSignature.class, navSpec, navSortBy,
                !"desc".equalsIgnoreCase(sortDirection), id);

            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            EmailAccountSignatureDTO dto = getService.convertToDTO(signature);
            dto.setContent(content);

            Map<String, Object> response = new HashMap<>();
            response.put("signature", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Signature retrieved", response));
        } catch (Exception e) {
            log.error("Error fetching signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch signature", "SIGNATURE_FETCH_FAILED"));
        }
    }

    /** The account a signature belongs to, obfuscated — the write services are nested under it. */
    public String accountIdOf(String signatureIdObfuscated) {
        try {
            Long id = idObfuscator.decodeId(signatureIdObfuscated);
            EmailAccountSignature signature = signatureRepository.findById(id).orElse(null);
            if (signature == null || signature.getEmailAccount() == null) return null;
            return idObfuscator.encodeId(signature.getEmailAccount().getId());
        } catch (Exception e) {
            log.warn("Unreadable signature id: {}", signatureIdObfuscated);
            return null;
        }
    }

    /** ONE specification, shared by the rows, the counters and the record walk. */
    private Specification<EmailAccountSignature> buildSpec(EmailAccountSignatureFilter filter) {
        Specification<EmailAccountSignature> spec = Specification.unrestricted();

        String keyword = filter.effectiveKeyword();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like)));
        }
        if (filter.getName() != null && !filter.getName().isBlank()) {
            String like = "%" + filter.getName().toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (filter.getEmailAccountId() != null && !filter.getEmailAccountId().isBlank()) {
            try {
                Long accountId = idObfuscator.decodeId(filter.getEmailAccountId());
                spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("emailAccount").get("id"), accountId));
            } catch (Exception e) {
                log.warn("Unreadable emailAccountId filter: {}", filter.getEmailAccountId());
            }
        }
        Boolean enabled = filter.resolvedEnabled();
        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }
        if (filter.getIsDefault() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isDefault"), filter.getIsDefault()));
        }
        // shipped and custom together is everybody, which is no constraint at all
        boolean shipped = filter.wantsKind("shipped");
        boolean custom = filter.wantsKind("custom");
        if (shipped ^ custom) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isSystemDefault"), shipped));
        }
        return spec;
    }

    /**
     * The cards over the signatures.
     *
     * The one that matters is "no default": an account with signatures but none marked
     * default signs nothing, which looks identical to an account with no signatures at all
     * until somebody reads a sent message.
     */
    private Map<String, Object> buildStats(Specification<EmailAccountSignature> spec) {
        return listStats.of(EmailAccountSignature.class, spec)
            .total()
            .count("enabled", (root, query, cb) -> cb.isTrue(root.get("enabled")))
            .complement("disabled", "enabled")
            .count("inUse", (root, query, cb) -> cb.isTrue(root.get("isDefault")))
            .count("shipped", (root, query, cb) -> cb.isTrue(root.get("isSystemDefault")))
            .complement("custom", "shipped")
            .build();
    }
}
