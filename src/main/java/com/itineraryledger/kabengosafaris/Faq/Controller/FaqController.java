package com.itineraryledger.kabengosafaris.Faq.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Faq.DTOs.CreateFaqDTO;
import com.itineraryledger.kabengosafaris.Faq.DTOs.ReorderFaqsDTO;
import com.itineraryledger.kabengosafaris.Faq.DTOs.UpdateFaqDTO;
import com.itineraryledger.kabengosafaris.Faq.Services.FaqService;
import com.itineraryledger.kabengosafaris.Faq.Specifications.FaqFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** The site's global FAQ list, for the management panel. */
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
@Slf4j
public class FaqController {

    private final FaqService faqService;
    private final com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;
    private final com.itineraryledger.kabengosafaris.Faq.Repository.FaqRepository faqRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_FAQ')")
    public ResponseEntity<ApiResponse<?>> getAllFaqs(
        @ModelAttribute FaqFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "50") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        return faqService.getAllFaqs(filter, includeStats, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_FAQ')")
    public ResponseEntity<ApiResponse<?>> getFaqById(
        @PathVariable String id,
        @ModelAttribute FaqFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return faqService.getFaqById(id, filter, sortBy, sortDirection);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_FAQ')")
    public ResponseEntity<ApiResponse<?>> createFaq(@Valid @RequestBody CreateFaqDTO createDTO) {
        return faqService.createFaq(createDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FAQ')")
    public ResponseEntity<ApiResponse<?>> updateFaq(
        @PathVariable String id,
        @Valid @RequestBody UpdateFaqDTO updateDTO
    ) {
        return faqService.updateFaq(id, updateDTO);
    }

    /**
     * PATCH /bulk — show or hide a whole selection.
     *
     * Seasonal answers go on and off together (a park's fees, a closed route), and doing that
     * one record at a time is how a stale answer ends up staying on the site.
     */
    @org.springframework.web.bind.annotation.PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FAQ')")
    public ResponseEntity<?> bulkFlags(
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        return bulkFlags.apply("FAQ", faqRepository, request, faq -> {
            if (request.getIsActive() != null) faq.setIsActive(request.getIsActive());
        });
    }

    /** The running order, as a whole list — position IS the display order. */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FAQ')")
    public ResponseEntity<ApiResponse<?>> reorder(@Valid @RequestBody ReorderFaqsDTO dto) {
        return faqService.reorder(dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_FAQ')")
    public ResponseEntity<ApiResponse<?>> deleteFaqs(@RequestBody List<String> ids) {
        return faqService.deleteFaqs(ids);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_FAQ')")
    public ResponseEntity<ApiResponse<?>> deleteFaq(@PathVariable String id) {
        return faqService.deleteFaqs(List.of(id));
    }
}
