package com.itineraryledger.kabengosafaris.Quote.QuoteDay.Services;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.CreateQuoteDayDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.QuoteDayDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs.UpdateQuoteDayDTO;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Entity.QuoteDay;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayAccommodation.Repository.QuoteDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayActivity.Repository.QuoteDayActivityRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.QuoteDayPark.Repository.QuoteDayParkRepository;
import com.itineraryledger.kabengosafaris.Quote.QuoteDay.Repository.QuoteDayRepository;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteCostEstimationService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteTreeCopyService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QuoteDayService - the days a quotation owns.
 *
 * <p>Days arrive here by the Itinerary-to-Quote deep copy, and <b>a quotation may then add its
 * own</b>. It used to be unable to: the day tree was fixed at whatever the catalogue trip had,
 * on the reasoning that a quote is priced from what it copied, so an invented day would carry no
 * price. That stopped being true when {@link QuoteCostEstimationService#triggerRecalc} began
 * re-deriving every item from the day tree after each write - an added day is now priced exactly
 * like a copied one.
 *
 * <p>The restriction cost real money in the meantime. A party landing at two in the morning needs
 * the room from the night before, and a party staying on after the safari needs the nights after
 * it; neither is a reason to build a second catalogue itinerary that will never be sold again,
 * but that was the only way to quote it.
 *
 * <p>Numbering and dates are not decided here. Every path that moves days hands the order to
 * {@link QuoteDayCalendarService}, so there is one rule rather than one per operation.
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
    private final QuoteDayCalendarService calendar;
    private final QuoteTreeCopyService treeCopy;
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

    /**
     * Add a day this quotation's itinerary does not have.
     *
     * <p>{@code position} is 1-based and clamped; null appends. <b>Position 1 moves the quote's
     * start date back one day</b> - the start date is the date of day 1 by definition, so this is
     * the only way the days already in the quote keep the dates they already had. The response
     * says when that happened, because a silently moved start date would reprice the whole trip
     * into a different season without anybody being told.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> createDay(String quoteIdObf, CreateQuoteDayDTO dto) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObf);
            Quote quote = quoteId == null ? null : quoteRepository.findById(quoteId).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }

            List<QuoteDay> days = new ArrayList<>(
                    quoteDayRepository.findByQuoteIdOrderByDayNumberAsc(quoteId));

            int position = dto.getPosition() == null ? days.size() + 1
                    : Math.max(1, Math.min(dto.getPosition(), days.size() + 1));

            LocalDate movedStartTo = null;
            if (position == 1 && quote.getSafariStartDate() != null && !days.isEmpty()) {
                movedStartTo = quote.getSafariStartDate().minusDays(1);
                quote.setSafariStartDate(movedStartTo);
                quoteRepository.save(quote);
            }

            QuoteDay day = QuoteDay.builder()
                    .quote(quote)
                    .title(dto.getTitle())
                    .description(dto.getDescription())
                    .morningActivities(dto.getMorningActivities())
                    .afternoonActivities(dto.getAfternoonActivities())
                    .eveningActivities(dto.getEveningActivities())
                    .wildlifeHighlights(dto.getWildlifeHighlights())
                    .scenicHighlights(dto.getScenicHighlights())
                    .specialNotes(dto.getSpecialNotes())
                    .startLocation(dto.getStartLocation())
                    .endLocation(dto.getEndLocation())
                    .distanceKm(dto.getDistanceKm())
                    /* A day added to a quotation is nearly always a night somebody sleeps. */
                    .isOvernight(dto.getIsOvernight() == null ? Boolean.TRUE : dto.getIsOvernight())
                    .mealsIncluded(dto.getMealsIncluded())
                    .internalNotes(dto.getInternalNotes())
                    .build();

            days.add(position - 1, day);
            calendar.recalculate(quote, days);
            quoteCostEstimationService.triggerRecalc(quoteId);

            Map<String, Object> body = new HashMap<>();
            body.put("day", toDTO(day));
            body.put("totalDays", days.size());
            body.put("safariStartDate", quote.getSafariStartDate());
            body.put("startDateMoved", movedStartTo != null);
            String message = movedStartTo == null
                    ? "Day added at position " + position
                    : "Day added in front; the quote now starts on " + movedStartTo;
            return ResponseEntity.ok(ApiResponse.success(200, message, body));
        } catch (Exception e) {
            log.error("Error creating quote day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to create quote day", "QUOTE_DAY_CREATE_FAILED"));
        }
    }

    /**
     * Copy a day, with its stays, activities, parks and flights, directly after the original.
     *
     * <p>Three nights in the same camp are three days that differ by a sentence, and the copy
     * lands next rather than last because "another night here" means here. The copying itself is
     * {@link QuoteTreeCopyService#copyDay}, the same code a new version uses, so a duplicated day
     * cannot carry less than a copied one.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> duplicateDay(String dayIdObf) {
        try {
            Long dayId = idObfuscator.decodeId(dayIdObf);
            QuoteDay source = dayId == null ? null : quoteDayRepository.findById(dayId).orElse(null);
            if (source == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Day not found", "DAY_NOT_FOUND"));
            }
            Quote quote = source.getQuote();

            QuoteDay copy = treeCopy.copyDay(source);
            copy.setQuote(quote);

            List<QuoteDay> days = new ArrayList<>(
                    quoteDayRepository.findByQuoteIdOrderByDayNumberAsc(quote.getId()));
            int index = 0;
            while (index < days.size() && !days.get(index).getId().equals(source.getId())) index++;
            days.add(Math.min(index + 1, days.size()), copy);

            calendar.recalculate(quote, days);
            quoteCostEstimationService.triggerRecalc(quote.getId());

            Map<String, Object> body = new HashMap<>();
            body.put("day", toDTO(copy));
            body.put("totalDays", days.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Day duplicated successfully", body));
        } catch (Exception e) {
            log.error("Error duplicating quote day", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to duplicate quote day", "QUOTE_DAY_DUPLICATE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> reorderDays(String quoteIdObf, java.util.List<String> orderedDayIds) {
        try {
            Long quoteId = idObfuscator.decodeId(quoteIdObf);
            Quote quote = quoteId == null ? null : quoteRepository.findById(quoteId).orElse(null);
            if (quote == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Quote not found", "QUOTE_NOT_FOUND"));
            }
            if (orderedDayIds == null || orderedDayIds.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Ordered day ID list is required", "EMPTY_PAYLOAD"));
            }
            List<QuoteDay> ordered = new ArrayList<>();
            for (String obf : orderedDayIds) {
                Long id = idObfuscator.decodeId(obf);
                if (id == null) continue;
                QuoteDay day = quoteDayRepository.findById(id).orElse(null);
                if (day == null || !day.getQuote().getId().equals(quoteId)) continue;
                ordered.add(day);
            }
            /* A partial list would renumber the days it names and leave the rest holding the
             * numbers it just handed out. Say so rather than scramble the trip. */
            long total = quoteDayRepository.countByQuoteId(quoteId);
            if (ordered.size() != total) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                        "Send every day of the quote: got " + ordered.size() + " of " + total,
                        "INCOMPLETE_DAY_ORDER"));
            }

            calendar.recalculate(quote, ordered);
            quoteCostEstimationService.triggerRecalc(quoteId);
            return ResponseEntity.ok(ApiResponse.success(200,
                    "Renumbered " + ordered.size() + " days", ordered.size()));
        } catch (Exception e) {
            log.error("Error reordering quote days", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500, "Failed to reorder days", "QUOTE_DAY_REORDER_FAILED"));
        }
    }

    /**
     * Remove a day and close the gap.
     *
     * <p>Deleting day 1 moves the start date <i>forward</i>, for the same reason adding one in
     * front moves it back: the remaining days must keep the dates they already had.
     */
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
            Quote quote = day.getQuote();
            boolean wasFirst = Integer.valueOf(1).equals(day.getDayNumber());

            quoteDayRepository.delete(day);
            quoteDayRepository.flush();

            if (wasFirst && quote.getSafariStartDate() != null
                    && quoteDayRepository.countByQuoteId(quote.getId()) > 0) {
                quote.setSafariStartDate(quote.getSafariStartDate().plusDays(1));
                quoteRepository.save(quote);
            }
            calendar.recalculate(quote);
            quoteCostEstimationService.triggerRecalc(quote.getId());
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
        dto.setDate(day.getDayDate());
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
