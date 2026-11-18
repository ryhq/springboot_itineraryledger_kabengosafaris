package com.itineraryledger.kabengosafaris.Quote.QuoteDay.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adding a day to a quotation that the itinerary behind it does not have.
 *
 * <p>This is the whole point of the change it arrived with: a client's trip grows an arrival
 * night or a few days at the coast end, and that is a change to this client's quotation, not a
 * reason to build a second catalogue itinerary nobody will ever sell again.
 *
 * <p>There is no {@code dayNumber} and no date here, both on purpose. The position is
 * {@link #position}, and every number and date in the quote is then recomputed from it by
 * {@code QuoteDayCalendarService} - so the days cannot end up disagreeing with the dates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuoteDayDTO {

    @NotBlank(message = "Day title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    /**
     * Where the new day lands, 1-based. Null or past the end appends.
     *
     * <p><b>Position 1 moves the quote's start date back a day</b>, because the start date is
     * by definition the date of day 1 and every existing day must keep the date it already had.
     * That is what makes "they land at two in the morning, so we need the night before" a thing
     * a quotation can say.
     */
    private Integer position;

    private String description;
    private String morningActivities;
    private String afternoonActivities;
    private String eveningActivities;
    private String wildlifeHighlights;
    private String scenicHighlights;
    private String specialNotes;
    private String startLocation;
    private String endLocation;
    private Integer distanceKm;

    /** Defaults to true: a day added to a quotation is nearly always a night somebody sleeps. */
    private Boolean isOvernight;

    private String mealsIncluded;
    private String internalNotes;
}
