package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.DTOs;

import lombok.*;

/**
 * What can be said about one fee on one park visit.
 *
 * Which fees apply is a set, changed as a whole (see the PUT on the collection).
 * These two are about a single fee: whether it is inside the quoted price, and
 * what the office needs to know about it. Null means leave unchanged.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateItineraryDayParkTariffDTO {

    /** Off and the cost estimate leaves it out, though it stays on the visit. */
    private Boolean isIncludedInPrice;

    private String notes;
}
