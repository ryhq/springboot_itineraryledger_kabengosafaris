package com.itineraryledger.kabengosafaris.Safari;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.DTOs.CostLineItemDTO;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Enums.ExclusionReason;
import com.itineraryledger.kabengosafaris.Itinerary.CostEstimation.Services.Core.ParkTariffRateLookupService;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;
import com.itineraryledger.kabengosafaris.Safari.CostEstimation.Services.Calculators.SafariParkTariffCostCalculator;
import com.itineraryledger.kabengosafaris.Safari.DTOs.FullSafariDTO;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A waived fee is not a fee.
 *
 * {@code SafariDayParkTariff} has carried {@code isWaived} and a {@code waive(reason)} method since
 * it was written, and the repository's unpaid-fee queries respect it. The estimator read it
 * nowhere: a fee somebody had deliberately given away -- a comp, a resident exemption, a park's own
 * goodwill -- was still charged in the safari's cost, per person, per day it appeared.
 *
 * It is not simply dropped either. Money that was waived is money somebody gave away, so the line
 * is priced and shown with its reason; dropping it silently would hide the size of the gift.
 */
class WaivedParkFeesAreNotChargedTest {

    private static final Long PARK_ID = 41L;
    private static final Long CHARGED_TARIFF_ID = 7L;
    private static final Long WAIVED_TARIFF_ID = 8L;

    private ParkTariffRateLookupService lookup;
    private SafariParkTariffCostCalculator calculator;

