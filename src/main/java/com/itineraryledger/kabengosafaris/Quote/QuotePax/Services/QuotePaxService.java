package com.itineraryledger.kabengosafaris.Quote.QuotePax.Services;

import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.DTOs.QuotePaxDTO;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.DTOs.UpsertQuotePaxDTO;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Entity.QuotePax;
import com.itineraryledger.kabengosafaris.Quote.QuotePax.Repository.QuotePaxRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCostEstimationService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QuotePaxService — combined Get/Upsert/Delete for QuotePax rows on a Quote.
 * Mirrors SafariPaxGetService + SafariPaxUpsertService + SafariPaxDeleteService.
 * Uniqueness on (quote, nation, age) is enforced at the DB level; this
 * service reconciles incoming bulk upserts against the existing rows.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuotePaxService {

    private final QuoteRepository quoteRepository;
    private final QuotePaxRepository quotePaxRepository;
    private final PaxNationCategoryRepository paxNationCategoryRepository;
    private final PaxAgeCategoryRepository paxAgeCategoryRepository;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getQuotePax(String quoteIdObfuscated) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            if (quoteId == null || !quoteRepository.existsById(quoteId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            List<QuotePax> paxList = quotePaxRepository.findByQuoteIdOrderByIdAsc(quoteId);
            List<QuotePaxDTO> dtos = paxList.stream().map(this::toDTO).collect(Collectors.toList());
            int total = paxList.stream().mapToInt(p -> p.getCount() != null ? p.getCount() : 0).sum();

            Map<String, Object> body = new HashMap<>();
            body.put("paxEntries", dtos);
            body.put("totalPax", total);
            return ResponseEntity.ok(ApiResponse.success(200,
                    "Retrieved " + dtos.size() + " pax categories (total: " + total + " passengers)", body));
        } catch (Exception e) {
            log.error("Error fetching quote pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch quote pax", "QUOTE_PAX_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getQuotePaxById(String quoteIdObf, String paxIdObf) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObf);
            Long paxId = idObfuscator.decodeId(paxIdObf);
            if (quoteId == null || paxId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            if (!quoteRepository.existsById(quoteId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            QuotePax pax = quotePaxRepository.findById(paxId).orElse(null);
            if (pax == null || !pax.getQuote().getId().equals(quoteId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Pax entry not found", "PAX_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Pax entry retrieved successfully", toDTO(pax)));
        } catch (Exception e) {
            log.error("Error fetching quote pax by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch quote pax", "QUOTE_PAX_FETCH_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> upsertQuotePax(String quoteIdObfuscated, List<UpsertQuotePaxDTO> dtos) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            if (quoteId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid quote ID", "INVALID_QUOTE_ID"));
            }
            Quote quote = quoteRepository.findById(quoteId).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            if (dtos == null || dtos.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "At least one pax entry is required", "EMPTY_PAYLOAD"));
            }

            int created = 0, updated = 0;
            List<QuotePaxDTO> resultDTOs = new ArrayList<>();

            for (UpsertQuotePaxDTO dto : dtos) {
                Long nationId = idObfuscator.decodeId(dto.getNationCategoryId());
                Long ageId = idObfuscator.decodeId(dto.getAgeCategoryId());
                if (nationId == null || ageId == null) {
                    return ResponseEntity.badRequest().body(
                            ApiResponse.error(400, "Invalid pax category ID", "INVALID_CATEGORY_ID"));
                }
                PaxNationCategory nation = paxNationCategoryRepository.findById(nationId).orElse(null);
                PaxAgeCategory age = paxAgeCategoryRepository.findById(ageId).orElse(null);
                if (nation == null || age == null) {
                    return ResponseEntity.status(404).body(
                            ApiResponse.error(404, "Pax category not found", "CATEGORY_NOT_FOUND"));
                }

                QuotePax existing = quotePaxRepository
                        .findByQuoteIdAndNationCategoryIdAndAgeCategoryId(quoteId, nationId, ageId)
                        .orElse(null);

                if (existing != null) {
                    existing.setCount(dto.getCount());
                    existing.setNotes(dto.getNotes());
                    QuotePax saved = quotePaxRepository.save(existing);
                    resultDTOs.add(toDTO(saved));
                    updated++;
                } else {
                    QuotePax fresh = QuotePax.builder()
                            .quote(quote)
                            .nationCategory(nation)
                            .ageCategory(age)
                            .count(dto.getCount())
                            .notes(dto.getNotes())
                            .build();
                    QuotePax saved = quotePaxRepository.save(fresh);
                    resultDTOs.add(toDTO(saved));
                    created++;
                }
            }

            quoteCostEstimationService.triggerRecalc(quoteId);

            Map<String, Object> body = new HashMap<>();
            body.put("paxEntries", resultDTOs);
            body.put("created", created);
            body.put("updated", updated);
            return ResponseEntity.ok(ApiResponse.success(200,
                    "Upserted " + (created + updated) + " pax entries (" + created + " new, " + updated + " updated)",
                    body));
        } catch (Exception e) {
            log.error("Error upserting quote pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to upsert quote pax", "QUOTE_PAX_UPSERT_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteQuotePax(String quoteIdObfuscated, List<String> paxIdsObfuscated) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObfuscated);
            if (quoteId == null || !quoteRepository.existsById(quoteId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            if (paxIdsObfuscated == null || paxIdsObfuscated.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "At least one pax ID is required", "EMPTY_PAYLOAD"));
            }
            int deleted = 0;
            for (String obf : paxIdsObfuscated) {
                Long paxId = idObfuscator.decodeId(obf);
                if (paxId == null) continue;
                QuotePax pax = quotePaxRepository.findById(paxId).orElse(null);
                if (pax != null && pax.getQuote().getId().equals(quoteId)) {
                    quotePaxRepository.delete(pax);
                    deleted++;
                }
            }
            if (deleted > 0) quoteCostEstimationService.triggerRecalc(quoteId);
            return ResponseEntity.ok(ApiResponse.success(200, "Deleted " + deleted + " pax entries", deleted));
        } catch (Exception e) {
            log.error("Error deleting quote pax", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete quote pax", "QUOTE_PAX_DELETE_FAILED"));
        }
    }

    private QuotePaxDTO toDTO(QuotePax pax) {
        QuotePaxDTO dto = new QuotePaxDTO();
        dto.setId(idObfuscator.encodeId(pax.getId()));
        dto.setQuoteId(idObfuscator.encodeId(pax.getQuote().getId()));
        if (pax.getNationCategory() != null) {
            dto.setNationCategoryId(idObfuscator.encodeId(pax.getNationCategory().getId()));
            dto.setNationCategoryName(pax.getNationCategory().getName());
        }
        if (pax.getAgeCategory() != null) {
            dto.setAgeCategoryId(idObfuscator.encodeId(pax.getAgeCategory().getId()));
            dto.setAgeCategoryName(pax.getAgeCategory().getName());
        }
        dto.setCount(pax.getCount());
        dto.setNotes(pax.getNotes());
        dto.setCreatedAt(pax.getCreatedAt());
        dto.setUpdatedAt(pax.getUpdatedAt());
        return dto;
    }
}
