package com.itineraryledger.kabengosafaris.Legal.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocument;
import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocumentType;
import com.itineraryledger.kabengosafaris.Legal.DTOs.LegalDocumentDTO;
import com.itineraryledger.kabengosafaris.Legal.DTOs.UpsertLegalDocumentDTO;
import com.itineraryledger.kabengosafaris.Legal.Repository.LegalDocumentRepository;
import com.itineraryledger.kabengosafaris.Legal.Services.LegalDocumentService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writing and publishing the privacy notice and the terms.
 *
 * <p>Company-level content, so it reuses the company profile's permissions rather than minting a
 * pair of names for two endpoints. A permission is matched by name in this application and a name
 * nothing checks is dead weight in a list of five hundred.
 */
@RestController
@RequestMapping("/api/legal-documents")
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentController {

    private final LegalDocumentRepository repository;
    private final LegalDocumentService service;
    private final IdObfuscator idObfuscator;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> list(
        @RequestParam(required = false) String documentType
    ) {
        LegalDocumentType type = LegalDocumentType.bySlug(documentType);
        List<LegalDocument> rows = type == null
            ? repository.findAll()
            : service.versionsOf(type);
        return ResponseEntity.ok(ApiResponse.success(200, "Legal documents retrieved",
            rows.stream().map(this::toDTO).toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> one(@PathVariable String id) {
        LegalDocument document = repository.findById(idObfuscator.decodeId(id)).orElse(null);
        if (document == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "No such legal document", "LEGAL_DOCUMENT_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Legal document retrieved", toDTO(document)));
    }

    /**
     * A new version. Versions are never edited in place once published.
     *
     * <p>Somebody consented to a particular text, and the record of that is the version string on
     * their participant row. Editing the words behind an already-published version makes that
     * record a lie, so a change is a new version and publishing it retires the old one.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody UpsertLegalDocumentDTO request) {
        LegalDocumentType type = LegalDocumentType.bySlug(request.getDocumentType());
        if (type == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "documentType must be PRIVACY_POLICY or TERMS", "INVALID_DOCUMENT_TYPE"));
        }

        String version = request.getVersion() == null || request.getVersion().isBlank()
            ? LocalDate.now().toString()
            : request.getVersion().trim();

        if (repository.findByDocumentTypeAndVersion(type, version).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "Version " + version + " of this notice already exists. Use a new version string: "
                    + "the one that is already published is what somebody consented to.",
                "VERSION_EXISTS"));
        }

        LegalDocument saved = repository.save(LegalDocument.builder()
            .documentType(type)
            .version(version)
            .title(request.getTitle() == null || request.getTitle().isBlank()
                ? type.getDisplayName() : request.getTitle().trim())
            .body(request.getBody())
            .effectiveFrom(request.getEffectiveFrom() == null ? LocalDate.now() : request.getEffectiveFrom())
            .published(false)
            .build());

        if (Boolean.TRUE.equals(request.getPublished())) {
            saved = service.publish(saved.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Legal document saved", toDTO(saved)));
    }

    /** Correcting a draft. A published version's words are fixed; its publication is not. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id,
                                                 @Valid @RequestBody UpsertLegalDocumentDTO request) {
        LegalDocument document = repository.findById(idObfuscator.decodeId(id)).orElse(null);
        if (document == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "No such legal document", "LEGAL_DOCUMENT_NOT_FOUND"));
        }
        if (Boolean.TRUE.equals(document.getPublished())
                && request.getBody() != null
                && !request.getBody().equals(document.getBody())) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "This version is published, and clients have agreed to these words. Create a new "
                    + "version instead; publishing it will retire this one.",
                "PUBLISHED_VERSION_IS_FIXED"));
        }

        if (request.getTitle() != null) document.setTitle(request.getTitle().trim());
        if (request.getBody() != null) document.setBody(request.getBody());
        if (request.getEffectiveFrom() != null) document.setEffectiveFrom(request.getEffectiveFrom());
        LegalDocument saved = repository.save(document);

        if (Boolean.TRUE.equals(request.getPublished()) && !Boolean.TRUE.equals(saved.getPublished())) {
            saved = service.publish(saved.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(200, "Legal document updated", toDTO(saved)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> publish(@PathVariable String id) {
        LegalDocument saved = service.publish(idObfuscator.decodeId(id));
        if (saved == null) {
            return ResponseEntity.status(404).body(
                ApiResponse.error(404, "No such legal document", "LEGAL_DOCUMENT_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(200,
            "Published. Both websites and the client portal now show this version.", toDTO(saved)));
    }

    private LegalDocumentDTO toDTO(LegalDocument document) {
        return LegalDocumentDTO.builder()
            .id(idObfuscator.encodeId(document.getId()))
            .documentType(document.getDocumentType().name())
            .documentTypeDisplayName(document.getDocumentType().getDisplayName())
            .version(document.getVersion())
            .title(document.getTitle())
            .body(document.getBody())
            .html(service.renderHtml(document.getBody()))
            .effectiveFrom(document.getEffectiveFrom())
            .published(document.getPublished())
            .createdAt(document.getCreatedAt())
            .updatedAt(document.getUpdatedAt())
            .build();
    }
}