    @BeforeEach
    void setUp() {
        lookup = mock(ParkTariffRateLookupService.class);
        ParkTariffRepository parkTariffRepository = mock(ParkTariffRepository.class);
        PaxNationCategoryRepository nationCategoryRepository = mock(PaxNationCategoryRepository.class);
        IdObfuscator idObfuscator = mock(IdObfuscator.class);

        /* The ids in a safari DTO are obfuscated and rotate on restart; the numbers are internal. */
        when(idObfuscator.decodeId("park")).thenReturn(PARK_ID);
        when(idObfuscator.decodeId("charged-tariff")).thenReturn(CHARGED_TARIFF_ID);
        when(idObfuscator.decodeId("waived-tariff")).thenReturn(WAIVED_TARIFF_ID);
        when(idObfuscator.decodeId("nation")).thenReturn(1L);
        when(idObfuscator.decodeId("age")).thenReturn(2L);
        when(idObfuscator.encodeId(anyLong())).thenReturn("encoded");

        Tariff tariff = mock(Tariff.class);
        when(tariff.getChargingBasis()).thenReturn(ChargingBasis.PER_PERSON);
        ParkTariff parkTariff = mock(ParkTariff.class);
        when(parkTariff.getTariff()).thenReturn(tariff);
        when(parkTariffRepository.findByParkIdAndTariffId(anyLong(), anyLong()))
                .thenReturn(Optional.of(parkTariff));

        /* USD 70.80 a head, the Ngorongoro conservation fee both companies actually charge. */
        ParkTariffRate rate = mock(ParkTariffRate.class);
        when(rate.getStoRate()).thenReturn(new BigDecimal("70.80"));
        when(rate.getRackRate()).thenReturn(new BigDecimal("92.04"));
        when(rate.getCurrency()).thenReturn("USD");
        when(lookup.lookupPersonRateWithIssueLogging(
                anyLong(), anyString(), anyLong(), anyString(), any(),
                anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn(ParkTariffRateLookupService.LookupResult.found(rate, "High Season"));

        calculator = new SafariParkTariffCostCalculator(
                lookup, parkTariffRepository, nationCategoryRepository, idObfuscator);
    }

    @Test
    @DisplayName("a waived fee is charged to nobody")
    void aWaivedFeeIsNotInTheCharges() {
        List<CostLineItemDTO> charged = calculator.calculateForDay(
                dayWithAChargedAndAWaivedFee(), LocalDate.of(2026, 11, 30), null, twoAdults(), 1);

        assertEquals(1, charged.size(),
                "the waived fee is still being charged: " + names(charged));
        assertTrue(charged.get(0).getItemName().startsWith("Conservation Fee"),
                "the wrong fee survived: " + names(charged));
        assertEquals(new BigDecimal("141.60"), charged.get(0).getStoTotalPrice(),
                "two adults at 70.80 is the whole of what this day charges");
    }

    @Test
    @DisplayName("the waived fee is still priced, so what was given away can be seen")
    void theWaivedFeeIsShownWithItsReason() {
        List<CostLineItemDTO> excluded = calculator.calculateExcludedForDay(
                dayWithAChargedAndAWaivedFee(), LocalDate.of(2026, 11, 30), null, twoAdults(), 1);

        assertEquals(1, excluded.size(), "expected exactly the waived fee: " + names(excluded));
        CostLineItemDTO line = excluded.get(0);

        assertEquals(ExclusionReason.WAIVED_FEE, line.getExclusionReason(),
                "a waived fee reads as 'waived', not as 'not included' -- somebody gave this up");
        assertEquals(new BigDecimal("141.60"), line.getStoTotalPrice(),
                "priced exactly as it would have been charged, or the figure means nothing");
        assertNotNull(line.getEntryId(), "without the row id no control can act on the line");
        assertEquals("waived-row", line.getEntryId());
        assertEquals("visit", line.getParentEntryId(),
                "a park tariff is addressed through its park visit, not off the day");
    }

    @Test
    @DisplayName("a fee that is both waived and excluded reads as waived")
    void waivedWinsTheLabel() {
        FullSafariDTO.DayDTO day = dayWithAChargedAndAWaivedFee();
        FullSafariDTO.ParkTariffDTO waived = day.getParks().get(0).getTariffs().get(1);
        waived.setIsIncludedInPrice(false);

        List<CostLineItemDTO> excluded = calculator.calculateExcludedForDay(
                day, LocalDate.of(2026, 11, 30), null, twoAdults(), 1);

        assertEquals(1, excluded.size(), "the line must not be reported twice");
        assertEquals(ExclusionReason.WAIVED_FEE, excluded.get(0).getExclusionReason(),
                "'not included' loses information a waiver carries: who gave the money up");
    }

    private FullSafariDTO.DayDTO dayWithAChargedAndAWaivedFee() {
        FullSafariDTO.ParkTariffDTO charged = new FullSafariDTO.ParkTariffDTO();
        charged.setId("charged-row");
        charged.setTariffId("charged-tariff");
        charged.setTariffName("Conservation Fee");
        charged.setIsIncludedInPrice(true);
        charged.setIsWaived(false);

        FullSafariDTO.ParkTariffDTO waived = new FullSafariDTO.ParkTariffDTO();
        waived.setId("waived-row");
        waived.setTariffId("waived-tariff");
        waived.setTariffName("Concession Fee");
        waived.setIsIncludedInPrice(true);
        waived.setIsWaived(true);
        waived.setWaiverReason("Comped by the park after the road closure");

        FullSafariDTO.DayParkDTO visit = new FullSafariDTO.DayParkDTO();
        visit.setId("visit");
        visit.setParkId("park");
        visit.setParkName("Ngorongoro Conservation Area");
        visit.setTariffs(List.of(charged, waived));

        FullSafariDTO.DayDTO day = new FullSafariDTO.DayDTO();
        day.setDayNumber(3);
        day.setParks(List.of(visit));
        return day;
    }

    private List<FullSafariDTO.PaxDTO> twoAdults() {
        FullSafariDTO.PaxDTO pax = new FullSafariDTO.PaxDTO();
        pax.setNationCategoryId("nation");
        pax.setNationCategoryName("Non-Resident");
        pax.setAgeCategoryId("age");
        pax.setAgeCategoryName("Adult");
        pax.setCount(2);
        return List.of(pax);
    }

    private String names(List<CostLineItemDTO> lines) {
        return lines.stream().map(CostLineItemDTO::getItemName).toList().toString();
    }
}
