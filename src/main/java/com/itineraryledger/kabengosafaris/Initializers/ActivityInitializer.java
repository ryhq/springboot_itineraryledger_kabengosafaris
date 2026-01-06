package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.Activity.ChargingBasis;
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
 * ActivityInitializer - Initializes Tanzania Tourism Activities at application startup
 *
 * IMPORTANT: The order of activities in this initializer matches the production database
 * (idealafr_idealafricantravels.sql) to ensure consistent auto-generated IDs.
 * DO NOT change the order of activities without updating the production database!
 *
 * Order (IDs 1-26):
 * 1. Entrance, 2. Walking Safari, 3. Ranger, 4. Bush Meals, 5. Canopy Walk,
 * 6. Night Game Drive, 7. Canoeing, 8. Filming, 9. Others, 10. Sport Fishing,
 * 11. Horse Riding, 12. Cycling, 13. Boating, 14. Rhino Viewing, 15. Crater Service,
 * 16. Sun Down, 17. Hot Air Balloon, 18. Olduvai Gorge, 19. Shifting Sand, 20. Laetoli Footprints,
 * 21. Guided Lectures, 22. Bird Watching, 23. Cultural Tours, 24. Photography Safaris,
 * 25. Rock Climbing, 26. Wildlife Tracking
 *
 * Runs AFTER ParkInitializer (Order = HIGHEST_PRECEDENCE + 7)
 *
 * Behavior:
 * - Only creates activities that don't already exist (checks by name)
 * - Does not update existing activities to preserve user modifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 7) // Run after ParkInitializer
public class ActivityInitializer implements ApplicationRunner {

    private final ActivityRepository activityRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        boolean success = true;

