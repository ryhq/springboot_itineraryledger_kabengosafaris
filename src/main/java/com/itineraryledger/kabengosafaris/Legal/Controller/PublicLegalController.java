package com.itineraryledger.kabengosafaris.Legal.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocument;
import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocumentType;
import com.itineraryledger.kabengosafaris.Legal.Services.LegalDocumentService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * The published notice, for anybody.
 *
 * <p>Both public websites read this instead of carrying their own copy. They are two copies of
 * one codebase, so a hand-written privacy page there is two edits and two deploys every time a
 * sentence changes, and the two drift. More to the point, their current page states that we
 * collect nothing automatically and set no cookies, which a client portal with a session makes
 * untrue the day it ships.
 *
 * <p>Served as rendered HTML as well as source, so a site can drop it into a page without
 * implementing the markup, and so there is exactly one implementation of the escaping.
 */
@RestController
@RequestMapping("/api/public/legal")
@RequiredArgsConstructor
public class PublicLegalController {

    private final LegalDocumentService service;

    @GetMapping("/{type}")
    public ResponseEntity<ApiResponse<?>> published(@PathVariable String type) {
        LegalDocumentType documentType = LegalDocumentType.bySlug(type);
        if (documentType == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404,
                "No such notice", "LEGAL_DOCUMENT_NOT_FOUND"));
        }

        LegalDocument document = service.published(documentType).orElse(null);
        if (document == null) {
            /*
             * 404 rather than an empty page. A website that renders "" under a Privacy Policy
             * heading looks like a company that has not got one, which is worse than a page that
             * is honestly missing and shows up in a check.
             */
            return ResponseEntity.status(404).body(ApiResponse.error(404,
                "No " + documentType.getDisplayName().toLowerCase() + " has been published yet",
                "LEGAL_DOCUMENT_NOT_PUBLISHED"));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", documentType.name());
        payload.put("slug", documentType.getSlug());
        payload.put("title", document.getTitle());
        payload.put("version", document.getVersion());
        payload.put("effectiveFrom", document.getEffectiveFrom());
        payload.put("html", service.renderHtml(document.getBody()));
        return ResponseEntity.ok(ApiResponse.success(200, "Notice retrieved", payload));
    }
}
