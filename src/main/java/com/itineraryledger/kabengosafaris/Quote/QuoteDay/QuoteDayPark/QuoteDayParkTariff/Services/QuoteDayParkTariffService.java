package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Services;

import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Entity.QuoteDayPark;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.DTOs.CreateQuoteDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.DTOs.QuoteDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.DTOs.UpdateQuoteDayParkTariffDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Entity.QuoteDayParkTariff;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.QuoteDayParkTariff.Repository.QuoteDayParkTariffRepository;
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
public class QuoteDayParkTariffService {

    private final QuoteDayParkRepository quoteDayParkRepository;
    private final QuoteDayParkTariffRepository quoteDayParkTariffRepository;
    private final ParkTariffRepository parkTariffRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> list(String dayParkIdObf) {
        try {
            Long dayParkId = idObfuscator.decodeId(dayParkIdObf);
            if (dayParkId == null || !quoteDayParkRepository.existsById(dayParkId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote day park not found", "QUOTE_DAY_PARK_NOT_FOUND"));
            }
            List<QuoteDayParkTariff> rows = quoteDayParkTariffRepository.findByQuoteDayParkIdOrderByIdAsc(dayParkId);
            List<QuoteDayParkTariffDTO> dtos = rows.stream().map(this::toDTO).collect(Collectors.toList());
            Map<String, Object> body = new HashMap<>();
            body.put("parkTariffs", dtos);
            body.put("total", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Retrieved " + dtos.size() + " park tariffs", body));
        } catch (Exception e) {
            log.error("Error listing quote day park tariffs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to list park tariffs", "QDPT_LIST_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayParkTariff row = quoteDayParkTariffRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDPT_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Park tariff retrieved successfully", toDTO(row)));
        } catch (Exception e) {
            log.error("Error fetching quote day park tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch park tariff", "QDPT_FETCH_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String dayParkIdObf, CreateQuoteDayParkTariffDTO dto) {
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
            Long tariffId = idObfuscator.decodeId(dto.getTariffId());
            if (tariffId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid tariff ID", "INVALID_TARIFF_ID"));
            }
            ParkTariff parkTariff = parkTariffRepository.findByParkIdAndTariffId(parkId, tariffId).orElse(null);
            if (parkTariff == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404,
                        "ParkTariff not found for park=" + parkId + " tariff=" + tariffId,
                        "PARK_TARIFF_NOT_FOUND"));
            }
            QuoteDayParkTariff row = QuoteDayParkTariff.builder()
                    .quoteDayPark(parkVisit)
                    .parkTariff(parkTariff)
                    .notes(dto.getNotes())
                    .isIncludedInPrice(dto.getIsIncludedInPrice() != null ? dto.getIsIncludedInPrice() : true)
                    .build();
            QuoteDayParkTariff saved = quoteDayParkTariffRepository.save(row);
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Park tariff added", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error creating quote day park tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to add park tariff", "QDPT_CREATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> update(String idObf, UpdateQuoteDayParkTariffDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayParkTariff row = quoteDayParkTariffRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDPT_NOT_FOUND"));
            }
            if (dto.getNotes() != null) row.setNotes(dto.getNotes());
            if (dto.getIsIncludedInPrice() != null) row.setIsIncludedInPrice(dto.getIsIncludedInPrice());
            QuoteDayParkTariff saved = quoteDayParkTariffRepository.save(row);
            return ResponseEntity.ok(ApiResponse.success(200, "Park tariff updated successfully", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error updating quote day park tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update park tariff", "QDPT_UPDATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayParkTariff row = quoteDayParkTariffRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDPT_NOT_FOUND"));
            }
            quoteDayParkTariffRepository.delete(row);
            return ResponseEntity.ok(ApiResponse.success(200, "Park tariff deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting quote day park tariff", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete park tariff", "QDPT_DELETE_FAILED"));
        }
    }

    private QuoteDayParkTariffDTO toDTO(QuoteDayParkTariff row) {
        QuoteDayParkTariffDTO dto = new QuoteDayParkTariffDTO();
        dto.setId(idObfuscator.encodeId(row.getId()));
        dto.setQuoteDayParkId(idObfuscator.encodeId(row.getQuoteDayPark().getId()));
        if (row.getParkTariff() != null) {
            if (row.getParkTariff().getPark() != null) {
                dto.setParkId(idObfuscator.encodeId(row.getParkTariff().getPark().getId()));
                dto.setParkName(row.getParkTariff().getPark().getName());
            }
            if (row.getParkTariff().getTariff() != null) {
                dto.setTariffId(idObfuscator.encodeId(row.getParkTariff().getTariff().getId()));
                dto.setTariffName(row.getParkTariff().getTariff().getName());
            }
        }
        dto.setNotes(row.getNotes());
        dto.setIsIncludedInPrice(row.getIsIncludedInPrice());
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }
}
