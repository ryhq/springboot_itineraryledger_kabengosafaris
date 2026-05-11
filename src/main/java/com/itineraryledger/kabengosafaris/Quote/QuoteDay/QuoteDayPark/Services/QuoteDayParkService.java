package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Services;

import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark.ParkEntryType;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.DTOs.CreateQuoteDayParkDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.DTOs.QuoteDayParkDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.DTOs.UpdateQuoteDayParkDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkActivity.Repository.QuoteDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Repository.QuoteDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Repository.QuoteDayParkRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Repository.QuoteDayRepository;
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
public class QuoteDayParkService {

    private final QuoteDayRepository quoteDayRepository;
    private final QuoteDayParkRepository quoteDayParkRepository;
    private final QuoteDayParkActivityRepository parkActivityRepository;
    private final QuoteDayParkTariffRepository parkTariffRepository;
    private final ParkRepository parkRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> list(String dayIdObf) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null || !quoteDayRepository.existsById(dayId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote day not found", "QUOTE_DAY_NOT_FOUND"));
            }
            List<QuoteDayPark> rows = quoteDayParkRepository.findByQuoteDayIdOrderBySortOrderAsc(dayId);
            List<QuoteDayParkDTO> dtos = rows.stream().map(this::toDTO).collect(Collectors.toList());
            Map<String, Object> body = new HashMap<>();
            body.put("parks", dtos);
            body.put("total", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Retrieved " + dtos.size() + " parks", body));
        } catch (Exception e) {
            log.error("Error listing quote day parks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to list parks", "QDP_LIST_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayPark row = quoteDayParkRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDP_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Park retrieved successfully", toDTO(row)));
        } catch (Exception e) {
            log.error("Error fetching quote day park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch park", "QDP_FETCH_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String dayIdObf, CreateQuoteDayParkDTO dto) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid day ID", "INVALID_DAY_ID"));
            }
            QuoteDay day = quoteDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Quote day not found", "QUOTE_DAY_NOT_FOUND"));
            }
            Long parkId = idObfuscator.decodeId(dto.getParkId());
            Park park = parkId == null ? null : parkRepository.findById(parkId).orElse(null);
            if (park == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
            }
            QuoteDayPark row = QuoteDayPark.builder()
                    .quoteDay(day)
                    .park(park)
                    .entryType(dto.getEntryType() != null ? dto.getEntryType() : ParkEntryType.DAY_TRIP)
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                    .arrivalTime(dto.getArrivalTime())
                    .departureTime(dto.getDepartureTime())
                    .notes(dto.getNotes())
                    .build();
            QuoteDayPark saved = quoteDayParkRepository.save(row);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Park added to quote day", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error creating quote day park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to add park", "QDP_CREATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> update(String idObf, UpdateQuoteDayParkDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayPark row = quoteDayParkRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDP_NOT_FOUND"));
            }
            if (dto.getParkId() != null) {
                Long parkId = idObfuscator.decodeId(dto.getParkId());
                Park park = parkId == null ? null : parkRepository.findById(parkId).orElse(null);
                if (park == null) {
                    return ResponseEntity.status(404).body(ApiResponse.error(404, "Park not found", "PARK_NOT_FOUND"));
                }
                row.setPark(park);
            }
            if (dto.getEntryType() != null) row.setEntryType(dto.getEntryType());
            if (dto.getSortOrder() != null) row.setSortOrder(dto.getSortOrder());
            if (dto.getArrivalTime() != null) row.setArrivalTime(dto.getArrivalTime());
            if (dto.getDepartureTime() != null) row.setDepartureTime(dto.getDepartureTime());
            if (dto.getNotes() != null) row.setNotes(dto.getNotes());
            QuoteDayPark saved = quoteDayParkRepository.save(row);
            return ResponseEntity.ok(ApiResponse.success(200, "Park updated successfully", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error updating quote day park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update park", "QDP_UPDATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayPark row = quoteDayParkRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDP_NOT_FOUND"));
            }
            quoteDayParkRepository.delete(row);
            return ResponseEntity.ok(ApiResponse.success(200, "Park deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting quote day park", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete park", "QDP_DELETE_FAILED"));
        }
    }

    private QuoteDayParkDTO toDTO(QuoteDayPark row) {
        QuoteDayParkDTO dto = new QuoteDayParkDTO();
        dto.setId(idObfuscator.encodeId(row.getId()));
        dto.setQuoteDayId(idObfuscator.encodeId(row.getQuoteDay().getId()));
        if (row.getPark() != null) {
            dto.setParkId(idObfuscator.encodeId(row.getPark().getId()));
            dto.setParkName(row.getPark().getName());
        }
        dto.setEntryType(row.getEntryType());
        dto.setEntryTypeDisplayName(row.getEntryType() != null ? row.getEntryType().getDisplayName() : null);
        dto.setSortOrder(row.getSortOrder());
        dto.setArrivalTime(row.getArrivalTime());
        dto.setDepartureTime(row.getDepartureTime());
        dto.setNotes(row.getNotes());
        dto.setParkActivityCount(parkActivityRepository.countByQuoteDayParkId(row.getId()));
        dto.setParkTariffCount(parkTariffRepository.countByQuoteDayParkId(row.getId()));
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }
}
