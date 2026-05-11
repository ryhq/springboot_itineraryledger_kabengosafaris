package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Services;

import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.UpdateQuoteDayDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository.QuoteDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Repository.QuoteDayActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Repository.QuoteDayParkRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Repository.QuoteDayRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QuoteDayService — Get/Update/Delete for QuoteDay rows owned by a Quote.
 * Days are created by the Itinerary→Quote deep-copy, not manually; so this
 * service does not expose a create endpoint.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteDayService {

    private final QuoteRepository quoteRepository;
    private final QuoteDayRepository quoteDayRepository;
    private final QuoteDayAccommodationRepository accRepository;
    private final QuoteDayActivityRepository actRepository;
    private final QuoteDayParkRepository parkRepository;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> getDays(String quoteIdObf) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObf);
            if (quoteId == null || !quoteRepository.existsById(quoteId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            List<QuoteDay> days = quoteDayRepository.findByQuoteIdOrderByDayNumberAsc(quoteId);
            List<QuoteDayDTO> dtos = days.stream().map(this::toDTO).collect(Collectors.toList());

            Map<String, Object> body = new HashMap<>();
            body.put("days", dtos);
            body.put("totalDays", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Retrieved " + dtos.size() + " days", body));
        } catch (Exception e) {
            log.error("Error fetching quote days", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch quote days", "QUOTE_DAY_FETCH_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getDayById(String dayIdObf) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid day ID", "INVALID_DAY_ID"));
            }
            QuoteDay day = quoteDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Day not found", "DAY_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Day retrieved successfully", toDTO(day)));
        } catch (Exception e) {
            log.error("Error fetching quote day by id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch quote day", "QUOTE_DAY_FETCH_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateDay(String dayIdObf, UpdateQuoteDayDTO dto) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid day ID", "INVALID_DAY_ID"));
            }
            QuoteDay day = quoteDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Day not found", "DAY_NOT_FOUND"));
            }

            if (dto.getTitle() != null) day.setTitle(dto.getTitle());
            if (dto.getDescription() != null) day.setDescription(dto.getDescription());
            if (dto.getMorningActivities() != null) day.setMorningActivities(dto.getMorningActivities());
            if (dto.getAfternoonActivities() != null) day.setAfternoonActivities(dto.getAfternoonActivities());
            if (dto.getEveningActivities() != null) day.setEveningActivities(dto.getEveningActivities());
            if (dto.getWildlifeHighlights() != null) day.setWildlifeHighlights(dto.getWildlifeHighlights());
            if (dto.getScenicHighlights() != null) day.setScenicHighlights(dto.getScenicHighlights());
            if (dto.getSpecialNotes() != null) day.setSpecialNotes(dto.getSpecialNotes());
            if (dto.getStartLocation() != null) day.setStartLocation(dto.getStartLocation());
            if (dto.getEndLocation() != null) day.setEndLocation(dto.getEndLocation());
            if (dto.getDistanceKm() != null) day.setDistanceKm(dto.getDistanceKm());
            if (dto.getIsOvernight() != null) day.setIsOvernight(dto.getIsOvernight());
            if (dto.getMealsIncluded() != null) day.setMealsIncluded(dto.getMealsIncluded());
            if (dto.getInternalNotes() != null) day.setInternalNotes(dto.getInternalNotes());

            QuoteDay saved = quoteDayRepository.save(day);
            quoteCostEstimationService.triggerRecalc(saved.getQuote().getId());
            return ResponseEntity.ok(ApiResponse.success(200, "Day updated successfully", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error updating quote day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update quote day", "QUOTE_DAY_UPDATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteDay(String dayIdObf) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid day ID", "INVALID_DAY_ID"));
            }
            QuoteDay day = quoteDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Day not found", "DAY_NOT_FOUND"));
            }
            Long quoteId = day.getQuote().getId();
            quoteDayRepository.delete(day);
            quoteCostEstimationService.triggerRecalc(quoteId);
            return ResponseEntity.ok(ApiResponse.success(200, "Day deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting quote day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete quote day", "QUOTE_DAY_DELETE_FAILED"));
        }
    }

    private QuoteDayDTO toDTO(QuoteDay day) {
        QuoteDayDTO dto = new QuoteDayDTO();
        dto.setId(idObfuscator.encodeId(day.getId()));
        dto.setQuoteId(idObfuscator.encodeId(day.getQuote().getId()));
        dto.setDayNumber(day.getDayNumber());
        dto.setDayTag(day.getDayTag());
        dto.setTitle(day.getTitle());
        dto.setDescription(day.getDescription());
        dto.setMorningActivities(day.getMorningActivities());
        dto.setAfternoonActivities(day.getAfternoonActivities());
        dto.setEveningActivities(day.getEveningActivities());
        dto.setWildlifeHighlights(day.getWildlifeHighlights());
        dto.setScenicHighlights(day.getScenicHighlights());
        dto.setSpecialNotes(day.getSpecialNotes());
        dto.setStartLocation(day.getStartLocation());
        dto.setEndLocation(day.getEndLocation());
        dto.setDistanceKm(day.getDistanceKm());
        dto.setIsOvernight(day.getIsOvernight());
        dto.setMealsIncluded(day.getMealsIncluded());
        dto.setInternalNotes(day.getInternalNotes());
        dto.setActivityCount(actRepository.countByQuoteDayId(day.getId()));
        dto.setAccommodationCount(accRepository.countByQuoteDayId(day.getId()));
        dto.setParkCount(parkRepository.countByQuoteDayId(day.getId()));
        dto.setCreatedAt(day.getCreatedAt());
        dto.setUpdatedAt(day.getUpdatedAt());
        return dto;
    }
}
