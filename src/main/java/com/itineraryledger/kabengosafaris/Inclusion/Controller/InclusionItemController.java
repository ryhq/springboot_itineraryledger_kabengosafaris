package com.itineraryledger.kabengosafaris.Inclusion.Controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.Inclusion.DTOs.CreateInclusionItemDTO;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.ReorderInclusionItemsDTO;
import com.itineraryledger.kabengosafaris.Inclusion.DTOs.UpdateInclusionItemDTO;
import com.itineraryledger.kabengosafaris.Inclusion.Repository.InclusionItemRepository;
import com.itineraryledger.kabengosafaris.Inclusion.Services.InclusionItemService;
import com.itineraryledger.kabengosafaris.Inclusion.Specifications.InclusionItemFilter;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Response.BulkFlags;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** The catalogue of lines a trip's promise is assembled from, for the management panel. */
@RestController
@RequestMapping("/api/inclusion-items")
@RequiredArgsConstructor
@Slf4j
public class InclusionItemController {

    private final InclusionItemService service;
    private final BulkFlags bulkFlags;
    private final InclusionItemRepository repository;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_INCLUSION_ITEM')")
    public ResponseEntity<ApiResponse<?>> getAll(
        @ModelAttribute InclusionItemFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "50") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "asc") String sortDirection
    ) {
        return service.getAll(filter, includeStats, page, size, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_INCLUSION_ITEM')")
    public ResponseEntity<ApiResponse<?>> getById(
        @PathVariable String id,
        @ModelAttribute InclusionItemFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        return service.getById(id, filter, sortBy, sortDirection);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_INCLUSION_ITEM')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateInclusionItemDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INCLUSION_ITEM')")
    public ResponseEntity<ApiResponse<?>> update(
        @PathVariable String id,
        @Valid @RequestBody UpdateInclusionItemDTO dto
    ) {
        return service.update(id, dto);
    }

    /**
     * PATCH /bulk — enable or disable a whole selection.
     *
     * <p>Lines go on and off together when a policy changes (a park stops waiving a fee, an insurer
     * changes cover), and doing that one row at a time is how half a change ships.
     */
    @PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INCLUSION_ITEM')")
    public ResponseEntity<?> bulk(@RequestBody BulkFlags.Request request) {
        return bulkFlags.apply("INCLUSION_ITEM", repository, request, item -> {
            if (request.getIsActive() != null) item.setIsActive(request.getIsActive());
        });
    }

    /** The running order, as a whole list — position IS the display order. */
    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_INCLUSION_ITEM')")
    public ResponseEntity<ApiResponse<?>> reorder(@Valid @RequestBody ReorderInclusionItemsDTO dto) {
        return service.reorder(dto);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_INCLUSION_ITEM')")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody List<String> ids) {
        return service.delete(ids);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_INCLUSION_ITEM')")
    public ResponseEntity<ApiResponse<?>> deleteOne(@PathVariable String id) {
        return service.delete(List.of(id));
    }
}
