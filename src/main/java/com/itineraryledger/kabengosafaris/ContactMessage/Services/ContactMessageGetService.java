package com.itineraryledger.kabengosafaris.ContactMessage.Services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.ContactMessageDTO;
import com.itineraryledger.kabengosafaris.ContactMessage.DTOs.ContactMessageListItemDTO;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessage;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;
import com.itineraryledger.kabengosafaris.ContactMessage.Repository.ContactMessageRepository;
import com.itineraryledger.kabengosafaris.ContactMessage.Specifications.ContactMessageSpecification;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ContactMessageGetService {

    private final ContactMessageRepository repository;
    private final IdObfuscator idObfuscator;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList(
        "code", "name", "email", "subject", "status", "createdAt", "updatedAt"
    );
    private static final String DEFAULT_SORT_FIELD = "createdAt";

    @Autowired
    public ContactMessageGetService(ContactMessageRepository repository, IdObfuscator idObfuscator) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
    }

    public ResponseEntity<ApiResponse<?>> getMessageById(String idObfuscated) {
        log.info("Fetching contact message with ID: {}", idObfuscated);
        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode message ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid message ID", "INVALID_MESSAGE_ID")
                );
            }

            ContactMessage message = repository.findById(id).orElse(null);
            if (message == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Contact message not found", "MESSAGE_NOT_FOUND")
                );
            }

            ContactMessageDTO dto = convertToDTO(message);

            Long nextId = repository.findNextId(id).orElse(null);
            Long previousId = repository.findPreviousId(id).orElse(null);
            if (nextId == null) nextId = repository.findFirstId().orElse(null);
            if (previousId == null) previousId = repository.findLastId().orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("message", dto);
            response.put("nextId", nextId != null ? idObfuscator.encodeId(nextId) : null);
            response.put("previousId", previousId != null ? idObfuscator.encodeId(previousId) : null);

            return ResponseEntity.ok().body(ApiResponse.success(200, "Contact message retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error fetching contact message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch contact message", "MESSAGE_FETCH_FAILED")
            );
        }
    }

    public ResponseEntity<ApiResponse<?>> listMessages(
        Integer pageNumber, Integer pageSize, String sortBy, String sortDirection,
        ContactMessageStatus status, String email, String subject,
        LocalDateTime createdAfter, LocalDateTime createdBefore, String keyword
    ) {
        log.info("Listing contact messages with filters");
        try {
            pageNumber = (pageNumber != null) ? pageNumber : 0;
            pageSize = (pageSize != null) ? pageSize : 20;
            sortDirection = (sortDirection != null && !sortDirection.isEmpty()) ? sortDirection : "desc";

            String validatedSortBy = validateSortField(sortBy);
            if (validatedSortBy == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid sort field: " + sortBy + ". Valid fields are: " + VALID_SORT_FIELDS, "INVALID_SORT_FIELD")
                );
            }

            Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(validatedSortBy).descending()
                : Sort.by(validatedSortBy).ascending();
            Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

            Specification<ContactMessage> spec = Specification.unrestricted();
            if (status != null) spec = spec.and(ContactMessageSpecification.byStatus(status));
            if (email != null && !email.isEmpty()) spec = spec.and(ContactMessageSpecification.byEmail(email));
            if (subject != null && !subject.isEmpty()) spec = spec.and(ContactMessageSpecification.bySubject(subject));
            if (createdAfter != null) spec = spec.and(ContactMessageSpecification.createdAfter(createdAfter));
            if (createdBefore != null) spec = spec.and(ContactMessageSpecification.createdBefore(createdBefore));
            if (keyword != null && !keyword.isEmpty()) spec = spec.and(ContactMessageSpecification.searchKeyword(keyword));

            Page<ContactMessage> page = repository.findAll(spec, pageable);

            List<ContactMessageListItemDTO> dtos = page.getContent().stream()
                .map(this::convertToListItemDTO)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("messages", dtos);
            response.put("currentPage", page.getNumber());
            response.put("totalItems", page.getTotalElements());
            response.put("totalPages", page.getTotalPages());
            response.put("pageSize", page.getSize());
            response.put("validSortFields", VALID_SORT_FIELDS);
            response.put("currentSortBy", validatedSortBy);
            response.put("currentSortDirection", sortDirection);

            return ResponseEntity.ok().body(ApiResponse.success(200, "Contact messages retrieved successfully", response));
        } catch (Exception e) {
            log.error("Error listing contact messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list contact messages", "MESSAGES_LIST_FAILED")
            );
        }
    }

    private String validateSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return DEFAULT_SORT_FIELD;
        for (String field : VALID_SORT_FIELDS) {
            if (field.equalsIgnoreCase(sortBy)) return field;
        }
        return null;
    }

    public ContactMessageDTO convertToDTO(ContactMessage message) {
        ContactMessageDTO dto = ContactMessageDTO.builder()
            .id(idObfuscator.encodeId(message.getId()))
            .code(message.getCode())
            .name(message.getName())
            .email(message.getEmail())
            .phone(message.getPhone())
            .subject(message.getSubject())
            .message(message.getMessage())
            .status(message.getStatus())
            .statusDisplayName(message.getStatus() != null ? message.getStatus().getDisplayName() : null)
            .source(message.getSource())
            .preferredLocale(message.getPreferredLocale())
            .adminNotes(message.getAdminNotes())
            .createdAt(message.getCreatedAt())
            .updatedAt(message.getUpdatedAt())
            .respondedAt(message.getRespondedAt())
            .build();

        if (message.getCustomer() != null) {
            dto.setCustomerId(idObfuscator.encodeId(message.getCustomer().getId()));
            dto.setCustomerName(message.getCustomer().getFullName());
        }

        return dto;
    }

    private ContactMessageListItemDTO convertToListItemDTO(ContactMessage message) {
        return ContactMessageListItemDTO.builder()
            .id(idObfuscator.encodeId(message.getId()))
            .code(message.getCode())
            .name(message.getName())
            .email(message.getEmail())
            .subject(message.getSubject())
            .status(message.getStatus())
            .statusDisplayName(message.getStatus() != null ? message.getStatus().getDisplayName() : null)
            .createdAt(message.getCreatedAt())
            .build();
    }
}
