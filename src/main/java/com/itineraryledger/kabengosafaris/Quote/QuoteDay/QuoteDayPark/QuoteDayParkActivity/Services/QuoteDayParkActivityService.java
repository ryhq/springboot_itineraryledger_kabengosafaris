package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Services;

import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.DTOs.CreateQuoteDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.DTOs.QuoteDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.DTOs.UpdateQuoteDayParkActivityDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Entity.QuoteDayParkActivity;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Repository.QuoteDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Repository.QuoteDayParkRepository;
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
public class QuoteDayParkActivityService {

    private final QuoteDayParkRepository quoteDayParkRepository;
    private final QuoteDayParkActivityRepository quoteDayParkActivityRepository;
    private final ParkActivityRepository parkActivityRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> list(String dayParkIdObf) {
        try {
            Long dayParkId = idObfuscator.decodeId(dayParkIdObf);
            if (dayParkId == null || !quoteDayParkRepository.existsById(dayParkId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote day park not found", "QUOTE_DAY_PARK_NOT_FOUND"));
            }
            List<QuoteDayParkActivity> rows = quoteDayParkActivityRepository.findByQuoteDayParkIdOrderBySortOrderAsc(dayParkId);
            List<QuoteDayParkActivityDTO> dtos = rows.stream().map(this::toDTO).collect(Collectors.toList());
            Map<String, Object> body = new HashMap<>();
            body.put("parkActivities", dtos);
            body.put("total", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Retrieved " + dtos.size() + " park activities", body));
        } catch (Exception e) {
            log.error("Error listing quote day park activities", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to list park activities", "QDPA_LIST_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayParkActivity row = quoteDayParkActivityRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDPA_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Park activity retrieved successfully", toDTO(row)));
        } catch (Exception e) {
            log.error("Error fetching quote day park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch park activity", "QDPA_FETCH_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String dayParkIdObf, CreateQuoteDayParkActivityDTO dto) {
        try {
            Long dayParkId = idObfuscator.decodeId(dayParkIdObf);
            if (dayParkId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid day-park ID", "INVALID_ID"));
            }
            QuoteDayPark parkVisit = quoteDayParkRepository.findById(dayParkId).orElse(null);
            if (parkVisit == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Quote day park not found", "QUOTE_DAY_PARK_NOT_FOUND"));
            }
            Long parkId = parkVisit.getPark().getId();
            Long activityId = idObfuscator.decodeId(dto.getActivityId());
            if (activityId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid activity ID", "INVALID_ACTIVITY_ID"));
            }
            ParkActivity parkActivity = parkActivityRepository.findByParkIdAndActivityId(parkId, activityId).orElse(null);
            if (parkActivity == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404,
                        "ParkActivity not found for park=" + parkId + " activity=" + activityId,
                        "PARK_ACTIVITY_NOT_FOUND"));
            }

            QuoteDayParkActivity row = QuoteDayParkActivity.builder()
                    .quoteDayPark(parkVisit)
                    .parkActivity(parkActivity)
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                    .durationHours(dto.getDurationHours())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .notes(dto.getNotes())
                    .isIncludedInPrice(dto.getIsIncludedInPrice() != null ? dto.getIsIncludedInPrice() : true)
                    .build();
            QuoteDayParkActivity saved = quoteDayParkActivityRepository.save(row);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Park activity added", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error creating quote day park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to add park activity", "QDPA_CREATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> update(String idObf, UpdateQuoteDayParkActivityDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayParkActivity row = quoteDayParkActivityRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDPA_NOT_FOUND"));
            }
            if (dto.getSortOrder() != null) row.setSortOrder(dto.getSortOrder());
            if (dto.getDurationHours() != null) row.setDurationHours(dto.getDurationHours());
            if (dto.getStartTime() != null) row.setStartTime(dto.getStartTime());
            if (dto.getEndTime() != null) row.setEndTime(dto.getEndTime());
            if (dto.getNotes() != null) row.setNotes(dto.getNotes());
            if (dto.getIsIncludedInPrice() != null) row.setIsIncludedInPrice(dto.getIsIncludedInPrice());
            QuoteDayParkActivity saved = quoteDayParkActivityRepository.save(row);
            return ResponseEntity.ok(ApiResponse.success(200, "Park activity updated successfully", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error updating quote day park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update park activity", "QDPA_UPDATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayParkActivity row = quoteDayParkActivityRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDPA_NOT_FOUND"));
            }
            quoteDayParkActivityRepository.delete(row);
            return ResponseEntity.ok(ApiResponse.success(200, "Park activity deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting quote day park activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete park activity", "QDPA_DELETE_FAILED"));
        }
    }

    private QuoteDayParkActivityDTO toDTO(QuoteDayParkActivity row) {
        QuoteDayParkActivityDTO dto = new QuoteDayParkActivityDTO();
        dto.setId(idObfuscator.encodeId(row.getId()));
        dto.setQuoteDayParkId(idObfuscator.encodeId(row.getQuoteDayPark().getId()));
        if (row.getParkActivity() != null) {
            if (row.getParkActivity().getPark() != null) {
                dto.setParkId(idObfuscator.encodeId(row.getParkActivity().getPark().getId()));
                dto.setParkName(row.getParkActivity().getPark().getName());
            }
            if (row.getParkActivity().getActivity() != null) {
                dto.setActivityId(idObfuscator.encodeId(row.getParkActivity().getActivity().getId()));
                dto.setActivityName(row.getParkActivity().getActivity().getName());
            }
        }
        dto.setSortOrder(row.getSortOrder());
        dto.setDurationHours(row.getDurationHours());
        dto.setStartTime(row.getStartTime());
        dto.setEndTime(row.getEndTime());
        dto.setNotes(row.getNotes());
        dto.setIsIncludedInPrice(row.getIsIncludedInPrice());
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }
}
