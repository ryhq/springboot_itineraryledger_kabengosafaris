package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailContactRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.EmailContactDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailContactGetService {

    private final EmailContactRepository emailContactRepository;
    private final IdObfuscator idObfuscator;
    private final com.itineraryledger.kabengosafaris.Response.RecordNavigation recordNavigation;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "emailAddress", "displayName", "frequency", "lastContactedAt", "source", "isStarred", "createdAt"
    );
    private static final String DEFAULT_SORT_FIELD = "frequency";

    /**
     * Autocomplete search — used by compose TO/CC/BCC fields.
     * Lightweight: no pagination metadata, just a flat list sorted by frequency.
     */
    public ResponseEntity<ApiResponse<?>> searchContacts(String accountIdObfuscated, String search, int limit) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);

            List<EmailContact> contacts;
            if (search != null && !search.isBlank()) {
                contacts = emailContactRepository.searchContacts(accountId, search.trim(), PageRequest.of(0, limit));
            } else {
                contacts = emailContactRepository
                    .findByEmailAccountId(accountId, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "frequency")))
                    .getContent();
            }

            List<EmailContactDTO> dtos = contacts.stream().map(this::toDTO).toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Contacts retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error searching contacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to search contacts", "SEARCH_CONTACTS_FAILED"));
        }
    }

    /**
     * Paginated list with Specification-based filtering and sorting.
     *
     * Filters: isStarred, source, search (email or display name)
     */
    public ResponseEntity<ApiResponse<?>> getContacts(String accountIdObfuscated, int page, int size,
                                                       String sortBy, String sortDirection,
                                                       Boolean isStarred, String source, String search) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD"));
            }

            Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, validatedSortBy));

            // Build specification
            Specification<EmailContact> spec = buildSpec(accountIdObfuscated, isStarred, source, search);

            Page<EmailContact> pagedContacts = emailContactRepository.findAll(spec, pageRequest);
            List<EmailContactDTO> dtos = pagedContacts.getContent().stream().map(this::toDTO).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("contacts", dtos);
            response.put("currentPage", pagedContacts.getNumber());
            response.put("totalItems", pagedContacts.getTotalElements());
            response.put("totalPages", pagedContacts.getTotalPages());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection != null ? sortDirection : "desc");

            return ResponseEntity.ok(ApiResponse.success(200, "Contacts retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error getting contacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get contacts", "GET_CONTACTS_FAILED"));
        }
    }

    /**
     * Get single contact with circular navigation
     */
    public ResponseEntity<ApiResponse<?>> getContact(
        String accountIdObfuscated,
        String contactIdObfuscated,
        Boolean isStarred,
        String source,
        String search,
        String sortBy,
        String sortDirection
    ) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long contactId = idObfuscator.decodeId(contactIdObfuscated);

            EmailContact contact = emailContactRepository.findById(contactId).orElse(null);
            if (contact == null || !contact.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Contact not found", "CONTACT_NOT_FOUND"));
            }

            /*
             * Circular navigation over the caller's filtered, sorted set — scoped to the
             * parent when one is given. The id-ordered walk this replaces stepped through a
             * different set from the one on screen and could not say where you were in it.
             */
            String validatedSortBy = validateSortField(sortBy);
            java.util.Map<String, Object> nav = recordNavigation.navigate(
                EmailContact.class,
                buildSpec(accountIdObfuscated, isStarred, source, search),
                validatedSortBy != null ? validatedSortBy : "frequency",
                "asc".equalsIgnoreCase(sortDirection),
                contactId
            );
            Long nextId = (Long) nav.get("nextRawId");
            Long previousId = (Long) nav.get("previousRawId");

            Map<String, Object> response = new HashMap<>();
            response.put("contact", toDTO(contact));
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);
            response.put("position", nav.get("position"));
            response.put("total", nav.get("total"));

            return ResponseEntity.ok(ApiResponse.success(200, "Contact retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error getting contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get contact", "GET_CONTACT_FAILED"));
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public EmailContactDTO toDTO(EmailContact contact) {
        return EmailContactDTO.builder()
            .id(idObfuscator.encodeId(contact.getId()))
            .emailAddress(contact.getEmailAddress())
            .displayName(contact.getDisplayName())
            .frequency(contact.getFrequency())
            .lastContactedAt(contact.getLastContactedAt())
            .source(contact.getSource().name())
            .isStarred(contact.getIsStarred())
            .createdAt(contact.getCreatedAt())
            .build();
    }

    /**
     * The ONE description of the filtered set, shared by the rows and by the record
     * arrows — paging that walked a different set from the one on screen would be
     * worse than no arrows (see CLAUDE.md).
     */
    private Specification<EmailContact> buildSpec(
        String accountIdObfuscated,
        Boolean isStarred,
        String source,
        String search
    ) {
        Long accountId = idObfuscator.decodeId(accountIdObfuscated);

        Specification<EmailContact> spec = Specification.<EmailContact>unrestricted().and(EmailContactSpecification.forAccount(accountId));

            Specification<EmailContact> starredSpec = EmailContactSpecification.isStarred(isStarred);
            if (starredSpec != null) spec = spec.and(starredSpec);

            Specification<EmailContact> sourceSpec = EmailContactSpecification.hasSource(source);
            if (sourceSpec != null) spec = spec.and(sourceSpec);

            Specification<EmailContact> searchSpec = EmailContactSpecification.searchTerm(search);
            if (searchSpec != null) spec = spec.and(searchSpec);

        return spec;
    }
}
