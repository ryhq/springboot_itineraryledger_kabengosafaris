package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Services;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.DTOs.CreateQuoteDayActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.DTOs.QuoteDayActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.DTOs.UpdateQuoteDayActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Entity.QuoteDayActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Repository.QuoteDayActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Repository.QuoteDayRepository;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteDayActivityService {

    private final QuoteDayRepository quoteDayRepository;
    private final QuoteDayActivityRepository quoteDayActivityRepository;
    private final ActivityRepository activityRepository;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> list(String dayIdObf) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null || !quoteDayRepository.existsById(dayId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote day not found", "QUOTE_DAY_NOT_FOUND"));
            }
            List<QuoteDayActivity> rows = quoteDayActivityRepository.findByQuoteDayIdOrderBySortOrderAsc(dayId);
            List<QuoteDayActivityDTO> dtos = rows.stream().map(this::toDTO).collect(Collectors.toList());
            Map<String, Object> body = new HashMap<>();
            body.put("activities", dtos);
            body.put("total", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Retrieved " + dtos.size() + " activities", body));
        } catch (Exception e) {
            log.error("Error listing quote day activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to list activities", "QDACT_LIST_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayActivity row = quoteDayActivityRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDACT_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Activity retrieved successfully", toDTO(row)));
        } catch (Exception e) {
            log.error("Error fetching quote day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch activity", "QDACT_FETCH_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String dayIdObf, CreateQuoteDayActivityDTO dto) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid day ID", "INVALID_DAY_ID"));
            }
            QuoteDay day = quoteDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Quote day not found", "QUOTE_DAY_NOT_FOUND"));
            }
            Long activityId = idObfuscator.decodeId(dto.getActivityId());
            Activity activity = activityId == null ? null : activityRepository.findById(activityId).orElse(null);
            if (activity == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
            }
            QuoteDayActivity row = QuoteDayActivity.builder()
                    .quoteDay(day)
                    .activity(activity)
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                    .durationHours(dto.getDurationHours())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .notes(dto.getNotes())
                    .isIncludedInPrice(dto.getIsIncludedInPrice() != null ? dto.getIsIncludedInPrice() : true)
                    .isOptional(dto.getIsOptional() != null ? dto.getIsOptional() : false)
                    .build();
            QuoteDayActivity saved = quoteDayActivityRepository.save(row);
            quoteCostEstimationService.triggerRecalc(saved.getQuoteDay().getQuote().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Activity added to quote day", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error creating quote day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to add activity", "QDACT_CREATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> update(String idObf, UpdateQuoteDayActivityDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayActivity row = quoteDayActivityRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDACT_NOT_FOUND"));
            }
            if (dto.getActivityId() != null) {
                Long activityId = idObfuscator.decodeId(dto.getActivityId());
                Activity activity = activityId == null ? null : activityRepository.findById(activityId).orElse(null);
                if (activity == null) {
                    return ResponseEntity.status(404).body(ApiResponse.error(404, "Activity not found", "ACTIVITY_NOT_FOUND"));
                }
                row.setActivity(activity);
            }
            if (dto.getSortOrder() != null) row.setSortOrder(dto.getSortOrder());
            if (dto.getDurationHours() != null) row.setDurationHours(dto.getDurationHours());
            if (dto.getStartTime() != null) row.setStartTime(dto.getStartTime());
            if (dto.getEndTime() != null) row.setEndTime(dto.getEndTime());
            if (dto.getNotes() != null) row.setNotes(dto.getNotes());
            if (dto.getIsIncludedInPrice() != null) row.setIsIncludedInPrice(dto.getIsIncludedInPrice());
            if (dto.getIsOptional() != null) row.setIsOptional(dto.getIsOptional());
            QuoteDayActivity saved = quoteDayActivityRepository.save(row);
            quoteCostEstimationService.triggerRecalc(saved.getQuoteDay().getQuote().getId());
            return ResponseEntity.ok(ApiResponse.success(200, "Activity updated successfully", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error updating quote day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update activity", "QDACT_UPDATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayActivity row = quoteDayActivityRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDACT_NOT_FOUND"));
            }
            Long quoteId = row.getQuoteDay().getQuote().getId();
            quoteDayActivityRepository.delete(row);
            quoteCostEstimationService.triggerRecalc(quoteId);
            return ResponseEntity.ok(ApiResponse.success(200, "Activity deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting quote day activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete activity", "QDACT_DELETE_FAILED"));
        }
    }

    private QuoteDayActivityDTO toDTO(QuoteDayActivity row) {
        QuoteDayActivityDTO dto = new QuoteDayActivityDTO();
        dto.setId(idObfuscator.encodeId(row.getId()));
        dto.setQuoteDayId(idObfuscator.encodeId(row.getQuoteDay().getId()));
        if (row.getActivity() != null) {
            dto.setActivityId(idObfuscator.encodeId(row.getActivity().getId()));
            dto.setActivityName(row.getActivity().getName());
        }
        dto.setSortOrder(row.getSortOrder());
        dto.setDurationHours(row.getDurationHours());
        dto.setStartTime(row.getStartTime());
        dto.setEndTime(row.getEndTime());
        dto.setNotes(row.getNotes());
        dto.setIsIncludedInPrice(row.getIsIncludedInPrice());
        dto.setIsOptional(row.getIsOptional());
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }
}
