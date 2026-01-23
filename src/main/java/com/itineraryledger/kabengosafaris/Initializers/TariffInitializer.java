package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * TariffInitializer - Initializes Tanzania Park Tariffs at application startup
 *
 * Order (IDs 1-12):
 * 1. Conservation Fee (park entry fee), 2. Public Camping Fee, 3. Special Camping Fee,
 * 4. Seasonal Camping Fee, 5. Fly Campsites Fee, 6. Upper Barafu Camping Fee,
 * 7. Premium Special Camping Fee, 8. Parks Accommodations Fee, 9. Rescue Fee,
 * 10. Concession Fee, 11. Hiking / Mountaineering Fee, 12. WMA
 *
 * Runs AFTER GlobalSeasonInitializer (Order = HIGHEST_PRECEDENCE + 11)
 *
 * Behavior:
 * - Only creates tariffs that don't already exist (checks by name)
 * - Does not update existing tariffs to preserve user modifications
 * - Marks all initialized tariffs as system tariffs (isSystem = true)
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 12) // Run after GlobalSeasonInitializer
public class TariffInitializer implements ApplicationRunner {

    private final TariffRepository tariffRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = true;

        try {
            initializeTariffs();
        } catch (Exception e) {
            log.error("Error during tariff initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║               TARIFF INITIALIZER - START                           ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║            ✓ TARIFF INITIALIZER - COMPLETED                        ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║            ✗ TARIFF INITIALIZER - FAILED                           ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializeTariffs() {
        log.info("Starting Tanzania Park Tariffs initialization...");

        List<Tariff> tariffsToCreate = getTanzaniaTariffs();
        int createdCount = 0;
        int skippedCount = 0;

        for (Tariff tariff : tariffsToCreate) {
            if (!tariffRepository.existsByNameIgnoreCase(tariff.getName())) {
                tariffRepository.save(tariff);
                log.info("✓ Created tariff: {}", tariff.getName());
                createdCount++;
            } else {
                log.debug("⊘ Tariff already exists: {}", tariff.getName());
                skippedCount++;
            }
        }

        log.info("");
        log.info("Tariff initialization complete: {} tariffs created, {} tariffs skipped (already exist)",
                createdCount, skippedCount);
        log.info("Total tariffs in database: {}", tariffRepository.count());
    }

    /**
     * Get list of all Tanzania park tariffs to initialize
     *
     * IMPORTANT: The order of tariffs in this initializer matches the production database
     * to ensure consistent auto-generated IDs.
     * DO NOT change the order of tariffs without updating the production database!
     *
     * Order (IDs 1-12):
     * 1. Conservation Fee (park entry fee), 2. Public Camping Fee, 3. Special Camping Fee,
     * 4. Seasonal Camping Fee, 5. Fly Campsites Fee, 6. Upper Barafu Camping Fee,
     * 7. Premium Special Camping Fee, 8. Parks Accommodations Fee, 9. Rescue Fee,
     * 10. Concession Fee, 11. Hiking / Mountaineering Fee, 12. WMA
     */
    private List<Tariff> getTanzaniaTariffs() {
        List<Tariff> tariffs = new ArrayList<>();

        // ID 1: Conservation Fee (park entry fee - formerly "Entrance" activity)
        tariffs.add(createConservationFeeTariff());

        // ID 2: Public Camping Fee
        tariffs.add(createPublicCampingFeeTariff());

        // ID 3: Special Camping Fee
        tariffs.add(createSpecialCampingFeeTariff());

        // ID 4: Seasonal Camping Fee
        tariffs.add(createSeasonalCampingFeeTariff());

        // ID 5: Fly Campsites Fee
        tariffs.add(createFlyCampsitesFeeTariff());

        // ID 6: Upper Barafu Camping Fee
        tariffs.add(createUpperBarafuCampingFeeTariff());

        // ID 7: Premium Special Camping Fee
        tariffs.add(createPremiumSpecialCampingFeeTariff());

        // ID 8: Parks Accommodations Fee
        tariffs.add(createParksAccommodationsFeeTariff());

        // ID 9: Rescue Fee
        tariffs.add(createRescueFeeTariff());

        // ID 10: Concession Fee
        tariffs.add(createConcessionFeeTariff());

        // ID 11: Hiking / Mountaineering Fee
        tariffs.add(createHikingMountaineeringFeeTariff());

        // ID 12: WMA (Wildlife Management Area)
        tariffs.add(createWmaTariff());

        return tariffs;
    }

    // =====================================================================
    // CONSERVATION FEE (ID 1) - Park Entry Fee
    // =====================================================================

    /**
     * ID 1: Conservation Fee
     * Mandatory park entry fee charged per person per day for all Tanzania national parks.
     * This is the primary fee for accessing any national park, covering park entry and
     * contributing to conservation efforts, anti-poaching operations, and park infrastructure.
     */
    private Tariff createConservationFeeTariff() {
        return Tariff.builder()
                .name("Conservation Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Park entry fee providing access to park amenities and the opportunity to experience game drives, walking safaris, and the natural scenery and wildlife. This is the mandatory conservation fee charged by TANAPA for all national park visitors.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    // =====================================================================
    // CAMPING FEES (IDs 2-7)
    // =====================================================================

    /**
     * ID 2: Public Camping Fee (was ID 1)
     * TANAPA-managed public campsites with basic shared facilities.
     */
    private Tariff createPublicCampingFeeTariff() {
        return Tariff.builder()
                .name("Public Camping Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Covers camping at designated TANAPA public campsites within national parks. These sites are managed by park authorities and provide basic shared facilities including pit latrines, designated cooking areas, and sometimes water points. Public campsites operate on a first-come, first-served basis and do not require advance booking, making them ideal for budget travelers and self-drive safaris. Sites are typically located in scenic areas near wildlife corridors. Campers must bring their own tents, sleeping gear, and cooking equipment. Park rangers conduct periodic patrols for security. Fee is charged per person per night with reduced rates for children aged 5-15 years; children under 5 are free. All fees are subject to 18% VAT as per TANAPA regulations and must be paid electronically via the Government Electronic Payment Gateway (GePG).")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 3: Special Camping Fee (was ID 2)
     * Private wilderness camping with exclusivity and no shared facilities.
     */
    private Tariff createSpecialCampingFeeTariff() {
        return Tariff.builder()
                .name("Special Camping Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Applies to designated special campsites offering exclusive private wilderness camping experiences. Unlike public campsites, special campsites are allocated to single groups only, providing complete privacy and a genuine bush experience without other campers. These sites have no permanent facilities - you pitch your tent at a secluded wilderness location with pristine views and undisturbed wildlife. Prior booking through TANAPA or authorized tour operators is mandatory, and permits must be obtained in advance. Special campsites are popular in Serengeti, Tarangire, and Nyerere National Parks where they offer intimate wildlife viewing opportunities. Mobile tented camps operated by safari companies are typically set up at these locations. Fee is charged per person per night and is higher than public camping fees to reflect the exclusivity. All rates are subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 4: Seasonal Camping Fee (was ID 3)
     * Campsites operational only during specific seasons, often in migration areas.
     */
    private Tariff createSeasonalCampingFeeTariff() {
        return Tariff.builder()
                .name("Seasonal Camping Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Charged for campsites that operate only during specific periods of the year, typically aligned with wildlife movements such as the Great Migration in Serengeti or seasonal wildlife concentrations in other parks. These temporary sites are established in remote areas that may be inaccessible during wet seasons due to flooding or poor road conditions. Seasonal campsites provide unique opportunities to camp near prime wildlife viewing locations during peak animal activity. They are particularly popular along migration routes in the Serengeti ecosystem and in parks like Katavi and Nyerere during dry season when wildlife concentrates around water sources. Advance booking is required and availability is limited to the operational season. Fee is charged per person per night with rates varying by park and season. Mobile safari operators frequently use these sites for specialized migration-following itineraries. All fees subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 5: Fly Campsites Fee (was ID 4)
     * Temporary mobile bush camps for walking safaris and authentic wilderness experiences.
     */
    private Tariff createFlyCampsitesFeeTariff() {
        return Tariff.builder()
                .name("Fly Campsites Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Covers temporary mobile fly camps set up in remote wilderness areas for authentic bush camping experiences. Fly camping involves spending one or more nights away from the main camp in a lightweight mobile setup, offering an intimate connection with nature with only a sheet of canvas between you and the African night sky. These camps are established in advance by professional staff and include essential amenities for a comfortable night - dome tents, bush shower, and safari toilet. Fly camping is typically combined with walking safaris led by armed TANAPA rangers, allowing guests to track wildlife on foot through pristine wilderness. This experience is particularly popular in Nyerere National Park (formerly Selous), Ruaha, and Katavi where vast wilderness areas support this authentic safari style. Minimum group sizes often apply, and the experience is usually available only during dry season (mid-July to November). Fly camping follows strict Leave No Trace principles to minimize environmental impact. Fee is per person per night, subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 6: Upper Barafu Camping Fee (was ID 5)
     * High-altitude camping fee for Mount Kilimanjaro summit attempts.
     */
    private Tariff createUpperBarafuCampingFeeTariff() {
        return Tariff.builder()
                .name("Upper Barafu Camping Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Specific to Mount Kilimanjaro climbing routes, this fee applies to climbers camping at high-altitude sites near the summit. Barafu Camp sits at 4,673 meters (15,331 feet) in the alpine desert zone and serves as the primary staging point for summit attempts via the Machame, Lemosho, and Umbwe routes. The nearby Kosovo high camp at 4,870 meters offers an alternative for climbers seeking a shorter summit night push. Camping at these extreme altitudes requires proper acclimatization and specialized high-altitude gear. The standard Kilimanjaro camping fee is approximately USD 50 per person per night, with crater camping commanding a premium rate of USD 100 per night for those choosing to camp at the summit. All climbing fees must be paid through TANAPA's electronic payment system, and climbers must register at park gates with valid permits. Climbing is only permitted with registered Tanzanian tour operators and licensed guides. Rescue fee is charged separately. All rates subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 7: Premium Special Camping Fee (was ID 6)
     * Exclusive luxury wilderness camping in prime locations.
     */
    private Tariff createPremiumSpecialCampingFeeTariff() {
        return Tariff.builder()
                .name("Premium Special Camping Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Applies to exclusive premium camping locations in Tanzania's most prestigious parks, particularly the Serengeti. Premium special campsites offer the ultimate in wilderness luxury - secluded sites in prime wildlife viewing areas with complete privacy and stunning landscapes. These locations are reserved for high-end mobile safari operators who establish luxury tented camps with full amenities including en-suite facilities, fine dining, and professional staff. Premium sites are strategically positioned in areas known for exceptional wildlife concentrations, such as the central Serengeti plains during migration calving season or predator-rich territories. Unlike standard special campsites, premium locations undergo additional vetting and have stricter environmental compliance requirements. Advance reservation is mandatory and often requires booking months ahead, especially for peak season dates. Fee is significantly higher than standard special camping rates, reflecting the exclusivity and prime positioning. Charged per person per night, subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    // =====================================================================
    // ACCOMMODATION & SERVICE FEES (IDs 8-10)
    // =====================================================================

    /**
     * ID 8: Parks Accommodations Fee (was ID 7)
     * Fee for TANAPA-owned accommodation facilities within parks.
     */
    private Tariff createParksAccommodationsFeeTariff() {
        return Tariff.builder()
                .name("Parks Accommodations Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Applies to overnight stays in TANAPA-owned and operated accommodation facilities within national park boundaries. These include mountain huts on Kilimanjaro and Mount Meru climbing routes, park hostels, rest houses, bandas (traditional-style cottages), and self-catering cottages at various parks. On Kilimanjaro, climbers typically stay in designated huts along routes like Marangu, which offers the only hut accommodation on the mountain. Mount Meru climbers use Miriakamba and Saddle Huts managed by Arusha National Park. These facilities provide basic to moderate comfort with beds, mattresses, and sometimes communal dining areas. Booking is required through TANAPA or authorized tour operators, particularly for popular routes and peak seasons. Hut accommodation is often preferred by climbers who want to avoid carrying camping gear. Fee is charged per person per night with different rates for various facility types. Payment is processed electronically via TANAPA's GePG system. All rates subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 9: Rescue Fee (was ID 8)
     * Mandatory mountain safety fee covering emergency evacuation services.
     */
    private Tariff createRescueFeeTariff() {
        return Tariff.builder()
                .name("Rescue Fee")
                .chargingBasis(ChargingBasis.PER_VEHICLE)
                .description("Mandatory fee for all climbers attempting Mount Kilimanjaro (5,895m) and Mount Meru (4,566m), covering emergency rescue and evacuation services within Kilimanjaro and Arusha National Parks. This fee funds park rescue teams, equipment maintenance, communication systems, and emergency response infrastructure. Rescue services include stretcher evacuations by trained park rangers, emergency medical assistance at ranger stations, and coordination with external medical evacuation services when required. The fee is approximately USD 20 per climber per trip and is included in the overall park permit structure. While this basic rescue fee covers park-based rescue operations, helicopter evacuations and hospital treatment costs depend on the climber's personal travel/medical insurance policy - comprehensive travel insurance with high-altitude evacuation coverage is strongly recommended for all mountain climbers. Park rangers are stationed at camps along climbing routes and are equipped with radios for emergency communication. Registration at park gates includes emergency contact information. Fee is charged per climbing trip regardless of duration.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 10: Concession Fee (was ID 9)
     * Overnight fee for guests staying at lodges and camps within park boundaries.
     */
    private Tariff createConcessionFeeTariff() {
        return Tariff.builder()
                .name("Concession Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Charged to visitors staying overnight at lodges, tented camps, and hotels located within national park or conservation area boundaries. This fee is distinct from park entry/conservation fees and applies specifically to overnight accommodation guests. Concession fees are paid by lodges and camps to TANAPA or NCAA (Ngorongoro Conservation Area Authority) for the privilege of operating within protected areas, and this cost is passed on to guests. For Serengeti National Park, the concession fee is approximately USD 60-70 per adult per night during peak season, with reduced rates for children aged 5-15 and during low season (October-June). This fee is in addition to the daily park conservation fee, so guests staying inside parks pay both fees. Some lodges include concession fees in their quoted rates while others list them separately - always confirm what's included when booking. Since 2017, these fees are typically paid directly at park gates rather than through accommodation providers. Revenue supports infrastructure maintenance, anti-poaching operations, and community development programs. All rates subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    // =====================================================================
    // ACTIVITY FEES (IDs 11-12)
    // =====================================================================

    /**
     * ID 11: Hiking / Mountaineering Fee (was ID 10)
     * Fee for guided hiking and climbing activities in protected areas.
     */
    private Tariff createHikingMountaineeringFeeTariff() {
        return Tariff.builder()
                .name("Hiking / Mountaineering Fee")
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Applies to individuals participating in guided hiking, trekking, and mountain climbing activities within Tanzania's protected areas. This fee supports trail maintenance, ranger services, safety infrastructure, and environmental conservation along hiking routes. In Tanzania, independent hiking is prohibited in national parks - all hikers must be accompanied by licensed guides and, in wildlife areas, armed TANAPA rangers for safety. This requirement covers Mount Kilimanjaro and Mount Meru climbs, walking safaris, and nature trails throughout the national park system. The fee helps fund habitat restoration along popular trails, construction and maintenance of bridges and steps, signage, and ranger patrol stations. For Mount Meru in Arusha National Park, guides accompany all groups because routes pass through zones with free-roaming wildlife including buffalo and elephant. All payments must be made electronically through TANAPA's Government Electronic Payment Gateway (GePG) - cash is not accepted at park gates. Permits are issued at park gates following registration and safety briefings. Fee is charged per person per day or per trip depending on the activity. All rates subject to 18% VAT.")
                .isActive(true)
                .isSystem(true)
                .build();
    }

    /**
     * ID 12: WMA (Wildlife Management Area) (was ID 11)
     * Community conservation area access and bed night levy fees.
     */
    private Tariff createWmaTariff() {
        return Tariff.builder()
                .name("WMA")
                .chargingBasis(ChargingBasis.PER_GROUP)
                .description("Fee for accessing Wildlife Management Areas (WMAs) - community-owned and managed conservation lands that serve as vital buffer zones and wildlife corridors between and around Tanzania's national parks. WMAs represent a groundbreaking community-based conservation model where local villages set aside traditional lands for wildlife protection and sustainable tourism. Currently, 19 established WMAs have added approximately 3% to Tanzania's total protected land area, with more areas pending gazettement. WMA fees typically range from USD 12-30 per person per day for conservation fees, plus approximately USD 17.70 per night bed levy for lodge guests. These funds flow directly to local communities, creating economic incentives for conservation and providing income that discourages poaching and habitat destruction. Community members serve as scouts patrolling for poaching and encroachment. Notable WMAs include Burunge, Randilen, and Enduimet (near Kilimanjaro National Park), Lake Natron area, and community lands surrounding Serengeti. For lodges in WMAs, the bed night levy/concession fee is typically incorporated into accommodation bills. WMAs offer excellent wildlife viewing and cultural experiences while directly supporting community livelihoods and conservation.")
                .isActive(true)
                .isSystem(true)
                .build();
    }
}
