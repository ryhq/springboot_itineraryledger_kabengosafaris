package com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Services;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs.CreateQuoteDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs.QuoteDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.DTOs.UpdateQuoteDayAccommodationDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Entity.QuoteDayAccommodation;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository.QuoteDayAccommodationRepository;
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
public class QuoteDayAccommodationService {

    private final QuoteDayRepository quoteDayRepository;
    private final QuoteDayAccommodationRepository quoteDayAccommodationRepository;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationRoomTypeRepository roomTypeRepository;
    private final AccommodationRoomStandardRepository roomStandardRepository;
    private final AccommodationBoardTypeRepository boardTypeRepository;
    private final QuoteCostEstimationService quoteCostEstimationService;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> list(String dayIdObf) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null || !quoteDayRepository.existsById(dayId)) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote day not found", "QUOTE_DAY_NOT_FOUND"));
            }
            List<QuoteDayAccommodation> rows = quoteDayAccommodationRepository.findByQuoteDayIdOrderByIdAsc(dayId);
            List<QuoteDayAccommodationDTO> dtos = rows.stream().map(this::toDTO).collect(Collectors.toList());

            Map<String, Object> body = new HashMap<>();
            body.put("accommodations", dtos);
            body.put("total", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Retrieved " + dtos.size() + " accommodations", body));
        } catch (Exception e) {
            log.error("Error listing quote day accommodations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to list accommodations", "QDA_LIST_FAILED"));
        }
    }

    public ResponseEntity<ApiResponse<?>> getById(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayAccommodation row = quoteDayAccommodationRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDA_NOT_FOUND"));
            }
            return ResponseEntity.ok(ApiResponse.success(200, "Accommodation retrieved successfully", toDTO(row)));
        } catch (Exception e) {
            log.error("Error fetching quote day accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to fetch accommodation", "QDA_FETCH_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String dayIdObf, CreateQuoteDayAccommodationDTO dto) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            if (dayId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid day ID", "INVALID_DAY_ID"));
            }
            QuoteDay day = quoteDayRepository.findById(dayId).orElse(null);
            if (day == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Quote day not found", "QUOTE_DAY_NOT_FOUND"));
            }

            Accommodation acc = lookupAccommodation(dto.getAccommodationId());
            AccommodationRoomType rt = lookupRoomType(dto.getRoomTypeId());
            AccommodationRoomStandard rs = lookupRoomStandard(dto.getRoomStandardId());
            AccommodationBoardType bt = lookupBoardType(dto.getBoardTypeId());
            if (acc == null || rt == null || rs == null || bt == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404,
                        "One of accommodation/roomType/roomStandard/boardType not found", "REF_NOT_FOUND"));
            }

            QuoteDayAccommodation row = QuoteDayAccommodation.builder()
                    .quoteDay(day)
                    .accommodation(acc)
                    .roomType(rt)
                    .roomStandard(rs)
                    .boardType(bt)
                    .roomCount(dto.getRoomCount() != null ? dto.getRoomCount() : 1)
                    .isAlternative(dto.getIsAlternative() != null ? dto.getIsAlternative() : false)
                    .notes(dto.getNotes())
                    .build();
            QuoteDayAccommodation saved = quoteDayAccommodationRepository.save(row);
            quoteCostEstimationService.triggerRecalc(saved.getQuoteDay().getQuote().getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201, "Accommodation added to quote day", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error creating quote day accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to add accommodation", "QDA_CREATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> update(String idObf, UpdateQuoteDayAccommodationDTO dto) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayAccommodation row = quoteDayAccommodationRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDA_NOT_FOUND"));
            }
            if (dto.getAccommodationId() != null) {
                Accommodation acc = lookupAccommodation(dto.getAccommodationId());
                if (acc == null) return notFound("Accommodation");
                row.setAccommodation(acc);
            }
            if (dto.getRoomTypeId() != null) {
                AccommodationRoomType rt = lookupRoomType(dto.getRoomTypeId());
                if (rt == null) return notFound("Room Type");
                row.setRoomType(rt);
            }
            if (dto.getRoomStandardId() != null) {
                AccommodationRoomStandard rs = lookupRoomStandard(dto.getRoomStandardId());
                if (rs == null) return notFound("Room Standard");
                row.setRoomStandard(rs);
            }
            if (dto.getBoardTypeId() != null) {
                AccommodationBoardType bt = lookupBoardType(dto.getBoardTypeId());
                if (bt == null) return notFound("Board Type");
                row.setBoardType(bt);
            }
            if (dto.getRoomCount() != null) row.setRoomCount(dto.getRoomCount());
            if (dto.getIsAlternative() != null) row.setIsAlternative(dto.getIsAlternative());
            if (dto.getNotes() != null) row.setNotes(dto.getNotes());

            QuoteDayAccommodation saved = quoteDayAccommodationRepository.save(row);
            quoteCostEstimationService.triggerRecalc(saved.getQuoteDay().getQuote().getId());
            return ResponseEntity.ok(ApiResponse.success(200, "Accommodation updated successfully", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error updating quote day accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to update accommodation", "QDA_UPDATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(String idObf) {
        try {
            Long id = idObfuscator.decodeId(idObf);
            if (id == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid ID", "INVALID_ID"));
            }
            QuoteDayAccommodation row = quoteDayAccommodationRepository.findById(id).orElse(null);
            if (row == null) {
                return ResponseEntity.status(404).body(ApiResponse.error(404, "Not found", "QDA_NOT_FOUND"));
            }
            Long quoteId = row.getQuoteDay().getQuote().getId();
            quoteDayAccommodationRepository.delete(row);
            quoteCostEstimationService.triggerRecalc(quoteId);
            return ResponseEntity.ok(ApiResponse.success(200, "Accommodation deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting quote day accommodation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to delete accommodation", "QDA_DELETE_FAILED"));
        }
    }

    private Accommodation lookupAccommodation(String obf) {
        Long id = idObfuscator.decodeId(obf);
        return id == null ? null : accommodationRepository.findById(id).orElse(null);
    }
    private AccommodationRoomType lookupRoomType(String obf) {
        Long id = idObfuscator.decodeId(obf);
        return id == null ? null : roomTypeRepository.findById(id).orElse(null);
    }
    private AccommodationRoomStandard lookupRoomStandard(String obf) {
        Long id = idObfuscator.decodeId(obf);
        return id == null ? null : roomStandardRepository.findById(id).orElse(null);
    }
    private AccommodationBoardType lookupBoardType(String obf) {
        Long id = idObfuscator.decodeId(obf);
        return id == null ? null : boardTypeRepository.findById(id).orElse(null);
    }
    private ResponseEntity<ApiResponse<?>> notFound(String what) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, what + " not found", "REF_NOT_FOUND"));
    }

    private QuoteDayAccommodationDTO toDTO(QuoteDayAccommodation row) {
        QuoteDayAccommodationDTO dto = new QuoteDayAccommodationDTO();
        dto.setId(idObfuscator.encodeId(row.getId()));
        dto.setQuoteDayId(idObfuscator.encodeId(row.getQuoteDay().getId()));
        if (row.getAccommodation() != null) {
            dto.setAccommodationId(idObfuscator.encodeId(row.getAccommodation().getId()));
            dto.setAccommodationName(row.getAccommodation().getName());
        }
        if (row.getRoomType() != null) {
            dto.setRoomTypeId(idObfuscator.encodeId(row.getRoomType().getId()));
            dto.setRoomTypeName(row.getRoomType().getName());
        }
        if (row.getRoomStandard() != null) {
            dto.setRoomStandardId(idObfuscator.encodeId(row.getRoomStandard().getId()));
            dto.setRoomStandardName(row.getRoomStandard().getName());
        }
        if (row.getBoardType() != null) {
            dto.setBoardTypeId(idObfuscator.encodeId(row.getBoardType().getId()));
            dto.setBoardTypeName(row.getBoardType().getName());
        }
        dto.setRoomCount(row.getRoomCount());
        dto.setIsAlternative(row.getIsAlternative());
        dto.setNotes(row.getNotes());
        dto.setCreatedAt(row.getCreatedAt());
        dto.setUpdatedAt(row.getUpdatedAt());
        return dto;
    }
}