        try {
            initializeActivities();
        } catch (Exception e) {
            log.error("Error during activity initialization: {}", e.getMessage(), e);
            success = false;
        } finally {
            printEndBanner(success);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║              ACTIVITY INITIALIZER - START                          ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║           ✓ ACTIVITY INITIALIZER - COMPLETED                       ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║           ✗ ACTIVITY INITIALIZER - FAILED                          ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializeActivities() {
        log.info("Starting Tanzania Activities initialization...");

        List<Activity> activitiesToCreate = getTanzaniaActivities();
        int createdCount = 0;
        int skippedCount = 0;

        for (Activity activity : activitiesToCreate) {
            if (!activityRepository.existsByName(activity.getName())) {
                activityRepository.save(activity);
                log.info("✓ Created activity: {}", activity.getName());
                createdCount++;
            } else {
                log.debug("⊘ Activity already exists: {}", activity.getName());
                skippedCount++;
            }
        }

        log.info("");
        log.info("Activity initialization complete: {} activities created, {} activities skipped (already exist)",
                createdCount, skippedCount);
        log.info("Total activities in database: {}", activityRepository.count());
    }

    /**
     * Get list of all Tanzania activities to initialize
     *
     * IMPORTANT: The order of activities in this initializer matches the production database
     * (idealafr_idealafricantravels.sql) to ensure consistent auto-generated IDs.
     * DO NOT change the order of activities without updating the production database!
     *
     * Order (IDs 1-26):
     * 1. Entrance, 2. Walking Safari, 3. Ranger, 4. Bush Meals, 5. Canopy Walk,
     * 6. Night Game Drive, 7. Canoeing, 8. Filming, 9. Others, 10. Sport Fishing,
     * 11. Horse Riding, 12. Cycling, 13. Boating, 14. Rhino Viewing, 15. Crater Service,
     * 16. Sun Down, 17. Hot Air Balloon, 18. Olduvai Gorge, 19. Shifting Sand, 20. Laetoli Footprints,
     * 21. Guided Lectures, 22. Bird Watching, 23. Cultural Tours, 24. Photography Safaris,
     * 25. Rock Climbing, 26. Wildlife Tracking
     */
    private List<Activity> getTanzaniaActivities() {
        List<Activity> activities = new ArrayList<>();

        // ID 1: Entrance
        activities.add(createEntranceActivity());

        // ID 2: Walking Safari
        activities.add(createWalkingSafariActivity());

        // ID 3: Ranger
        activities.add(createRangerActivity());

        // ID 4: Bush Meals
        activities.add(createBushMealsActivity());

        // ID 5: Canopy Walk
        activities.add(createCanopyWalkActivity());

        // ID 6: Night Game Drive
        activities.add(createNightGameDriveActivity());

        // ID 7: Canoeing
        activities.add(createCanoeingActivity());

        // ID 8: Filming
        activities.add(createFilmingActivity());

        // ID 9: Others
        activities.add(createOthersActivity());

        // ID 10: Sport Fishing
        activities.add(createSportFishingActivity());

        // ID 11: Horse Riding
        activities.add(createHorseRidingActivity());

        // ID 12: Cycling
        activities.add(createCyclingActivity());

        // ID 13: Boating
        activities.add(createBoatingActivity());

        // ID 14: Rhino Viewing
        activities.add(createRhinoViewingActivity());

        // ID 15: Crater Service
        activities.add(createCraterServiceActivity());

        // ID 16: Sun Down
        activities.add(createSunDownActivity());

        // ID 17: Hot Air Balloon
        activities.add(createHotAirBalloonActivity());

        // ID 18: Olduvai Gorge
        activities.add(createOlduvaiGorgeActivity());

        // ID 19: Shifting Sand
        activities.add(createShiftingSandActivity());

        // ID 20: Laetoli Footprints
        activities.add(createLaetoliFootprintsActivity());

        // ID 21: Guided Lectures
        activities.add(createGuidedLecturesActivity());

        // ID 22: Bird Watching
        activities.add(createBirdWatchingActivity());

        // ID 23: Cultural Tours
        activities.add(createCulturalToursActivity());

        // ID 24: Photography Safaris
        activities.add(createPhotographySafarisActivity());

        // ID 25: Rock Climbing
        activities.add(createRockClimbingActivity());

        // ID 26: Wildlife Tracking
        activities.add(createWildlifeTrackingActivity());

        return activities;
    }

    // =====================================================================
    // ESSENTIAL PARK SERVICES (IDs 1-3)
    // =====================================================================

    private Activity createEntranceActivity() {
        return Activity.builder()
                .name("Entrance")
                .hasTariff(true)
                .isWebActive(false)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Park entrance fee providing access to park amenities and the opportunity to experience game drives, walking safaris, and the natural scenery and wildlife.")
                .detailedDescription("The entrance fee is a mandatory charge for accessing Tanzania's national parks and conservation areas. This fee supports park management, conservation efforts, anti-poaching operations, infrastructure maintenance, and community development programs. Upon payment, visitors receive access to the park's full range of offerings including game drives, designated picnic areas, viewing points, and visitor centers. The fee varies by park and visitor category (international, resident, East African). Some parks offer multi-day passes at discounted rates. Revenue from entrance fees directly contributes to wildlife protection and habitat preservation. Park rangers at entrance gates provide maps, safety briefings, and current wildlife sighting information. The entrance process includes vehicle registration and visitor documentation for security and conservation monitoring purposes.")
                .minimumAge(0)
                .maximumParticipants(999)
                .equipmentRequired("Valid identification, park permit or booking confirmation, payment method (cash USD or card where accepted)")
                .seasonAvailability("Year-round; All parks require entrance fees regardless of season")
                .tags("Entrance Fee, Park Access, Conservation Fee, Mandatory, Park Services, Gate Fee")
                .safetyInformation("Keep your entrance receipt as it may be checked at various points within the park. Follow all park rules and regulations. Respect gate opening and closing times. Ensure your vehicle is registered if driving yourself.")
                .isActive(true)
                .build();
    }

    private Activity createWalkingSafariActivity() {
        return Activity.builder()
                .name("Walking Safari")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Explore the wilderness on foot with armed guides, experiencing nature at ground level and tracking animals through the bush.")
                .detailedDescription("Walking safaris offer an intimate and thrilling way to experience Tanzania's wilderness. Led by armed park rangers and professional guides, you'll explore the bush on foot, learning to track animals, identify plants, read spoor (animal tracks), and understand the intricate relationships within the ecosystem. This activity engages all your senses - the smell of wild sage, the sound of birds, the texture of tree bark, and the excitement of encountering wildlife at their level. Walking safaris typically last 2-4 hours and cover 5-8 kilometers at a gentle pace with frequent stops for observations and explanations. You'll learn survival skills, understand animal behavior, and appreciate smaller details often missed from a vehicle - insects, plants, tracks, and signs of animal activity. The experience provides a deeper connection to nature and a sense of adventure that vehicle-based safaris cannot match.")
                .minimumAge(12)
                .maximumParticipants(8)
                .equipmentRequired("Sturdy walking boots, long trousers, long-sleeved shirt (neutral colors), wide-brimmed hat, sunscreen, insect repellent, water bottle, small backpack, binoculars")
                .seasonAvailability("Year-round; Best: June-October (dry season, easier walking)")
                .tags("Walking, Nature, Adventure, Tracking, Education, Bush Skills, Conservation")
                .safetyInformation("Always follow guide instructions. Stay together as a group. Walk in single file when instructed. Remain quiet and avoid sudden movements. Do not run if you encounter animals. The armed ranger is for your protection - follow their commands immediately.")
                .isActive(true)
                .build();
    }

    private Activity createRangerActivity() {
        return Activity.builder()
                .name("Ranger")
                .hasTariff(true)
                .isWebActive(false)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Hire a professional park ranger or guide to accompany you during activities, providing safety, expert guidance, and in-depth knowledge about wildlife and conservation.")
                .detailedDescription("Park rangers are trained professionals who enhance safari experiences through their extensive knowledge of wildlife behavior, tracking skills, and understanding of park ecosystems. Hiring a ranger is mandatory for certain activities like walking safaris, hiking in restricted areas, and visiting specific conservation zones. Rangers ensure visitor safety, particularly in areas with dangerous wildlife, and are trained in first aid and emergency response. They provide expert interpretation of animal behavior, identify species, explain conservation challenges, and share insights about anti-poaching efforts. Rangers know the best locations for wildlife sightings based on recent activity and seasonal patterns. Many rangers have specialized knowledge in areas like ornithology, botany, or large mammal behavior. Their presence supports local communities as many rangers are recruited from nearby villages. Ranger fees contribute to their salaries and professional development, supporting conservation career pathways.")
                .minimumAge(0)
                .maximumParticipants(20)
                .equipmentRequired("None required - ranger provides guidance and safety equipment as needed")
                .seasonAvailability("Year-round; Essential service available daily during park operating hours")
                .tags("Ranger, Guide, Safety, Expert Knowledge, Conservation, Wildlife Tracking, Professional Guide")
                .safetyInformation("Always follow your ranger's instructions immediately. Rangers are trained in wildlife safety and emergency procedures. They carry communication equipment and first aid supplies. Do not separate from your ranger during guided activities.")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // DINING & ENTERTAINMENT EXPERIENCES (IDs 4, 16)
    // =====================================================================

    private Activity createBushMealsActivity() {
        return Activity.builder()
                .name("Bush Meals")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Enjoy gourmet dining experiences in the wilderness, including bush breakfasts, picnic lunches, and candlelit dinners under African skies.")
                .detailedDescription("Bush meals transform dining into unforgettable safari experiences. Bush breakfasts after early morning balloon safaris or game drives feature freshly cooked meals under acacia trees with white-tablecloth service in the wilderness. Bush lunches during full-day safaris are enjoyed at scenic spots - beside rivers, under shade trees, overlooking valleys. Some lodges offer candlelit dinners under the stars, complete with traditional dancers, bonfire, and gourmet multi-course meals. These experiences combine exceptional food with spectacular settings and often include wildlife sightings - elephants approaching at dusk, nocturnal animals beginning their activities. Professional staff set up elegant dining areas in remote locations, creating magical moments that epitomize luxury safari experiences.")
                .minimumAge(5)
                .maximumParticipants(20)
                .equipmentRequired("Light jacket for cooler weather, camera, appropriate dining attire (smart casual), binoculars")
                .seasonAvailability("Year-round; Dry season (June-October) most reliable for outdoor dining events")
                .tags("Bush Meals, Wilderness Dining, Luxury, Gourmet, Safari Experience, Romance, Photography")
                .safetyInformation("Follow staff instructions regarding wildlife safety. Do not wander from dining areas. Keep food secured from wildlife. Stay within designated safe zones. Bring warm layers for evening meals.")
                .isActive(true)
                .build();
    }

    private Activity createSunDownActivity() {
        return Activity.builder()
                .name("Sun Down")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Experience spectacular African sunsets with cocktails and snacks at scenic viewpoints, a cherished safari tradition.")
                .detailedDescription("Sundowners are a quintessential safari ritual where guests gather at scenic viewpoints as the sun sets, enjoying cocktails, soft drinks, and canapés while watching the golden hour transform the African landscape. Guides select perfect locations - atop hills with panoramic views, beside waterholes where animals gather, or on open plains with unobstructed horizons. As the sun descends, casting warm golden and orange hues across the savanna, you'll witness the magical transition from day to night. Wildlife becomes silhouetted against colorful skies, and nocturnal animals begin their activities. The experience typically lasts 45-60 minutes, providing exceptional photography opportunities during the golden hour. Stories are shared, the day's sightings discussed, and the beauty of Africa's wilderness celebrated. Some sundowners include traditional music or cultural performances. It's a peaceful, romantic way to end safari days.")
                .minimumAge(8)
                .maximumParticipants(15)
                .equipmentRequired("Light jacket or wrap (temperatures drop after sunset), camera, sunglasses, binoculars")
                .seasonAvailability("Year-round; Most spectacular during dry season (June-October) with clearer skies")
                .tags("Sundowner, Sunset, Cocktails, Safari Tradition, Photography, Romance, Golden Hour, Relaxation")
                .safetyInformation("Stay within designated areas. Follow guide instructions. Do not wander off alone. Be aware of wildlife activity at dusk. Enjoy alcohol responsibly. Bring warm clothing as temperatures drop quickly after sunset.")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // ADVENTURE ACTIVITIES (IDs 5-7, 10-13, 25)
    // =====================================================================

    private Activity createCanopyWalkActivity() {
        return Activity.builder()
                .name("Canopy Walk")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Walk through the forest canopy on suspended bridges, experiencing the treetop ecosystem and stunning views.")
                .detailedDescription("Canopy walks provide a thrilling perspective on Tanzania's forests from elevated walkways 20-40 meters above the ground. Located in parks like Udzungwa Mountains, these suspended bridges and platforms allow you to walk through the treetops, experiencing the forest from the monkeys' perspective. The walkway sways gently, adding to the excitement while remaining completely safe. From this vantage point, you'll observe different bird species, colobus monkeys, butterflies, and unique plants that thrive in the canopy layer. The experience lasts 1-2 hours and includes educational information about forest ecology, the importance of canopy layers, and conservation. Stunning views extend across the forest to distant mountains. The activity is suitable for most fitness levels, though those with severe fear of heights should consider carefully. Sunrise and sunset canopy walks offer special lighting and increased animal activity.")
                .minimumAge(8)
                .maximumParticipants(15)
                .equipmentRequired("Comfortable walking shoes, light clothing, hat, sunscreen, camera, water bottle, binoculars")
                .seasonAvailability("Year-round; Best: June-October (clearer weather, better views)")
                .tags("Canopy Walk, Forest, Heights, Adventure, Monkeys, Birdwatching, Udzungwa, Views")
                .safetyInformation("Follow guide instructions. Hold handrails at all times. Walk carefully on bridges. Do not jump or rock the walkway. Maximum weight limits apply. Not suitable for those with extreme fear of heights or serious balance issues.")
                .isActive(true)
                .build();
    }

    private Activity createNightGameDriveActivity() {
        return Activity.builder()
                .name("Night Game Drive")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Discover the nocturnal world of African wildlife on specially equipped night drives with spotlights.")
                .detailedDescription("Night game drives reveal a completely different side of the African wilderness. As darkness falls, nocturnal predators and prey emerge, creating thrilling opportunities to observe behaviors rarely seen during daylight hours. Equipped with powerful spotlights and red filters (which don't disturb animals), you'll search for leopards hunting, lions on the prowl, hyenas scavenging, genets, civets, bush babies, and nightjars. Your guide's spotlight will illuminate eyes reflecting in the darkness - each color indicating different species. Night drives typically run from 6:30 PM to 9:30 PM and offer cooler temperatures and dramatic encounters. You might witness a predator making a kill, hear the spine-chilling calls of hyenas, or see porcupines, aardvarks, and other rarely-seen nocturnal creatures. The experience includes stargazing opportunities in areas with minimal light pollution.")
                .minimumAge(8)
                .maximumParticipants(6)
                .equipmentRequired("Warm jacket or fleece, long trousers, closed shoes, flashlight/headlamp, camera with good low-light capability, binoculars")
                .seasonAvailability("Year-round; Best: June-October (dry season, better visibility)")
                .tags("Night Safari, Nocturnal Wildlife, Predators, Adventure, Leopard, Photography")
                .safetyInformation("Remain seated in the vehicle at all times. Do not shine personal lights at animals. Keep noise to a minimum. Follow guide instructions regarding when to use camera flash. Be aware that nocturnal predators are active.")
                .isActive(true)
                .build();
    }

    private Activity createCanoeingActivity() {
        return Activity.builder()
                .name("Canoeing")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Paddle peacefully through rivers and lakes, observing hippos, crocodiles, and abundant birdlife from the water.")
                .detailedDescription("Canoeing safaris offer a unique aquatic perspective on Tanzania's wildlife, particularly in areas like Arusha National Park's Momella Lakes or along the Rufiji River in Selous/Nyerere. Paddling in stable two-person canoes, you'll glide silently across calm waters, getting remarkably close to hippos (from a safe distance), observing crocodiles basking on riverbanks, and spotting diverse waterbirds including African fish eagles, kingfishers, herons, and storks. The experience typically lasts 2-3 hours and requires no previous canoeing experience - guides provide instruction and accompany you. You'll learn about aquatic ecosystems, see animals coming to drink, and enjoy the serenity of being on the water. This activity is excellent for photographers wanting different angles and for those seeking a peaceful, contemplative wildlife experience. Some locations offer sunset canoeing for spectacular lighting and increased wildlife activity.")
                .minimumAge(10)
                .maximumParticipants(12)
                .equipmentRequired("Life jacket (provided), sun protection, quick-dry clothing, waterproof bag for camera/phone, water bottle, binoculars")
                .seasonAvailability("Year-round; Best: June-October (lower water levels, wildlife concentrated)")
                .tags("Canoeing, Water Safari, Hippos, Birdwatching, Adventure, Peaceful, Photography")
                .safetyInformation("Always wear the provided life jacket. Follow guide instructions regarding distance from hippos and crocodiles. Stay together as a group. Do not stand up in the canoe. Avoid sudden movements or loud noises near hippos - they are territorial and dangerous.")
                .isActive(true)
                .build();
    }

    private Activity createSportFishingActivity() {
        return Activity.builder()
                .name("Sport Fishing")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Enjoy catch-and-release fishing in designated park areas for tilapia, catfish, and other species.")
                .detailedDescription("Sport fishing in Tanzania's national parks and reserves is a regulated catch-and-release activity designed to provide recreational enjoyment while protecting fish populations. Available in select areas like Lake Manyara, Rubondo Island, and some Rufiji River sections, fishing trips last 3-4 hours and target species including tilapia, catfish (vundu), tiger fish, and others. Experienced guides provide equipment (rods, reels, bait) and instruction for beginners, while experienced anglers can bring their own gear. You'll learn about aquatic ecosystems, fish behavior, and conservation efforts while enjoying the peaceful experience of fishing in stunning natural settings. All catches must be released unharmed, and barbless hooks are required. The activity combines fishing with wildlife viewing as you'll see hippos, crocodiles, and birds while waiting for bites. Photography of catches before release is encouraged.")
                .minimumAge(12)
                .maximumParticipants(6)
                .equipmentRequired("Hat, sunscreen, sunglasses, camera, water bottle, light snacks. Fishing equipment provided or bring your own.")
                .seasonAvailability("Year-round; Best: June-October (clearer water, fish more active)")
                .tags("Fishing, Catch and Release, Conservation, Lake Manyara, Peaceful, Recreation")
                .safetyInformation("Use only barbless hooks. Release all fish carefully and immediately. Do not fish near hippos or crocodiles. Follow guide instructions regarding safe fishing locations. Wear sunscreen and stay hydrated.")
                .isActive(true)
                .build();
    }

    private Activity createHorseRidingActivity() {
        return Activity.builder()
                .name("Horse Riding")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Experience the African wilderness on horseback, getting close to wildlife in a unique and peaceful way.")
                .detailedDescription("Horse riding safaris offer a unique way to experience the African bush. On well-trained horses, you can approach wildlife more closely than in vehicles, as animals are less threatened by horses. Rides typically last 2-3 hours and cover diverse terrain including grasslands, woodlands, and riverbanks. Experienced guides lead small groups, matching horse and rider abilities. You'll encounter zebras, giraffes, wildebeest, and various antelope species, experiencing the thrill of being at their level. The pace varies from walking to cantering across open plains, providing excellent exercise combined with wildlife viewing. No previous riding experience is necessary for shorter rides, though longer expeditions require moderate to advanced skills. The silence of horse travel, compared to vehicles, creates a more intimate connection with nature. Some lodges offer multi-day riding safaris with overnight camping for experienced riders.")
                .minimumAge(12)
                .maximumParticipants(8)
                .equipmentRequired("Long trousers, boots with small heel or riding boots, hat with chin strap, sunscreen, camera")
                .seasonAvailability("Year-round; Best: June-October (optimal weather)")
                .tags("Horse Riding, Equestrian, Adventure, Wildlife, Unique Safari, Romance, Photography")
                .safetyInformation("Follow riding instructor guidance. Wear helmet at all times. Mount and dismount only when instructed. Keep safe distances from wildlife. Do not make sudden movements or loud noises. Inform guides of riding experience level.")
                .isActive(true)
                .build();
    }

    private Activity createCyclingActivity() {
        return Activity.builder()
                .name("Cycling")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Explore parks and conservation areas by mountain bike, combining exercise with wildlife viewing.")
                .detailedDescription("Cycling safaris offer an active, eco-friendly way to explore Tanzania's parks and conservation areas. Riding quality mountain bikes on designated trails, you'll cover more ground than walking while maintaining an intimate connection with nature. Guided rides typically last 2-4 hours and traverse varied terrain including forest trails, lakeshores, and grassland paths. The activity combines physical exercise with wildlife viewing - you'll see animals, birds, and unique vegetation while getting a workout. Cycling is quieter than vehicles, allowing closer approaches to some species. Routes are designed for various fitness levels from easy lakeside paths to challenging hill climbs. Guides share information about ecosystems, wildlife, and conservation. The experience includes stops for photos, refreshments, and wildlife observation. All equipment including bikes, helmets, and repair kits are provided.")
                .minimumAge(14)
                .maximumParticipants(10)
                .equipmentRequired("Comfortable cycling clothes, closed sports shoes, helmet (provided), sunscreen, sunglasses, water bottle")
                .seasonAvailability("Year-round; Best: June-October (dry trails, cooler temperatures)")
                .tags("Cycling, Mountain Biking, Adventure, Fitness, Eco-Tourism, Active Safari")
                .safetyInformation("Wear helmet at all times. Follow guide instructions regarding trail conditions and wildlife. Maintain group pace. Check bike brakes before starting. Stay hydrated. Avoid cycling near dangerous wildlife. Carry basic first aid.")
                .isActive(true)
                .build();
    }

    private Activity createBoatingActivity() {
        return Activity.builder()
                .name("Boating")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_GROUP)
                .description("Cruise along rivers and lakes in motorized boats, perfect for wildlife viewing and sunset experiences.")
                .detailedDescription("Boat safaris provide comfortable wildlife viewing from motorized vessels on Tanzania's rivers and lakes. Popular in Nyerere National Park (Selous), Lake Manyara, and along the Rufiji River, these cruises offer excellent opportunities to see hippos, crocodiles, elephants coming to drink, and an abundance of waterbirds. Boats accommodate 6-12 passengers and trips last 2-4 hours. Morning cruises focus on wildlife and bird activity, while sunset cruises offer spectacular photography opportunities with animals silhouetted against colorful skies. You'll see fish eagles diving for prey, monitor lizards on riverbanks, and pods of hippos wallowing in the water. Some boat safaris include fishing opportunities. The stable platform is excellent for photography, and the quiet electric motors allow close approaches to wildlife without disturbance. Refreshments are usually provided on longer cruises.")
                .minimumAge(5)
                .maximumParticipants(12)
                .equipmentRequired("Sunscreen, hat, sunglasses, camera, binoculars, light jacket for wind, water bottle")
                .seasonAvailability("Year-round; Sunset cruises particularly stunning in dry season (June-October)")
                .tags("Boat Safari, River Cruise, Hippos, Crocodiles, Sunset, Birdwatching, Photography, Relaxing")
                .safetyInformation("Remain seated while boat is moving. Keep hands inside the boat at all times. Do not lean over the sides. Follow guide instructions regarding safe distances from hippos. Lifejackets are available and should be worn by non-swimmers.")
                .isActive(true)
                .build();
    }

    private Activity createRockClimbingActivity() {
        return Activity.builder()
                .name("Rock Climbing")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Challenge yourself with rock climbing in parks featuring rugged landscapes and dramatic cliff formations, particularly in Udzungwa Mountains National Park.")
                .detailedDescription("Rock climbing in Tanzania's national parks offers thrilling vertical adventures in stunning natural settings. Udzungwa Mountains National Park is the premier destination, featuring dramatic escarpments, rock faces, and boulder fields with routes ranging from beginner-friendly to expert-level technical climbs. The park's unique geology creates excellent climbing conditions with solid rock formations and varied route options. Climbs range from sport climbing on bolted routes to traditional climbing requiring full gear placement, and bouldering on massive granite blocks. Professional guides assess climber abilities and recommend appropriate routes. Climbs typically last 4-6 hours including instruction, safety briefings, and multiple ascent attempts. The experience combines physical challenge with spectacular views across the Eastern Arc Mountains and opportunities to see unique wildlife including endemic primates visible from cliff faces. Some routes pass through forest canopies offering unusual perspectives on the ecosystem. All climbing follows Leave No Trace principles to protect fragile cliff ecosystems. Guides provide all necessary technical equipment including ropes, harnesses, helmets, and climbing shoes.")
                .minimumAge(12)
                .maximumParticipants(6)
                .equipmentRequired("Provided: helmet, harness, climbing shoes, ropes, carabiners, belay devices. Bring: athletic clothing, closed-toe shoes for approach hikes, water bottle, sunscreen, climbing gloves (optional)")
                .seasonAvailability("Year-round; Best: June-October and January-February (dry seasons, better rock conditions and grip)")
                .tags("Rock Climbing, Adventure, Udzungwa Mountains, Mountaineering, Technical Climbing, Bouldering, Extreme Sports")
                .safetyInformation("Previous climbing experience recommended but not mandatory - beginners receive thorough instruction. Follow all safety protocols. Use all provided safety equipment correctly. Inform guides of any medical conditions. Stay hydrated. Check weather conditions - climbing cancelled in rain or high winds. Proper physical fitness required.")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // PROFESSIONAL SERVICES (IDs 8, 9)
    // =====================================================================

    private Activity createFilmingActivity() {
        return Activity.builder()
                .name("Filming")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_DAY)
                .description("Professional or amateur filming permits for documentaries, commercial projects, or personal videos within park boundaries.")
                .detailedDescription("Filming in Tanzania's national parks requires special permits beyond standard entrance fees, with different categories for commercial productions, documentaries, student projects, and personal filming. Commercial filming (for broadcast, advertising, or sale) requires comprehensive permits obtained weeks in advance through Tanzania National Parks Authority (TANAPA) or relevant conservation authorities. Documentary filmmakers must submit detailed proposals including project scope, intended use, crew size, and equipment lists. Permits specify allowed filming locations, times, and any restrictions to protect wildlife and minimize disturbance. Professional productions may require ranger escorts and may be restricted during sensitive periods like calving seasons. Drone filming has strict regulations and is prohibited in many areas without special authorization. Amateur filming for personal use (wedding videos, travel vlogs) typically requires simpler permits with lower fees. All filming must adhere to ethical guidelines - no harassment of wildlife, no manipulation of animal behavior, and no filming of sensitive conservation operations. Revenue from filming permits supports conservation and can provide significant funding for park management.")
                .minimumAge(18)
                .maximumParticipants(30)
                .equipmentRequired("Filming equipment, permit documentation, letter of intent (for commercial), insurance documentation, equipment list")
                .seasonAvailability("Year-round; Advance booking required for commercial projects (4-8 weeks); Some restrictions during sensitive wildlife periods")
                .tags("Filming, Photography, Documentary, Commercial Production, Permits, Media, Conservation")
                .safetyInformation("Maintain safe distances from wildlife while filming. Do not use artificial lights that disturb animals. Follow all park regulations. Secure equipment during transport. Have insurance coverage for crew and equipment. Respect wildlife and environmental protocols.")
                .isActive(true)
                .build();
    }

    private Activity createOthersActivity() {
        return Activity.builder()
                .name("Others")
                .hasTariff(true)
                .isWebActive(false)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Any other park activities not explicitly listed but available and permitted within the park, subject to park regulations and additional fees.")
                .detailedDescription("The 'Others' activity category, based on the TANAPA (Tanzania National Parks Authority) reservation system, encompasses any park activities that have associated costs but are not explicitly listed in the standard activity offerings. This flexible category allows parks to offer unique, specialized, or newly introduced activities while maintaining proper tracking and fee collection. Examples may include specialized research activities, unique seasonal programs, special conservation experiences, educational workshops, or park-specific activities that don't fit standard categories. All activities under this category must comply with park regulations, conservation guidelines, and safety standards. Fees are determined by the specific activity and park management. Visitors should inquire at park headquarters or visitor centers about available 'Other' activities, their costs, requirements, and booking procedures. This category ensures comprehensive coverage of all fee-generating activities while maintaining flexibility for parks to introduce innovative visitor experiences.")
                .minimumAge(0)
                .maximumParticipants(999)
                .equipmentRequired("Varies by specific activity - consult with park authorities")
                .seasonAvailability("Year-round; Availability depends on specific activity and park")
                .tags("Miscellaneous, Special Activities, Park-Specific, TANAPA, Flexible Category, Custom Activities")
                .safetyInformation("Follow all park regulations and activity-specific safety guidelines. Consult with park rangers regarding requirements, restrictions, and safety protocols for the specific activity.")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // SPECIALIZED WILDLIFE ACTIVITIES (IDs 14, 26, 22)
    // =====================================================================

    private Activity createRhinoViewingActivity() {
        return Activity.builder()
                .name("Rhino Viewing")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Participate in conservation efforts by tracking and viewing black rhinos on foot in protected sanctuaries.")
                .detailedDescription("Rhino tracking and viewing experiences offer rare opportunities to observe critically endangered black rhinos in protected areas like Ngorongoro Crater. These specialized activities support conservation efforts through fees that fund anti-poaching operations and habitat protection. Guided by expert rangers who monitor rhino populations, you'll track these magnificent animals on foot or by vehicle, learning about individual rhinos, their behaviors, and conservation challenges. Black rhinos are extremely rare with fewer than 5,500 remaining in Africa, making each sighting precious. The experience includes education about rhino biology, threats from poaching, breeding programs, and success stories. Tracking sessions typically last 2-4 hours and success rates vary by season and location. Rangers use radio telemetry and traditional tracking methods to locate rhinos. The activity combines wildlife viewing with conservation education, helping visitors understand the critical importance of protecting these endangered species.")
                .minimumAge(16)
                .maximumParticipants(6)
                .equipmentRequired("Sturdy hiking boots, neutral-colored clothing, hat, sunscreen, binoculars, camera with telephoto lens")
                .seasonAvailability("Year-round; Success rate varies by season")
                .tags("Rhino, Conservation, Endangered Species, Tracking, Wildlife, Protection, Ngorongoro")
                .safetyInformation("Follow ranger instructions at all times. Maintain safe distances from rhinos - they can charge if threatened. Remain quiet and avoid sudden movements. Do not use flash photography. Stay downwind to avoid detection. Rhinos have poor eyesight but excellent hearing and smell.")
                .isActive(true)
                .build();
    }

    private Activity createWildlifeTrackingActivity() {
        return Activity.builder()
                .name("Wildlife Tracking")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Learn the ancient art of tracking wildlife with expert guides, following specific animals like lions, elephants, or rhinos through their natural habitats.")
                .detailedDescription("Wildlife tracking is an immersive experience that teaches the traditional skills used by indigenous trackers and modern conservationists to locate and monitor animals. Led by expert trackers with years of field experience, you'll learn to read signs invisible to untrained eyes - fresh footprints in soft soil, bent grass indicating an animal's path, scratch marks on trees, dung composition and freshness, disturbed vegetation, and scent markers. Trackers teach you to estimate when an animal passed through an area, identify individual animals by unique spoor characteristics, and understand territorial behaviors. The experience typically focuses on tracking specific species - lion prides, elephant herds, black rhinos, or leopards - following their movements from dawn. You'll learn about animal behavior patterns, preferred habitats, feeding signs, and social structures. Tracking combines intellectual challenge with physical activity, requiring observation skills, logical deduction, and patience. Modern tracking incorporates technology like GPS collars and radio telemetry for conservation research, which you may observe. The activity provides deep insights into predator-prey dynamics and ecosystem health. Successful tracks that lead to sightings create profound satisfaction beyond typical game drives.")
                .minimumAge(14)
                .maximumParticipants(6)
                .equipmentRequired("Comfortable walking boots, neutral-colored clothing (avoid bright colors), hat, sunscreen, binoculars, camera, water bottle, notebook for learning tracking signs")
                .seasonAvailability("Year-round; Best: June-October (dry season - clearer tracks, concentrated wildlife); Early morning sessions most successful")
                .tags("Wildlife Tracking, Conservation, Animal Behavior, Bushcraft, Expert Guides, Predators, Footprints, Field Skills")
                .safetyInformation("Always stay with your tracker. This activity involves close wildlife encounters - follow all instructions immediately. Maintain silence when tracking. Be prepared for long walks (5-10km) over varied terrain. Inform guides of fitness levels and any limitations. Wear sturdy footwear. Carry adequate water. Potentially dangerous wildlife may be encountered - tracker safety protocols are mandatory.")
                .isActive(true)
                .build();
    }

    private Activity createBirdWatchingActivity() {
        return Activity.builder()
                .name("Bird Watching")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Discover Tanzania's incredible avian diversity with over 1,100 species including numerous endemic and migratory birds.")
                .detailedDescription("Tanzania is one of Africa's premier bird watching destinations with over 1,100 recorded species including endemics found nowhere else. Bird watching safaris are led by expert ornithological guides who help identify species by sight, call, and behavior. Popular birding locations include Lake Manyara (flamingos, pelicans), Tarangire (over 550 species), Serengeti (raptors, secretary birds), and Ngorongoro Crater (crowned cranes, kori bustards). Specialized birding trips focus on specific habitats - wetlands for waders and waterfowl, forests for turacos and hornbills, grasslands for larks and pipits. The experience typically lasts 3-4 hours during peak bird activity (early morning and late afternoon). Guides maintain checklists helping you track sightings. You'll learn bird calls, identification techniques, migration patterns, and breeding behaviors. Some trips target rare species like the endemic Usambara eagle-owl or migratory steppe eagles. Equipment includes quality binoculars and field guides, though bringing your own optics is recommended for serious birders.")
                .minimumAge(10)
                .maximumParticipants(8)
                .equipmentRequired("Binoculars (essential), bird field guide, camera with telephoto lens, notebook, hat, sunscreen")
                .seasonAvailability("Year-round; Palearctic migrants present November-April; Resident breeders best October-April")
                .tags("Birdwatching, Ornithology, Endemic Species, Flamingos, Nature, Photography, Conservation")
                .safetyInformation("Stay with the group. Watch footing while looking up at birds. Bring sun protection for extended outdoor periods. Stay hydrated. Avoid disturbing nesting birds. Use field guide apps quietly to avoid disturbing wildlife.")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // NGORONGORO CRATER ACTIVITIES (IDs 15, 18-20)
    // =====================================================================

    private Activity createCraterServiceActivity() {
        return Activity.builder()
                .name("Crater Service")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_VEHICLE)
                .description("Descend into the world's largest intact volcanic caldera for unparalleled wildlife viewing in Ngorongoro Crater.")
                .detailedDescription("The Ngorongoro Crater descent is one of Africa's most spectacular wildlife experiences. Descending 600 meters into the world's largest intact volcanic caldera, you enter a natural amphitheater spanning 260 square kilometers and hosting over 25,000 large animals. The crater floor contains diverse habitats including grasslands, swamps, acacia woodlands, and Lake Magadi. This is one of few places where you can see all Big Five in a single day - lions, leopards, elephants, buffalo, and the critically endangered black rhino. The enclosed ecosystem means wildlife is concentrated and visible year-round. Morning descents offer the best lighting and animal activity. The experience includes picnic lunches beside hippo pools, watching flamingos on the lake, and observing the rare interaction between species in this unique environment. Only registered vehicles can descend, and time on the crater floor is limited to preserve the fragile ecosystem. The steep descent and ascent roads provide spectacular viewpoints for photography.")
                .minimumAge(5)
                .maximumParticipants(6)
                .equipmentRequired("Warm layers, rain jacket, camera with good zoom, binoculars, picnic lunch, water, sunscreen")
                .seasonAvailability("Year-round; Wildlife concentrated year-round due to enclosed ecosystem")
                .tags("Ngorongoro, Crater, Big Five, Black Rhino, Caldera, World Heritage, Spectacular Views")
                .safetyInformation("Remain in vehicle at all times except at designated picnic sites. Follow guide instructions. Road conditions can be challenging - hold on during descent/ascent. Weather can change rapidly - bring warm clothing. Keep all food secured from wildlife.")
                .isActive(true)
                .build();
    }

    private Activity createOlduvaiGorgeActivity() {
        return Activity.builder()
                .name("Olduvai Gorge")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Explore the \"Cradle of Mankind\" where some of the earliest human ancestor fossils were discovered.")
                .detailedDescription("Olduvai Gorge, often called the \"Cradle of Mankind,\" is one of the world's most important paleoanthropological sites. Located in the Ngorongoro Conservation Area, this steep-sided ravine has yielded fossils and stone tools dating back 2 million years, providing crucial evidence of human evolution. Your visit includes the small museum displaying replica fossils, stone tools, and information about Louis and Mary Leakey's groundbreaking discoveries. Expert guides explain the significance of findings including Homo habilis, Australopithecus boisei, and various animal fossils. You'll walk along the gorge rim, viewing the different geological layers representing millions of years of history. The site offers spectacular views and fascinating insights into human origins. The visit typically lasts 1.5-2 hours and combines education with stunning landscapes. Ongoing excavations occasionally add new discoveries.")
                .minimumAge(8)
                .maximumParticipants(30)
                .equipmentRequired("Comfortable walking shoes, hat, sunscreen, camera, water bottle, notebook for educational information")
                .seasonAvailability("Year-round")
                .tags("Archaeology, Human Evolution, Olduvai, Leakey, History, Education, Science, Fossils")
                .safetyInformation("Stay on designated paths. Do not remove any rocks or materials. Follow museum rules regarding photography. Bring sun protection as there is limited shade. Respect the archaeological significance of the site.")
                .isActive(true)
                .build();
    }

    private Activity createShiftingSandActivity() {
        return Activity.builder()
                .name("Shifting Sand")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Witness the mysterious moving sand dunes that slowly travel across the Ngorongoro plains.")
                .detailedDescription("The Shifting Sands are a unique geological phenomenon near Olduvai Gorge consisting of volcanic ash dunes that gradually move across the landscape. These crescent-shaped barchan dunes, formed from volcanic ash from the Ol Doinyo Lengai volcano, migrate westward approximately 17 meters per year driven by prevailing winds. The dunes hold cultural significance for the Maasai people, who use them as navigational landmarks and incorporate them into traditional ceremonies. Visits typically last 30-45 minutes and include short walks around the dunes for photographs and Maasai guide explanations about the geological processes and cultural importance. The stark black volcanic sands contrasting against the surrounding grasslands create dramatic photo opportunities. Some tours include Maasai cultural demonstrations near the sands. The location offers expansive views of the Ngorongoro Conservation Area landscape. This activity is often combined with Olduvai Gorge visits as they're in close proximity.")
                .minimumAge(6)
                .maximumParticipants(25)
                .equipmentRequired("Comfortable walking shoes, hat, sunscreen, sunglasses, camera, water bottle, scarf to protect from blowing sand")
                .seasonAvailability("Year-round")
                .tags("Geology, Shifting Sands, Ngorongoro, Maasai Culture, Unique Phenomenon, Photography, Volcanic Ash")
                .safetyInformation("Protect eyes and camera equipment from blowing sand. Stay with the group. Bring sun protection - minimal shade available. Respect Maasai cultural sites. Follow guide instructions regarding safe areas for walking.")
                .isActive(true)
                .build();
    }

    private Activity createLaetoliFootprintsActivity() {
        return Activity.builder()
                .name("Laetoli Footprints")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Visit the site of 3.6 million-year-old hominid footprints preserved in volcanic ash.")
                .detailedDescription("The Laetoli footprints are among the most remarkable paleoanthropological discoveries ever made. Preserved in volcanic ash 3.6 million years ago, these footprints provide definitive evidence that early human ancestors walked upright. Located near Olduvai Gorge, visits to the actual footprint site require special arrangements and are limited to preserve the delicate fossil evidence. Most visitors view detailed replicas at the Olduvai Museum and learn about the discovery's significance through expert presentations. The footprints were made by three individuals - possibly a family group - walking across wet volcanic ash after an eruption. When the ash hardened, it preserved their footsteps for millions of years. The discovery revolutionized understanding of human evolution, proving bipedalism evolved earlier than previously thought. Guides explain the scientific methods used to date and interpret the footprints, the significance of gait patterns, and what the tracks reveal about early hominid behavior and social structure.")
                .minimumAge(10)
                .maximumParticipants(20)
                .equipmentRequired("Comfortable walking shoes, hat, sunscreen, camera, water bottle, interest in human evolution")
                .seasonAvailability("Year-round (museum); Special site visits by arrangement only")
                .tags("Archaeology, Human Evolution, Footprints, Science, Education, Fossils, History, Laetoli")
                .safetyInformation("Follow all conservation guidelines. Photography may be restricted at the actual site. Do not touch any fossil material. Respect the scientific importance of the location. Listen to educational briefings. Special site visits require advance permission.")
                .isActive(true)
                .build();
    }

    // =====================================================================
    // AERIAL & SPECIAL EXPERIENCES (IDs 17, 21, 23-24)
    // =====================================================================

    private Activity createHotAirBalloonActivity() {
        return Activity.builder()
                .name("Hot Air Balloon")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Float silently over the Serengeti plains at sunrise, enjoying breathtaking aerial views of wildlife and landscapes.")
                .detailedDescription("A hot air balloon safari is an unforgettable bucket-list experience. Beginning before dawn, you'll watch the balloon inflation before climbing into the basket for a magical hour-long flight over the Serengeti or Tarangire. As the sun rises, casting golden light across the plains, you'll drift silently at treetop level, witnessing wildlife from a unique perspective - herds of elephants, grazing wildebeest, giraffes running beneath you, and predators on the hunt. The pilot can adjust altitude from just above the ground to several hundred feet, providing varied perspectives. The flight covers 10-20 kilometers depending on wind conditions. After landing, celebrate with a champagne breakfast in the bush, a tradition dating back to the first balloon flights. Professional photographers capture your experience, and you'll receive a flight certificate. This is one of the most spectacular and romantic ways to experience Tanzania's wilderness.")
                .minimumAge(7)
                .maximumParticipants(16)
                .equipmentRequired("Warm layers (early morning can be cold), sturdy shoes, hat, sunglasses, camera, sunscreen")
                .seasonAvailability("Year-round; Best: June-October, January-February (optimal weather and wildlife viewing)")
                .tags("Hot Air Balloon, Aerial Safari, Serengeti, Sunrise, Luxury, Romance, Photography, Champagne Breakfast")
                .safetyInformation("Follow all pilot instructions. Remain standing during flight - no sitting allowed. Hold on during landing which can be bumpy. Pregnant women and people with serious back problems should not participate. Minimum height restrictions apply for children. Weather dependent - flights may be cancelled for safety.")
                .isActive(true)
                .build();
    }

    private Activity createGuidedLecturesActivity() {
        return Activity.builder()
                .name("Guided Lectures")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_SESSION)
                .description("Attend educational presentations by expert naturalists covering ecology, conservation, and wildlife behavior.")
                .detailedDescription("Guided nature lectures provide in-depth education about Tanzania's ecosystems, wildlife, and conservation challenges. Presented by expert naturalists, park rangers, researchers, and conservationists, these sessions cover topics including animal behavior, ecological relationships, conservation success stories, anti-poaching efforts, climate change impacts, and community-based conservation. Lectures typically last 45-60 minutes and may include multimedia presentations, specimen displays, and Q&A sessions. Topics are tailored to current wildlife events (migration timing, recent sightings, breeding seasons) and visitor interests. Some lectures focus on specific subjects like elephant communication, predator-prey dynamics, bird migration, or the role of scavengers in ecosystems. These educational experiences enhance safari understanding and appreciation. Many lodges include evening lectures as part of the safari experience. Lectures may be conducted at visitor centers, lodge common areas, or outdoor amphitheaters. Special lectures by visiting researchers or conservationists provide cutting-edge insights into ongoing projects.")
                .minimumAge(8)
                .maximumParticipants(30)
                .equipmentRequired("Notebook, camera for presentation materials, curiosity and questions")
                .seasonAvailability("Year-round; Often scheduled in afternoon/evening during down time between game drives")
                .tags("Education, Conservation, Ecology, Wildlife, Lectures, Learning, Nature, Science, Expert Presentations")
                .safetyInformation("Arrive on time. Turn off mobile devices. Ask questions during designated time. Take notes for later reference. Photography of slides may be restricted - ask permission first.")
                .isActive(true)
                .build();
    }

    private Activity createCulturalToursActivity() {
        return Activity.builder()
                .name("Cultural Tours")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_GROUP)
                .description("Experience authentic Tanzanian culture through visits to Maasai, Hadzabe, Datoga, and other traditional communities.")
                .detailedDescription("Cultural village tours provide meaningful interactions with Tanzania's indigenous communities, offering insights into traditional lifestyles that have endured for centuries. Maasai village visits are most common, where you'll participate in traditional dances, learn about cattle herding, visit homes (bomas), observe traditional medicine practices, and purchase authentic crafts. Hadzabe visits with the last remaining hunter-gatherer tribe include demonstrations of bow-making, traditional hunting techniques, and gathering honey. Datoga communities showcase metalworking and jewelry-making. Tours last 2-3 hours and are conducted respectfully with community consent, ensuring tourism benefits local people directly. You'll learn about social structures, ceremonies, traditional clothing, food preparation, and challenges of maintaining culture in modern times. Photography is usually permitted, and guides facilitate communication. These experiences foster cultural understanding and support community development through fair-trade craft purchases and direct financial contributions to communities.")
                .minimumAge(6)
                .maximumParticipants(15)
                .equipmentRequired("Respectful clothing (covering shoulders and knees), camera, cash for purchasing crafts, water bottle, hat")
                .seasonAvailability("Year-round")
                .tags("Culture, Maasai, Hadzabe, Datoga, Traditional, Community, Crafts, Dance, Education, Indigenous Peoples")
                .safetyInformation("Dress respectfully. Ask permission before photographing people. Do not give children candy or money directly - give to community leaders. Purchase crafts to support the community. Listen attentively and show respect for different customs. Follow guide instructions regarding appropriate behavior and cultural sensitivities.")
                .isActive(true)
                .build();
    }

