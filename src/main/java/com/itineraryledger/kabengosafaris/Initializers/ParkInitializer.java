package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.Park.ParkType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ParkInitializer - Initializes Tanzania National Parks at application startup
 *
 * This initializer creates default Tanzania parks with accurate data including:
 * - Northern Circuit: Serengeti, Ngorongoro, Tarangire, Lake Manyara, Arusha, Kilimanjaro
 * - Southern Circuit: Nyerere, Ruaha, Mikumi, Udzungwa
 * - Western Circuit: Katavi, Mahale, Gombe, Rubondo
 * - Coastal & Marine: Saadani, Mafia Island, Mnazi Bay
 * - Additional: Mkomazi
 *
 * Runs AFTER RoleInitializer (Order = HIGHEST_PRECEDENCE + 6)
 *
 * Behavior:
 * - Only creates parks that don't already exist (checks by name)
 * - Does not update existing parks to preserve user modifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 6) // Run after all other initializers
public class ParkInitializer implements ApplicationRunner {

    private final ParkRepository parkRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = false;

        try {
            initializeParks();
            success = true;
        } catch (Exception e) {
            log.error("Error during park initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    /**
     * Print start banner for park initialization
     */
    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║                 PARK INITIALIZER - START                           ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    /**
     * Print end banner for park initialization
     */
    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║             ✓ PARK INITIALIZER - COMPLETED                         ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║             ✗ PARK INITIALIZER - FAILED                            ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    /**
     * Initialize all Tanzania parks
     */
    private void initializeParks() {
        log.info("Starting Tanzania Parks initialization...");

        List<Park> parksToCreate = getTanzaniaParks();
        int createdCount = 0;
        int skippedCount = 0;

        for (Park park : parksToCreate) {
            if (!parkRepository.existsByName(park.getName())) {
                parkRepository.save(park);
                log.info("Created park: {}", park.getName());
                createdCount++;
            } else {
                log.debug("⊘ Park already exists, skipping: {}", park.getName());
                skippedCount++;
            }
        }

        log.info("Park initialization complete: {} parks created, {} parks skipped (already exist)",
                createdCount, skippedCount);
        log.info("Total parks in database: {}", parkRepository.count());
    }

    /**
     * Get list of all Tanzania parks to initialize
     *
     * IMPORTANT: The order of parks in this initializer matches the production database
     * (idealafr_idealafricantravels.sql) to ensure consistent auto-generated IDs.
     * DO NOT change the order of parks without updating the production database!
     *
     * Order (IDs 1-23):
     * 1. Serengeti, 2. Ngorongoro, 3. Tarangire, 4. Lake Manyara, 5. Ruaha,
     * 6. Mikumi, 7. Katavi, 8. Selous, 9. Arusha, 10. Gombe,
     * 11. Burigi-Chato, 12. Kilimanjaro, 13. Mahale, 14. Mkomazi, 15. Rubondo,
     * 16. Saadani, 17. Udzungwa, 18. Saanane, 19. Ibanda-Kyerwa, 20. Kitulo,
     * 21. Nyerere, 22. Rumanyika-Karagwe, 23. Ugalla River
     */
    private List<Park> getTanzaniaParks() {
        List<Park> parks = new ArrayList<>();

        // ID 1: Serengeti
        parks.add(createSerengetiPark());

        // ID 2: Ngorongoro
        parks.add(createNgorongoroPark());

        // ID 3: Tarangire
        parks.add(createTarangirePark());

        // ID 4: Lake Manyara
        parks.add(createLakeManyaraPark());

        // ID 5: Ruaha
        parks.add(createRuahaPark());

        // ID 6: Mikumi
        parks.add(createMikumiPark());

        // ID 7: Katavi
        parks.add(createKataviPark());

        // ID 8: Selous Game Reserve
        parks.add(createSelousGameReserve());

        // ID 9: Arusha
        parks.add(createArushaPark());

        // ID 10: Gombe
        parks.add(createGombePark());

        // ID 11: Burigi-Chato
        parks.add(createBurigiChatoPark());

        // ID 12: Kilimanjaro
        parks.add(createKilimanjaroPark());

        // ID 13: Mahale
        parks.add(createMahalePark());

        // ID 14: Mkomazi
        parks.add(createMkomaziPark());

        // ID 15: Rubondo Island
        parks.add(createRubondoPark());

        // ID 16: Saadani
        parks.add(createSaadaniPark());

        // ID 17: Udzungwa Mountains
        parks.add(createUdzungwaPark());

        // ID 18: Saanane
        parks.add(createSaananePark());

        // ID 19: Ibanda-Kyerwa
        parks.add(createIbandaKyerwaPark());

        // ID 20: Kitulo
        parks.add(createKituloPark());

        // ID 21: Nyerere
        parks.add(createNyererePark());

        // ID 22: Rumanyika-Karagwe
        parks.add(createRumanyikaKaragwePark());

        // ID 23: Ugalla River
        parks.add(createUgallaRiverPark());

        // ID 24: Mafia Marine Park
        parks.add(createMafiaMarinePark());

        // ID 25: Mnazi Ruvuma Park
        parks.add(createMnaziRuvumaPark());

        return parks;
    }

    // =====================================================================
    // NORTHERN CIRCUIT PARKS
    // =====================================================================

    private Park createSerengetiPark() {
        return Park.builder()
                .name("Serengeti National Park")
                .slug("serengeti-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Mara and Simiyu")
                .district("Serengeti")
                .location("Northern Tanzania, 335 km northwest of Arusha")
                .latitude(new BigDecimal("-2.3333"))
                .longitude(new BigDecimal("34.8333"))
                .elevation("920-1,850 meters")
                .size("14,763 km²")
                .shortDescription("One of the most famous wildlife sanctuaries in the world, known for the annual Great Migration of over 1.5 million wildebeest and 250,000 zebras.")
                .fullDescription("Serengeti National Park is a vast ecosystem in north-central Tanzania. Established in 1951, it covers 14,763 square kilometers of grassland plains, savanna, riverine forest, and woodlands. The park is divided into three regions: Serengeti Plains, Western Corridor, and Northern Serengeti. It borders the Maasai Mara National Reserve in Kenya to the north. The park is home to the largest lion population in Africa and hosts the spectacular Great Migration, one of the most impressive natural events on Earth. The name \"Serengeti\" comes from the Maasai language and means \"endless plains\".")
                .history("Established in 1951 and designated as a UNESCO World Heritage Site in 1981. The area was first protected as a game reserve in 1929 before being upgraded to national park status.")
                .ecosystem("Grassland savanna, acacia woodlands, riverine forests, and open plains. The ecosystem supports diverse habitats from short grass plains to kopjes (rocky outcrops).")
                .wildlife("Home to the Big Five (lion, leopard, elephant, buffalo, rhinoceros) and hosts the Great Migration of over 2 million wildebeest, zebras, and gazelles. The park has Africa's largest lion population with over 3,000 lions. Other species include cheetahs, hyenas, giraffes, hippos, crocodiles, and over 500 bird species.")
                .vegetation("Grassland plains dominate with scattered acacia trees, particularly umbrella acacias. Western corridor features riverine forests along the Grumeti River. Kopjes support unique plant communities.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season for wildlife viewing), January to February (calving season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by road from Arusha (8-9 hours) or by air to Seronera, Kogatende, or Lobo airstrips. Multiple entry gates including Naabi Hill, Ndabaka, and Ikoma.")
                .tags("Big Five, Great Migration, Safari, Wildlife, UNESCO World Heritage, Lion, Wildebeest, Zebra, Kopjes, Savanna")
                .isActive(true)
                .build();
    }

    private Park createNgorongoroPark() {
        return Park.builder()
                .name("Ngorongoro Conservation Area")
                .slug("ngorongoro-conservation-area")
                .parkType(ParkType.CONSERVATION_AREA)
                .region("Arusha")
                .district("Ngorongoro")
                .location("Crater Highlands, 180 km west of Arusha City")
                .latitude(new BigDecimal("-3.2000"))
                .longitude(new BigDecimal("35.4500"))
                .elevation("1,030-3,648 meters (Loolmalasin peak)")
                .size("8,292 km²")
                .shortDescription("Home to the world's largest intact volcanic caldera and one of Africa's most spectacular wildlife viewing areas with the highest density of predators.")
                .fullDescription("The Ngorongoro Conservation Area is a protected area and UNESCO World Heritage Site located in the Crater Highlands of northeastern Tanzania. The area is named after Ngorongoro Crater, a large volcanic caldera formed when a giant volcano exploded and collapsed 2-3 million years ago. The crater floor, 600 meters deep and covering 260 square kilometers, is home to approximately 25,000 large animals. The conservation area also includes Olduvai Gorge, one of the most important paleoanthropological sites in the world. Unlike national parks, the Ngorongoro Conservation Area allows human habitation, with Maasai pastoralists living alongside wildlife.")
                .history("Designated as a UNESCO World Heritage Site in 1979 and as an International Biosphere Reserve in 1981. The area was originally part of Serengeti National Park until it was separated in 1959 to create a multiple land use area.")
                .ecosystem("Highland plains, savanna, savanna woodlands, forests, and the volcanic crater ecosystem. Includes soda lakes, swamps, and montane forests on crater rims.")
                .wildlife("The crater hosts approximately 25,000 large mammals including the Big Five. It has the densest known population of lions in the world. The area is one of the few places in Africa where you can see the endangered black rhinoceros. Other species include elephants, buffaloes, hippos, wildebeest, zebras, elands, gazelles, and over 500 bird species including flamingos on Lake Magadi.")
                .vegetation("Ranges from short grass plains on the crater floor to montane forests on the crater rim. Includes fever tree forests, highland grasslands, and diverse plant communities supporting various ecological niches.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season), December to March (green season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by road from Arusha (3-4 hours). Main entrance at Lodoare Gate. Can be combined with Serengeti visits via Naabi Hill Gate.")
                .tags("Crater, UNESCO World Heritage, Big Five, Black Rhino, Maasai Culture, Olduvai Gorge, Volcanic Caldera, Wildlife Dense")
                .isActive(true)
                .build();
    }

    private Park createTarangirePark() {
        return Park.builder()
                .name("Tarangire National Park")
                .slug("tarangire-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Manyara")
                .district("Babati")
                .location("Northern Tanzania, 118 km southwest of Arusha")
                .latitude(new BigDecimal("-3.8333"))
                .longitude(new BigDecimal("36.0000"))
                .elevation("900-1,250 meters")
                .size("2,850 km²")
                .shortDescription("Famous for its huge elephant population, iconic baobab trees, and spectacular concentration of wildlife during the dry season.")
                .fullDescription("Tarangire National Park is Tanzania's sixth-largest national park, covering 2,850 square kilometers. Named after the Tarangire River that flows through the park, it serves as a vital water source during the dry season, attracting massive concentrations of wildlife. The park is renowned for having one of the highest elephant populations in Tanzania, with herds of up to 300 elephants common during the dry season. Ancient baobab trees dot the landscape, some over 1,000 years old. During the dry season (June-October), Tarangire has wildlife densities second only to Ngorongoro Crater.")
                .history("Established as a national park in 1970. The area has been important to wildlife migration patterns for millennia, particularly for elephants moving between seasonal water sources.")
                .ecosystem("Acacia woodlands, riverine forests, seasonal swamps, open grasslands, and ancient baobab-studded landscapes. The Tarangire River and Silale Swamp are key ecosystem features.")
                .wildlife("Home to large elephant herds (up to 3,000 individuals), lions, leopards, cheetahs, and over 550 bird species making it a paradise for bird watchers. Other mammals include buffaloes, giraffes, wildebeest, zebras, impalas, elands, fringe-eared oryx, and pythons in the swamps.")
                .vegetation("Dominated by ancient baobab trees, umbrella acacias, and sausage trees. Swamp areas feature dense vegetation including palms and tall grasses. Open plains with scattered acacias.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season - best for wildlife concentration)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by road from Arusha (2 hours). Main entrance at the northern gate. Good tarmac road to the park entrance.")
                .tags("Elephants, Baobab Trees, Bird Watching, Wildlife, Dry Season Safari, Tarangire River, Migration, Big Cats")
                .isActive(true)
                .build();
    }

    private Park createLakeManyaraPark() {
        return Park.builder()
                .name("Lake Manyara National Park")
                .slug("lake-manyara-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Manyara")
                .district("Monduli")
                .location("Northern Tanzania, 126 km west of Arusha, near Great Rift Valley escarpment")
                .latitude(new BigDecimal("-3.5333"))
                .longitude(new BigDecimal("35.8333"))
                .elevation("960-1,828 meters")
                .size("330 km² (including 230 km² of lake surface)")
                .shortDescription("Famous for its tree-climbing lions, large flocks of flamingos, and stunning location at the base of the Great Rift Valley escarpment.")
                .fullDescription("Lake Manyara National Park is a compact gem located between Lake Manyara and the Great Rift Valley escarpment. Despite its small size, it hosts remarkable biodiversity. The park is famous for its unusual tree-climbing lions, which lounge in acacia branches during the heat of the day. The alkaline lake attracts thousands of flamingos, creating spectacular pink shores during certain seasons. The groundwater forest at the park entrance is one of the few in East Africa.")
                .history("Established as a national park in 1960. The area was previously a hunting ground and gained protection due to its unique wildlife and ecological importance.")
                .ecosystem("Diverse ecosystems including groundwater forest, acacia woodland, open grassland, swamps, and the alkaline lake. The Great Rift Valley escarpment forms a dramatic backdrop.")
                .wildlife("Tree-climbing lions are the park's most famous residents. Large elephant herds, buffaloes, giraffes, zebras, wildebeest, hippos, and a remarkable variety of primates including baboons and blue monkeys. Over 400 bird species recorded.")
                .vegetation("Groundwater forest with mahogany and wild mango trees at the park entrance. Acacia woodlands dominate the drier areas. Lush vegetation supported by underground water sources from the escarpment.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round destination; July to October and January to February best for wildlife")
                .openingHours("6:30 AM - 6:00 PM daily")
                .accessInformation("Accessible by road from Arusha (2 hours). Located on the main route to Serengeti and Ngorongoro. Good tarmac road to park entrance.")
                .tags("Tree-climbing Lions, Flamingos, Great Rift Valley, Bird Watching, Groundwater Forest, Elephants, Diverse Ecosystems")
                .isActive(true)
                .build();
    }

    private Park createArushaPark() {
        return Park.builder()
                .name("Arusha National Park")
                .slug("arusha-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Arusha")
                .district("Arusha")
                .location("Northern Tanzania, 32 km northeast of Arusha City")
                .latitude(new BigDecimal("-3.2500"))
                .longitude(new BigDecimal("36.8333"))
                .elevation("1,500-4,566 meters (Mount Meru summit)")
                .size("137 km²")
                .shortDescription("Home to Mount Meru (4,566m), Tanzania's second-highest peak, featuring stunning landscapes, diverse wildlife, and unique walking safari opportunities.")
                .fullDescription("Arusha National Park, though small at 137 square kilometers, offers incredible diversity within close proximity to Arusha city. The park is dominated by Mount Meru, an active volcano and Tanzania's second-highest mountain at 4,566 meters. The park features three distinct areas: Ngurdoto Crater, the Momella Lakes, and Mount Meru. It is one of the few parks in Tanzania where walking safaris are permitted.")
                .history("Established as a national park in 1960, initially as Ngurdoto Crater National Park. Expanded in 1967 to include Mount Meru and renamed Arusha National Park.")
                .ecosystem("Diverse habitats including montane forest, highland moorland, alpine desert, crater floor, alkaline lakes, and open grassland.")
                .wildlife("Black and white colobus monkeys, blue monkeys, elephants, buffaloes, giraffes, leopards, spotted hyenas, and the rare red duiker. Over 400 bird species.")
                .vegetation("Montane forest with tall trees on Mount Meru slopes. Highland moorland and alpine vegetation at higher elevations. Grasslands and swamps around Momella Lakes.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; June to February best for Mount Meru trekking")
                .openingHours("6:30 AM - 6:30 PM daily")
                .accessInformation("Accessible by road from Arusha (45 minutes). Multiple entry gates including Momella Gate and Ngongongare Gate.")
                .tags("Mount Meru, Trekking, Colobus Monkeys, Walking Safari, Ngurdoto Crater, Momella Lakes, Bird Watching, Alpine")
                .isActive(true)
                .build();
    }

    private Park createKilimanjaroPark() {
        return Park.builder()
                .name("Kilimanjaro National Park")
                .slug("kilimanjaro-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Kilimanjaro")
                .district("Moshi Rural")
                .location("Northern Tanzania, near Kenyan border, 128 km from Arusha")
                .latitude(new BigDecimal("-3.0674"))
                .longitude(new BigDecimal("37.3556"))
                .elevation("1,800-5,895 meters (Uhuru Peak)")
                .size("1,668 km²")
                .shortDescription("Home to Mount Kilimanjaro, Africa's highest mountain at 5,895 meters, and the world's tallest free-standing mountain.")
                .fullDescription("Kilimanjaro National Park was established to protect Mount Kilimanjaro, Africa's highest peak at 5,895 meters and the world's tallest free-standing mountain. The mountain features five distinct climatic zones from cultivated farmland through rainforest, heath and moorland, alpine desert, to the arctic summit zone with glaciers. Over 35,000 people attempt to reach Uhuru Peak annually. UNESCO World Heritage Site.")
                .history("Established as a national park in 1973 and designated a UNESCO World Heritage Site in 1987. First summited by Hans Meyer and Ludwig Purtscheller in 1889.")
                .ecosystem("Five distinct vegetation zones creating unique habitats from lowland to montane zones.")
                .wildlife("Elephants, buffaloes, leopards, bushbucks, red duikers, and various primates in lower forests. Over 140 bird species.")
                .vegetation("Montane forest, heath zone with giant heathers, moorland with tussock grasses, alpine zone with giant groundsels and lobelias, glaciers at summit.")
                .primaryImage(null)
                .bestTimeToVisit("January to March and June to October (dry seasons for climbing)")
                .openingHours("24 hours (climbers register during day)")
                .accessInformation("Main gate at Marangu. Other gates: Machame, Londorossi, Lemosho, Rongai, Umbwe. Accessible from Moshi (1 hour).")
                .tags("Mount Kilimanjaro, Uhuru Peak, Trekking, UNESCO World Heritage, Africa Highest Mountain, Glaciers, Alpine, Seven Summits")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // SOUTHERN CIRCUIT PARKS (Simplified - add remaining methods similarly)
    // =====================================================================

    private Park createNyererePark() {
        return Park.builder()
                .name("Nyerere National Park")
                .slug("nyerere-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Morogoro, Ruvuma, and Lindi")
                .district("Liwale")
                .location("Southern Tanzania, 219 km from Dar es Salaam")
                .latitude(new BigDecimal("-7.6667"))
                .longitude(new BigDecimal("37.6167"))
                .elevation("100-1,300 meters")
                .size("30,893 km²")
                .shortDescription("Africa's largest game reserve and UNESCO World Heritage Site, renamed in 2019 to honor President Julius Nyerere.")
                .fullDescription("Nyerere National Park, created in 2019 from the northern portion of the Selous Game Reserve, is Tanzania's largest national park at 30,893 square kilometers. The Rufiji River flows through creating a network of channels, lakes, and swamps. UNESCO World Heritage Site with relatively few tourists.")
                .history("Originally established as a game reserve in 1905. Designated UNESCO World Heritage Site in 1982. Northern sector declared Nyerere National Park in 2019.")
                .ecosystem("Miombo woodland, riverine forests, open grasslands, seasonal swamps, and the Rufiji River system.")
                .wildlife("Over 15,000 elephants, lions, leopards, wild dogs, buffaloes, hippos, crocodiles. Tanzania's largest population of endangered African wild dogs. Over 440 bird species.")
                .vegetation("Miombo woodland, borassus palms along rivers, grasslands, and riverine forests with mahogany and ebony trees.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by air to Mtemere, Kiba, or Siwandu airstrips. Road from Dar es Salaam (5-6 hours).")
                .tags("UNESCO World Heritage, Rufiji River, Wild Dogs, Wilderness, Boat Safari, Elephants, Remote, Nyerere")
                .isActive(true)
                .build();
    }

    private Park createRuahaPark() {
        return Park.builder()
                .name("Ruaha National Park")
                .slug("ruaha-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Iringa and Dodoma")
                .district("Iringa")
                .location("Central-southern Tanzania, 130 km from Iringa")
                .latitude(new BigDecimal("-7.9000"))
                .longitude(new BigDecimal("34.8333"))
                .elevation("750-1,868 meters")
                .size("20,226 km²")
                .shortDescription("Tanzania's largest national park, featuring the Great Ruaha River and one of Africa's largest elephant populations.")
                .fullDescription("Ruaha National Park is Tanzania's largest national park at 20,226 square kilometers. The park is famous for its large elephant population (over 12,000) and has one of the highest concentrations of elephants in East Africa. Renowned for predators, particularly lions. Remote and uncrowded.")
                .history("Originally part of Rungwa Game Reserve in 1910. Upgraded to national park in 1964. Expanded in 2008.")
                .ecosystem("Miombo woodland, acacia savanna, riverine forests, baobab-studded plains, and the Great Ruaha River valley.")
                .wildlife("Over 12,000 elephants, 30,000+ buffaloes, 10% of Africa's lions, leopards, cheetahs, wild dogs. Over 571 bird species.")
                .vegetation("Miombo woodland, ancient baobabs, acacia species, riverine forests, and Commiphora-Combretum woodlands.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by air to Msembe airstrip. Road from Iringa (4-5 hours on rough roads).")
                .tags("Elephants, Lions, Great Ruaha River, Remote, Wild Dogs, Baobabs, Predators, Wilderness")
                .isActive(true)
                .build();
    }

    private Park createMikumiPark() {
        return Park.builder()
                .name("Mikumi National Park")
                .slug("mikumi-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Morogoro")
                .district("Kilosa")
                .location("Southern-central Tanzania, 283 km west of Dar es Salaam")
                .latitude(new BigDecimal("-7.3833"))
                .longitude(new BigDecimal("36.9000"))
                .elevation("550-1,250 meters")
                .size("3,230 km²")
                .shortDescription("The fourth-largest national park, known for reliable wildlife viewing along the main Dar es Salaam-Iringa highway.")
                .fullDescription("Mikumi National Park covers 3,230 square kilometers. Most accessible southern park, 4-5 hours from Dar es Salaam. The Mkata River floodplain is compared to the Serengeti. Excellent year-round wildlife viewing.")
                .history("Established as a national park in 1964. Originally part of the Selous ecosystem.")
                .ecosystem("Open grassland plains, miombo woodland, acacia savanna, and riverine forests.")
                .wildlife("Elephants, buffaloes, giraffes, zebras, wildebeest, lions, leopards, wild dogs. Over 400 bird species.")
                .vegetation("Mkata floodplain grasslands, miombo woodland, acacia trees, borassus palms, and riverine forests.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; June to October best for wildlife")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Main gate from Dar es Salaam (4-5 hours) via Morogoro. Park bisected by Dar-Iringa highway.")
                .tags("Accessible, Mkata Floodplain, Elephants, Lions, Bird Watching, Uluguru Mountains")
                .isActive(true)
                .build();
    }

    private Park createUdzungwaPark() {
        return Park.builder()
                .name("Udzungwa Mountains National Park")
                .slug("udzungwa-mountains-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Morogoro")
                .district("Kilolo")
                .location("Southern-central Tanzania, 350 km southwest of Dar es Salaam")
                .latitude(new BigDecimal("-7.7500"))
                .longitude(new BigDecimal("36.5000"))
                .elevation("250-2,576 meters (Luhombero Peak)")
                .size("1,990 km²")
                .shortDescription("A biodiversity hotspot featuring pristine rainforest, endemic primates, spectacular waterfalls, and excellent hiking.")
                .fullDescription("Udzungwa Mountains National Park protects 1,990 square kilometers of Eastern Arc Mountains. Unique as it has no roads - explored only on foot. Endemic species including Iringa red colobus and Sanje mangabey. Sanje Waterfall plunges 170 meters.")
                .history("Gazetted as national park in 1992. Previously a forest reserve.")
                .ecosystem("Tropical rainforest, montane forest, miombo woodland, grassland plateaus, streams, and waterfalls.")
                .wildlife("Six primate species including endemic Iringa red colobus and Sanje mangabey. Forest elephants, buffaloes. Over 400 bird species.")
                .vegetation("Pristine tropical rainforest, montane forest, highland grasslands, rich understory vegetation.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; June to October for hiking")
                .openingHours("7:00 AM - 5:00 PM daily")
                .accessInformation("Main entrance at Mang'ula village, 65 km from Mikumi or 350 km from Dar es Salaam.")
                .tags("Hiking, Waterfalls, Endemic Species, Primates, Rainforest, Biodiversity Hotspot, Eastern Arc")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // WESTERN CIRCUIT PARKS
    // =====================================================================

    private Park createKataviPark() {
        return Park.builder()
                .name("Katavi National Park")
                .slug("katavi-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Katavi")
                .district("Mpanda")
                .location("Western Tanzania, 40 km south of Mpanda")
                .latitude(new BigDecimal("-6.9167"))
                .longitude(new BigDecimal("31.2000"))
                .elevation("900-1,200 meters")
                .size("4,471 km²")
                .shortDescription("One of Tanzania's most remote wilderness areas, famous for huge buffalo herds and hippo concentrations.")
                .fullDescription("Katavi National Park is Tanzania's third-largest park and one of its most remote. Famous for highest concentrations of hippos and crocodiles in Africa. Buffalo herds can number in the thousands. Remote with more lions than tourists.")
                .history("Established as national park in 1974. Named after spirit Katabi believed to have lived in tamarind tree near Lake Katavi.")
                .ecosystem("Katuma River floodplains, seasonal lakes, miombo woodland, riverine forests, open grasslands.")
                .wildlife("Huge buffalo herds (1,000+), densest hippo/crocodile concentrations, elephants, lions, leopards, giraffes. Over 400 bird species.")
                .vegetation("Miombo woodland, floodplain grasslands, palm-fringed pools, riverine forests.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by air to Ikuu airstrip. Road access difficult, 40 km from Mpanda.")
                .tags("Remote, Wilderness, Buffalo Herds, Hippos, Crocodiles, Western Circuit, Unspoiled")
                .isActive(true)
                .build();
    }

    private Park createMahalePark() {
        return Park.builder()
                .name("Mahale Mountains National Park")
                .slug("mahale-mountains-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Kigoma")
                .district("Kigoma Rural")
                .location("Western Tanzania, Lake Tanganyika shore, 120 km south of Kigoma")
                .latitude(new BigDecimal("-6.2500"))
                .longitude(new BigDecimal("29.9167"))
                .elevation("772-2,462 meters (Mount Nkungwe)")
                .size("1,613 km²")
                .shortDescription("A remote paradise on Lake Tanganyika, home to wild chimpanzees, accessible only by boat.")
                .fullDescription("Mahale Mountains National Park protects 1,613 square kilometers on Lake Tanganyika shores. Home to approximately 1,000 wild chimpanzees with one habituated group. Completely remote with no roads - access only by boat. Crystal-clear waters offer excellent snorkeling.")
                .history("Established as national park in 1985. Japanese primatologist Dr. Toshisada Nishida began chimpanzee research in 1960s.")
                .ecosystem("Montane forest, lowland rainforest, miombo woodland, grassland, bamboo forests, Lake Tanganyika shoreline.")
                .wildlife("1,000 wild chimpanzees, eight other primate species, leopards, bushbucks. Over 355 bird species. Lake has 250+ endemic fish species.")
                .vegetation("Montane evergreen forest, lowland rainforest, miombo woodland, bamboo at higher elevations.")
                .primaryImage(null)
                .bestTimeToVisit("May to October (dry season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible only by boat or flight. 2-3 hour boat from Kigoma. No roads.")
                .tags("Chimpanzees, Lake Tanganyika, Remote, Primate Trekking, Snorkeling, Mountains, Western Circuit")
                .isActive(true)
                .build();
    }

    private Park createGombePark() {
        return Park.builder()
                .name("Gombe Stream National Park")
                .slug("gombe-stream-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Kigoma")
                .district("Kigoma Rural")
                .location("Western Tanzania, Lake Tanganyika shore, 16 km north of Kigoma")
                .latitude(new BigDecimal("-4.6667"))
                .longitude(new BigDecimal("29.6167"))
                .elevation("772-1,500 meters")
                .size("52 km²")
                .shortDescription("Tanzania's smallest park, world-famous as Jane Goodall's chimpanzee research site since 1960.")
                .fullDescription("Gombe Stream is Tanzania's smallest park at 52 square kilometers but globally renowned as Dr. Jane Goodall's chimpanzee research site since 1960 - the world's longest-running wildlife research project. About 100 habituated chimpanzees live in Gombe.")
                .history("Established as game reserve in 1943, upgraded to national park in 1968. Jane Goodall arrived in 1960.")
                .ecosystem("Lowland evergreen forest in valleys, miombo woodland on ridges, grassland plateaus, Lake Tanganyika shoreline.")
                .wildlife("100 chimpanzees with habituated groups, red-tailed monkeys, red colobus, blue monkeys, olive baboons, bushbucks. Over 200 bird species.")
                .vegetation("Gallery forest in valleys, miombo woodland on slopes, grasslands on plateaus.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; June to October best for chimpanzees")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible only by boat from Kigoma (30 minutes to 1 hour). No road access.")
                .tags("Chimpanzees, Jane Goodall, Research, Lake Tanganyika, Primates, Western Circuit, Small Park")
                .isActive(true)
                .build();
    }

    private Park createRubondoPark() {
        return Park.builder()
                .name("Rubondo Island National Park")
                .slug("rubondo-island-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Geita")
                .district("Biharamulo")
                .location("Northwestern Tanzania, island in Lake Victoria, west of Mwanza")
                .latitude(new BigDecimal("-2.3000"))
                .longitude(new BigDecimal("31.8333"))
                .elevation("1,134-1,486 meters")
                .size("457 km²")
                .shortDescription("A pristine island in Lake Victoria, offering unique forest-lake ecosystems with introduced chimps and rare sitatunga.")
                .fullDescription("Rubondo Island National Park comprises islands in Lake Victoria. 90% covered in dense forest. Unique as successful wildlife introduction site with chimpanzees introduced in 1960s. One of few places to observe wild chimpanzees and spot sitatunga. Over 400 bird species.")
                .history("Declared national park in 1977. Wildlife introduction site starting in 1960s under Frankfurt Zoological Society.")
                .ecosystem("Closed-canopy tropical forest, papyrus wetlands, savanna, rocky shores, sandy beaches, Lake Victoria waters.")
                .wildlife("Introduced chimpanzees, elephants, giraffes, suni, roan antelopes. Indigenous sitatunga, hippos, crocodiles, otters, bushbucks. Over 400 bird species.")
                .vegetation("Dense tropical forest with mahogany and fig trees, papyrus swamps, open grasslands, aquatic vegetation.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; June to October for chimpanzees and fishing")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by boat from Nkome (3.5 hours) or Muganza (1 hour). Fly to Rubondo airstrip.")
                .tags("Island, Lake Victoria, Chimpanzees, Sitatunga, Bird Watching, Fishing, Remote, Forest")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // COASTAL & MARINE PARKS
    // =====================================================================

    private Park createSaadaniPark() {
        return Park.builder()
                .name("Saadani National Park")
                .slug("saadani-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Pwani")
                .district("Bagamoyo and Pangani")
                .location("Coastal Tanzania, 45 km north of Bagamoyo, 100 km north of Dar es Salaam")
                .latitude(new BigDecimal("-6.0000"))
                .longitude(new BigDecimal("38.7667"))
                .elevation("0-200 meters")
                .size("1,062 km²")
                .shortDescription("Tanzania's only wildlife sanctuary bordering the Indian Ocean, combining beach and bush safari.")
                .fullDescription("Saadani National Park is unique as Tanzania's only coastal wildlife sanctuary where beach meets bush. Covers 1,062 square kilometers with 30 kilometers of Indian Ocean coastline. Hosts four of the Big Five. Beaches are important green turtle nesting sites. Historic Saadani village was once a 19th-century trading port.")
                .history("Gazetted as national park in 2005, combining Saadani Game Reserve, Mkwaja Ranch, Wami River, and Zaraninge Forest.")
                .ecosystem("Beaches, mangrove forests, salt flats, acacia woodlands, riverine forests, grasslands, Wami River system.")
                .wildlife("Four of Big Five: elephants, lions, leopards, buffaloes. Giraffes, wildebeest, hippos, crocodiles, green turtles. Over 300 bird species.")
                .vegetation("Coastal mangrove forests, acacia woodlands, palm trees, riverine vegetation, coastal dune vegetation.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; June to October for wildlife; November to February for turtles")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by road from Dar es Salaam (2.5-3 hours). Also by boat along coast or Wami River.")
                .tags("Beach, Coastal, Ocean, Unique, Wami River, Turtles, Boat Safari, Historic, Beach Safari")
                .isActive(true)
                .build();
    }

    private Park createMafiaMarinePark() {
        return Park.builder()
                .name("Mafia Island Marine Park")
                .slug("mafia-island-marine-park")
                .parkType(ParkType.MARINE_PARK)
                .region("Pwani")
                .district("Mafia")
                .location("Indian Ocean, 120 km southeast of Dar es Salaam")
                .latitude(new BigDecimal("-7.9167"))
                .longitude(new BigDecimal("39.7500"))
                .elevation("0-50 meters")
                .size("822 km²")
                .shortDescription("Tanzania's first marine protected area, renowned for pristine coral reefs, whale sharks, and world-class diving.")
                .fullDescription("Mafia Island Marine Park, established in 1995 as Tanzania's first marine park, protects 822 square kilometers. Globally renowned for whale shark encounters (October to March). Coral reefs support over 400 fish species. Five sea turtle species nest on beaches. UNESCO World Heritage Site.")
                .history("Gazetted in 1995 as Tanzania's first marine park. Historical significance with 13th-century ruins.")
                .ecosystem("Coral reefs, seagrass beds, mangrove forests, tidal channels, sandy beaches, open ocean.")
                .wildlife("Over 400 fish species, whale sharks (seasonal), manta rays, five sea turtle species, dolphins, dugongs (rare), humpback whales (seasonal).")
                .vegetation("Extensive seagrass beds, eight mangrove species, diverse hard and soft coral species.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; October to March for whale sharks")
                .openingHours("Accessible 24/7")
                .accessInformation("Accessible by air (30-minute flight from Dar) or boat. Main settlements: Kilindoni, Utende, Chole Island.")
                .tags("Marine Park, Diving, Snorkeling, Whale Sharks, Coral Reefs, Sea Turtles, Island, Indian Ocean")
                .isActive(true)
                .build();
    }

    private Park createMnaziRuvumaPark() {
        return Park.builder()
                .name("Mnazi Bay-Ruvuma Estuary Marine Park")
                .slug("mnazi-bay-ruvuma-estuary-marine-park")
                .parkType(ParkType.MARINE_PARK)
                .region("Mtwara")
                .district("Mtwara Rural")
                .location("Far southern Tanzania, 396 km south of Dar es Salaam, near Mozambique border")
                .latitude(new BigDecimal("-10.3833"))
                .longitude(new BigDecimal("40.3500"))
                .elevation("0-100 meters")
                .size("650 km²")
                .shortDescription("A pristine marine park at Tanzania's southern border with diverse coral reefs, mangroves, and turtle nesting sites.")
                .fullDescription("Mnazi Bay-Ruvuma Estuary Marine Park protects 650 square kilometers at Tanzania's southern border extending to Mozambique. Established in 2000, it's one of Tanzania's most pristine marine areas. Five sea turtle species nest on beaches. Excellent snorkeling and diving with minimal tourist pressure.")
                .history("Gazetted as marine park in 2000. Minimal development preserves wild character.")
                .ecosystem("Coral reefs, extensive mangrove forests, seagrass meadows, sandy beaches, rocky shores, Ruvuma River estuary.")
                .wildlife("Over 400 fish species, five sea turtle species, humpback whales (seasonal), dolphins, dugongs (rare), rays.")
                .vegetation("Extensive mangrove forests (nine species), seagrass beds, diverse coral species, coastal vegetation.")
                .primaryImage(null)
                .bestTimeToVisit("Year-round; June to October for diving; November to February for turtles")
                .openingHours("Accessible 24/7")
                .accessInformation("Accessible from Mtwara town (20 km). Fly to Mtwara from Dar or drive (10-12 hours).")
                .tags("Marine Park, Remote, Pristine, Diving, Snorkeling, Sea Turtles, Mangroves, Coral Reefs")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // ADDITIONAL PARKS
    // =====================================================================

    private Park createMkomaziPark() {
        return Park.builder()
                .name("Mkomazi National Park")
                .slug("mkomazi-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Kilimanjaro and Tanga")
                .district("Same")
                .location("Northern Tanzania, 112 km from Moshi, borders Kenya")
                .latitude(new BigDecimal("-4.2500"))
                .longitude(new BigDecimal("37.9167"))
                .elevation("230-1,570 meters")
                .size("3,245 km²")
                .shortDescription("A vital sanctuary for endangered black rhinos and African wild dogs with spectacular Kilimanjaro views.")
                .fullDescription("Mkomazi National Park covers 3,245 square kilometers forming continuous ecosystem with Kenya's Tsavo West. Upgraded to national park in 2008. Significant for black rhino sanctuary and African wild dog conservation programs. Spectacular views of Kilimanjaro and Usambara Mountains. Undervisited wilderness.")
                .history("Originally gazetted as game reserve in 1951. Upgraded to national park in 2008. Black rhino sanctuary established 1997.")
                .ecosystem("Acacia-commiphora bushland, open grasslands, rocky hills, seasonal rivers, baobab-studded landscapes.")
                .wildlife("Black rhinos (in sanctuary), African wild dogs (breeding program), elephants, giraffes, elands, lesser kudu, gerenuk, fringe-eared oryx, lions, leopards. Over 450 bird species.")
                .vegetation("Acacia and commiphora bushland, ancient baobabs, grasslands with scattered trees, vegetation adapted to semi-arid conditions.")
                .primaryImage(null)
                .bestTimeToVisit("June to February")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Main gate at Zange, 112 km from Moshi. Accessible from Arusha (5-6 hours) or from Dar via Tanga.")
                .tags("Black Rhino, Wild Dogs, Conservation, Kilimanjaro Views, Endangered Species, Remote, Northern Circuit")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // NEW PARKS (2019 onwards)
    // =====================================================================

    private Park createSelousGameReserve() {
        return Park.builder()
                .name("Selous Game Reserve")
                .slug("selous-game-reserve")
                .parkType(ParkType.GAME_RESERVE)
                .region("Lindi, Morogoro, Mtwara, and Ruvuma")
                .district("Liwale")
                .location("Southern Tanzania, 219 km from Dar es Salaam")
                .latitude(new BigDecimal("-8.0833"))
                .longitude(new BigDecimal("37.6167"))
                .elevation("100-1,300 meters")
                .size("50,000 km²")
                .shortDescription("One of the largest protected areas in Africa and UNESCO World Heritage Site, home to significant concentrations of elephants, buffalo, and endangered wild dogs.")
                .fullDescription("Selous Game Reserve is one of the largest faunal reserves in the world, covering 50,000 square kilometers in southern Tanzania. Designated a UNESCO World Heritage Site in 1982 due to its high biodiversity and vast undisturbed natural landscapes. In 2019, the northern section was carved out to create Nyerere National Park, while the southern portion remains as Selous Game Reserve. The Rufiji River flows through the reserve creating a network of channels, lakes, and swamps. Named after Frederick Selous, a famous big game hunter and conservationist who died here in 1917 during World War I.")
                .history("First designated as protected area in 1896. Became hunting reserve in 1905. Designated UNESCO World Heritage Site in 1982. Divided in 2019 with northern portion becoming Nyerere National Park.")
                .ecosystem("Miombo woodland, acacia savanna, riverine forests, seasonal swamps, wetlands, and the extensive Rufiji River system.")
                .wildlife("Over 2,100 plant species. Large populations of elephants, black rhinos, hippos, lions, leopards, spotted hyenas, painted dogs, Cape buffalo, Masai giraffes, plains zebras, and Nile crocodiles. Over 440 bird species including the African skimmer and Pel's fishing owl.")
                .vegetation("Extensive miombo woodlands, borassus palms along rivers, grasslands, riverine forests with mahogany and ebony trees, swamp vegetation.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible by air to various airstrips or by road from Dar es Salaam (5-6 hours). Multiple entry points.")
                .tags("UNESCO World Heritage, Rufiji River, Wild Dogs, Wilderness, Boat Safari, Elephants, Remote, Southern Circuit, Large Reserve")
                .isActive(true)
                .build();
    }

    private Park createBurigiChatoPark() {
        return Park.builder()
                .name("Burigi-Chato National Park")
                .slug("burigi-chato-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Kagera and Geita")
                .district("Karagwe, Biharamulo, Muleba, and Chato")
                .location("Northwestern Tanzania, bordering Rwanda and near Uganda, encircled by Kagera River and Lake Burigi")
                .latitude(new BigDecimal("-2.5000"))
                .longitude(new BigDecimal("30.9167"))
                .elevation("1,150-1,800 meters")
                .size("4,707 km²")
                .shortDescription("Tanzania's fourth-largest park, designated in 2019, renowned for rare roan and sable antelopes and exceptional birdwatching near Lake Victoria.")
                .fullDescription("Burigi-Chato National Park, covering 4,707 square kilometers, was designated as a national park in 2019. Located in Tanzania's Kagera and Geita regions, the park stretches from Lake Victoria in the east to the Rwandan border in the west, encircled by the Kagera River and Lake Burigi. The park is among the few places in Tanzania where visitors have strong chances of encountering rare roan and sable antelopes, two of Africa's most elusive and majestic herbivores. With over 400 recorded bird species, including fish eagles, papyrus gonolek, and the bizarre shoebill stork, it offers exceptional birdwatching opportunities. The park's scenic beauty features rolling hills, valleys, plains of acacia savannah, and a central basin with seasonal rivers, scattered lakes, and swamps.")
                .history("Designated as national park in 2019, previously operated as Burigi Game Reserve. Part of Tanzania's expansion of protected areas.")
                .ecosystem("Acacia savannah, rolling hills, riverine forests, wetlands, scattered lakes, seasonal swamps, and the Kagera River system.")
                .wildlife("Elephants, buffaloes, lions, leopards, zebras, giraffes, and diverse antelope species including rare roan and sable antelopes. Over 400 bird species including fish eagles, papyrus gonolek, and shoebill storks.")
                .vegetation("Acacia savannah plains, riverine vegetation along Kagera River, wetland plants, grasslands, and scattered woodland.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season for wildlife viewing)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Accessible from Karagwe town. Main access via Bukoba (approximately 120 km). Limited infrastructure as newly established park.")
                .tags("Roan Antelope, Sable Antelope, Birdwatching, Lake Victoria, Kagera River, New Park, Rolling Hills, Rwanda Border")
                .isActive(true)
                .build();
    }

    private Park createSaananePark() {
        return Park.builder()
                .name("Saanane Island National Park")
                .slug("saanane-island-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Mwanza")
                .district("Ilemela")
                .location("Island in Lake Victoria, 2 km southwest of Mwanza City center")
                .latitude(new BigDecimal("-2.5167"))
                .longitude(new BigDecimal("32.9000"))
                .elevation("1,134 meters (lake level)")
                .size("2.18 km²")
                .shortDescription("Tanzania's smallest national park and first island-based park, located within Mwanza City in Lake Victoria, home to unique De-brazza's monkeys.")
                .fullDescription("Saanane Island National Park, covering just 2.18 square kilometers, is the smallest national park in Tanzania and East Africa, and the first national park to be located within a city and on an island. Situated in Lake Victoria's Ilemela District, the park is exceptionally accessible via a 5-minute boat ride from the mainland. Despite its compact size, the park hosts diverse wildlife including impalas, velvet monkeys, rock hyrax, wild cats, and the rare De-brazza's monkey - making it the only park in Tanzania inhabiting this species. Reptiles include crocodiles, monitor lizards, agama lizards, pancake and leopard tortoises, and pythons. With over 70 recorded bird species and rich aquatic life including tilapia and Nile perch, the park offers unique urban wildlife experiences with rock hiking, picnicking, and exceptional birdwatching.")
                .history("Designated as national park, making it the first urban national park in Tanzania and the first island-based national park in the country.")
                .ecosystem("Island ecosystem in Lake Victoria with rocky terrain, grasslands, aquatic zones, and lakeshore habitats.")
                .wildlife("Impalas, velvet monkeys, rock hyrax, wild cats, unique De-brazza's monkeys (only location in Tanzania), crocodiles, monitor lizards, tortoises, pythons. Over 70 bird species. Aquatic life includes tilapia and Nile perch.")
                .vegetation("Island grasslands, rocky outcrops with adapted vegetation, lakeshore plants, scattered trees and shrubs.")
                .primaryImage(null)
                .bestTimeToVisit("June to August (dry season); November to March (birdwatching)")
                .openingHours("8:00 AM - 5:00 PM daily")
                .accessInformation("5-minute boat ride from Park Offices on Mwanza mainland. Exceptionally accessible, located within Mwanza City.")
                .tags("Island Park, Smallest Park, Urban Park, Lake Victoria, De-brazza Monkey, Mwanza, Accessible, Unique, Rock Hiking")
                .isActive(true)
                .build();
    }

    private Park createIbandaKyerwaPark() {
        return Park.builder()
                .name("Ibanda-Kyerwa National Park")
                .slug("ibanda-kyerwa-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Kagera")
                .district("Kyerwa")
                .location("Western Tanzania at tripoint where Tanzania, Rwanda, and Uganda borders meet, along Kagera River")
                .latitude(new BigDecimal("-1.1667"))
                .longitude(new BigDecimal("30.8333"))
                .elevation("1,150-1,450 meters")
                .size("200 km²")
                .shortDescription("Located at the unique tripoint of three nations, this 2019-designated park features rolling hills, riverine forests, and healthy populations of roan antelope and hippos.")
                .fullDescription("Ibanda-Kyerwa National Park covers 200 square kilometers in Tanzania's western corner at the unique location where Tanzania, Rwanda, and Uganda borders converge. Designated as a national park in 2019, it was previously Ibanda Game Reserve established in 1974. The park's scenic beauty features rolling hills, valleys, acacia savannah plains, and a central basin dissected by seasonal rivers filling scattered lakes and swamps. The Kagera River, serving as the park's main water source, nourishes its rich wildlife resources. The park is characterized by rolling hills, riverine forests, and open savannahs. Plains game including topi, eland, impala, waterbuck, reedbuck, and bushbuck are abundant. Large herds of roan antelope, one of Africa's most dramatic species, are particularly notable. Buffalo herds are common, while leopards and hyenas serve as the main large predators (currently no lions). The Kagera River and park's lakes and swamps host abundant hippos and crocodiles.")
                .history("Originally established as Ibanda Game Reserve in 1974. Upgraded to national park status in 2019 as part of Tanzania's park expansion program.")
                .ecosystem("Rolling hills, riverine forests, open savannahs, acacia savannah, seasonal rivers, scattered lakes, swamps, and Kagera River system.")
                .wildlife("African buffalo, hippos, leopards, hyenas, crocodiles. Plains game: topi, eland, impala, waterbuck, reedbuck, bushbuck. Notable for large herds of roan antelope. Over 200 bird species.")
                .vegetation("Acacia savannah, riverine forests along Kagera River, swamp vegetation, grasslands, scattered woodland.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season, animals gather near Kagera River, thinner vegetation)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Located in Kagera Region near tripoint of Tanzania-Rwanda-Uganda borders. Access via regional roads from Kagera.")
                .tags("Tripoint, Three Borders, Roan Antelope, Kagera River, Rolling Hills, Rwanda, Uganda, New Park 2019")
                .isActive(true)
                .build();
    }

    private Park createKituloPark() {
        return Park.builder()
                .name("Kitulo National Park")
                .slug("kitulo-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Mbeya and Njombe")
                .district("Rungwe and Ludewa")
                .location("Southern Highlands, between Kipengere and Poroto Mountains")
                .latitude(new BigDecimal("-9.0500"))
                .longitude(new BigDecimal("33.7833"))
                .elevation("2,600-3,000 meters")
                .size("412.9 km²")
                .shortDescription("Known as the 'Serengeti of Flowers' and 'Garden of God,' this montane grassland plateau hosts over 350 plant species including 45 orchid varieties.")
                .fullDescription("Kitulo National Park, covering 412.9 square kilometers, is a rare botanical marvel situated at elevations between 2,600 and 3,000 meters between the Kipengere and Poroto Mountains. Locally referred to as 'Bustani ya Mungu' (The Garden of God), and called the 'Serengeti of Flowers' by botanists, the park is exceptionally rich in flora with over 350 species of vascular plants. The park's crown jewel is its extraordinary orchid diversity: 45 species of terrestrial orchids have been recorded, with 31 endemic to Tanzania, 16 endemic to Kitulo and Poroto Mountains, and at least 3 species endemic only to Kitulo National Park. The park transforms into a spectacular wildflower display during the rainy season (November to April) when the montane grasslands erupt in breathtaking riots of color. This unique high-altitude ecosystem provides critical watershed protection and represents one of the world's great floral spectacles.")
                .history("Designated as national park to protect this unique montane grassland ecosystem and its exceptional botanical diversity, particularly its endemic orchid species.")
                .ecosystem("Montane grassland plateau, highland meadows, wetlands, streams, afro-alpine vegetation zones.")
                .wildlife("Limited large mammals due to high altitude. Some eland, reedbuck, and mountain reedbuck. Rich in endemic plants, particularly orchids. Various bird species adapted to highland habitats.")
                .vegetation("Over 350 vascular plant species including 45 terrestrial orchid species (31 endemic to Tanzania, 16 endemic to Kitulo-Poroto region, 3 endemic to Kitulo only). Wildflowers create spectacular displays November-April. Montane grasslands, afro-alpine plants.")
                .primaryImage(null)
                .bestTimeToVisit("November to April (rainy season - spectacular wildflower blooms and orchid displays)")
                .openingHours("7:00 AM - 5:00 PM daily")
                .accessInformation("Located in southern highlands between Mbeya and Njombe regions. Access via Chimala or Matamba gates. Challenging mountain roads require 4WD vehicles.")
                .tags("Orchids, Wildflowers, Botanical, Serengeti of Flowers, Garden of God, Endemic Species, Montane, Highland, Unique Flora")
                .isActive(true)
                .build();
    }

    private Park createRumanyikaKaragwePark() {
        return Park.builder()
                .name("Rumanyika-Karagwe National Park")
                .slug("rumanyika-karagwe-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Kagera")
                .district("Karagwe")
                .location("Northwestern Tanzania near Lake Victoria, close to Uganda and Rwanda borders, 120 km from Bukoba")
                .latitude(new BigDecimal("-1.9167"))
                .longitude(new BigDecimal("30.9000"))
                .elevation("1,150-1,450 meters")
                .size("245 km²")
                .shortDescription("A 2019-designated wetland paradise with over 300 bird species, featuring Lake Rumanyika, diverse habitats, and important migration corridors near Lake Victoria.")
                .fullDescription("Rumanyika-Karagwe National Park was designated as a national park in 2019 after previously operating as a game reserve. Located in the Kagera region near Lake Victoria, approximately 120 km from Bukoba, the park serves as a crucial wetland ecosystem and important bird migration corridor. The park's diverse habitats, ranging from 1,150 to 1,450 meters elevation, include wetlands, woodlands, the Rumanyika River, Lake Rumanyika, and significant swamp areas like Nsonge Swamp. This variety attracts both resident and migratory birds, with over 300 species recorded, making it a paradise for birdwatchers. The park's position adjacent to Lake Victoria and Kagera wetlands makes it particularly important during bird migration seasons (December to February). Lake Rumanyika hosts hippos, crocodiles, and numerous water birds. During dry seasons, wildlife concentrates around permanent water bodies including the Rumanyika River and Lake Rumanyika, providing excellent viewing opportunities.")
                .history("Previously operated as game reserve. Gazetted as national park in 2019 as part of Tanzania's protected area expansion program.")
                .ecosystem("Wetlands (1,150-1,450m elevation), woodlands, Lake Rumanyika, Rumanyika River, Nsonge Swamp, seasonal swamps, Lake Victoria adjacency.")
                .wildlife("Lions, leopards, elephants, hippos, crocodiles, giraffes, buffalo, various antelope species. Over 300 bird species making it exceptional for birdwatching. Important migration corridor for birds.")
                .vegetation("Wetland vegetation, woodland species, aquatic plants in Lake Rumanyika and swamps, grasslands, riverine vegetation along Rumanyika River.")
                .primaryImage(null)
                .bestTimeToVisit("December to February (bird migration); June to October (dry season)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Located near Karagwe, approximately 120 km from Bukoba. Access via regional roads in Kagera Region.")
                .tags("Wetlands, Birdwatching, Migration Corridor, Lake Victoria, Lake Rumanyika, Hippos, Swamps, New Park 2019, Over 300 Bird Species")
                .isActive(true)
                .build();
    }

    private Park createUgallaRiverPark() {
        return Park.builder()
                .name("Ugalla River National Park")
                .slug("ugalla-river-national-park")
                .parkType(ParkType.NATIONAL_PARK)
                .region("Tabora")
                .district("Kaliua and Urambo")
                .location("West-central Tanzania, eastern part of Lake Tanganyika region")
                .latitude(new BigDecimal("-5.2500"))
                .longitude(new BigDecimal("31.7500"))
                .elevation("900-1,200 meters")
                .size("3,865 km²")
                .shortDescription("Established in 2019, this vast miombo woodland park protects Tanzania's largest herds of sable and roan antelope along the life-giving Ugalla River.")
                .fullDescription("Ugalla River National Park, covering 3,865 square kilometers in Tabora Region of west-central Tanzania, was established in 2019 when the Tanzanian parliament separated part of the Ugalla River Game Reserve to form a national park. The park is characterized by two primary vegetation zones: extensive miombo woodlands and Zambezian flooded grasslands. Miombo forests, a unique and vast forest type found across southern Africa, dominate the landscape creating spacious woodlands and tall grass savannas. The Ugalla River, serving as the park's namesake and only permanent water source during dry season, is crucial for the ecosystem. The river harbors healthy populations of hippos and some of Africa's largest crocodiles. The park hosts impressive wildlife including elephants, buffalo, lions, leopards, giraffes, zebras, and the largest herds of sable and roan antelope to be seen in Tanzania. Additional species include wild dogs, hyenas, jackals, impalas, elands, greater kudus, Lichtenstein's hartebeests, topis, oribis, duikers, and various reedbuck species. Over 400 bird species have been recorded, making it a haven for birdwatchers.")
                .history("Established as national park in 2019 when Tanzanian parliament separated this portion from Ugalla River Game Reserve to create dedicated national park protection.")
                .ecosystem("Extensive miombo woodlands, Zambezian flooded grasslands, tall grass savannas, Ugalla River system (sole permanent dry-season water source).")
                .wildlife("Elephants, buffalo, lions, leopards, giraffes, zebras, largest herds of sable and roan antelope in Tanzania, wild dogs, hyenas, jackals, impalas, elands, greater kudus, Lichtenstein's hartebeests, topis, oribis, duikers, reedbucks. Hippos and large crocodiles in Ugalla River. Over 400 bird species.")
                .vegetation("Dominant miombo woodland (unique southern African forest type), tall grasses, Zambezian grassland species, riverine vegetation along Ugalla River.")
                .primaryImage(null)
                .bestTimeToVisit("June to October (dry season - wildlife concentrates near Ugalla River)")
                .openingHours("6:00 AM - 6:00 PM daily")
                .accessInformation("Located in Tabora Region, west-central Tanzania. Access from Tabora town. Limited infrastructure as newly established park.")
                .tags("Miombo Woodland, Sable Antelope, Roan Antelope, Ugalla River, New Park 2019, Wild Dogs, Crocodiles, Wilderness, Western Tanzania")
                .isActive(true)
                .build();
    }
}