    private Activity createPhotographySafarisActivity() {
        return Activity.builder()
                .name("Photography Safaris")
                .hasTariff(true)
                .isWebActive(true)
                .chargingBasis(ChargingBasis.PER_PERSON)
                .description("Improve your wildlife photography skills with professional guidance during dedicated photo safaris.")
                .detailedDescription("Photography safaris are specialized experiences designed for photographers of all levels, led by professional wildlife photographers who provide technical instruction and creative guidance. These safaris use specially modified vehicles with bean bags, roof hatches, and optimal positioning for photography. Guides understand animal behavior and positioning for best lighting, helping you capture dramatic shots. Sessions cover camera settings for action shots, proper exposure in harsh light, composition techniques, and using natural light effectively. Morning and evening drives target golden hour lighting. Smaller group sizes (maximum 4-6 photographers) ensure everyone gets optimal shooting positions. Specialized photography safaris may focus on specific subjects - birds in flight, big cats hunting, the Great Migration. Evening reviews allow sharing images and receiving constructive feedback. Advanced workshops cover post-processing techniques. Equipment advice helps you maximize your gear's potential. These experiences significantly improve photography skills while ensuring you don't miss spectacular shots.")
                .minimumAge(16)
                .maximumParticipants(6)
                .equipmentRequired("DSLR or mirrorless camera, telephoto lens (300mm+ recommended), wide-angle lens, extra batteries, multiple memory cards, lens cleaning kit, laptop for reviews (optional)")
                .seasonAvailability("Year-round; Great Migration timing (July-October) offers spectacular opportunities; Calving season (January-February) excellent for predator action")
                .tags("Photography, Wildlife Photography, Workshop, Education, Professional Guidance, Great Migration, Technical Skills")
                .safetyInformation("Secure camera equipment in vehicles. Protect gear from dust with covers/bags. Never prioritize a shot over safety. Follow guide instructions about positioning. Respect wildlife - do not encourage unnatural behavior for photos. Back up images regularly. Bring sufficient batteries and memory cards.")
                .isActive(true)
                .build();
    }
}
