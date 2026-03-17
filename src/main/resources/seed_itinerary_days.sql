-- =============================================
-- SECTION 4: Itinerary Days + Day Parks
-- =============================================

-- ============================================================
-- ITINERARY 0: Tanzania Safari & Zanzibar Beach Holiday (16 days, 15 nights)
-- ============================================================

-- Day 1: Arrival
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 1, 'Day 1', 'Arrival in Tanzania',
    'Upon arrival at Kilimanjaro International Airport (JRO), transfer to Arusha lodge. Rest and acclimatize.',
    NULL, NULL, NULL, NULL, NULL, NULL,
    'Kilimanjaro Airport (JRO)', 'Arusha', NULL, 1, 'D', @now, @now);
SET @day_0_1 = LAST_INSERT_ID();

-- Day 2: Arusha NP
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 2, 'Day 2', 'Arusha National Park Safari',
    'Full-day game drive in Arusha National Park exploring Ngurdoto Crater, Momella Lakes, and Mount Meru forests.',
    'Game drive through Ngurdoto Crater and montane forest',
    'Momella Lakes and continued game drive',
    NULL,
    'Black-and-white colobus monkeys, giraffes, buffaloes, flamingos, blue monkeys',
    NULL, NULL,
    'Arusha', 'Arusha', NULL, 1, 'B,L,D', @now, @now);
SET @day_0_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at)
VALUES (@day_0_2, @park_arusha, 'DAY_TRIP', 1, '07:00', '17:00', NULL, @now);

-- Day 3: Arusha to Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 3, 'Day 3', 'Arusha to Tarangire National Park',
    'Drive to Tarangire National Park. Game drives among giant elephant herds and ancient baobab trees.',
    'Transfer from Arusha (2-3 hours)',
    'Game drives in Tarangire - elephants, lions, leopards',
    NULL,
    'Large elephant herds (300+), lions, leopards, cheetahs, giraffes, 550+ bird species',
    NULL, NULL,
    'Arusha', 'Tarangire Area', 120, 1, 'B,L,D', @now, @now);
SET @day_0_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at)
VALUES (@day_0_3, @park_tarangire, 'DAY_TRIP', 1, '10:30', '17:30', NULL, @now);

-- Day 4: Full day Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 4, 'Day 4', 'Full Day Safari in Tarangire National Park',
    'Comprehensive exploration of Tarangire with full day of game drives in different sections.',
    'Early morning game drive - deep exploration',
    'Continued game drives - elephants bathing, predator viewing',
    NULL,
    'Elephant breeding herds, tree-climbing pythons, predator-prey interactions',
    NULL, NULL,
    'Tarangire Area', 'Tarangire Area', NULL, 1, 'B,L,D', @now, @now);
SET @day_0_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at)
VALUES (@day_0_4, @park_tarangire, 'DAY_TRIP', 1, '06:00', '18:00', NULL, @now);

-- Day 5: Tarangire to Lake Manyara
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 5, 'Day 5', 'Tarangire to Lake Manyara National Park',
    'Full-day game drive in Lake Manyara, famous for tree-climbing lions and flamingos.',
    'Transfer to Lake Manyara (1.5-2 hours)',
    'Game drives - groundwater forest, acacia woodland, lake shore',
    NULL,
    'Tree-climbing lions, flamingos, baboons, blue monkeys, elephants, hippos',
    NULL, NULL,
    'Tarangire Area', 'Manyara Area', 70, 1, 'B,L,D', @now, @now);
SET @day_0_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at)
VALUES (@day_0_5, @park_manyara, 'DAY_TRIP', 1, '09:30', '17:30', NULL, @now);

-- Day 6: Lake Eyasi Cultural Experience (no park visit — standalone activities)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 6, 'Day 6', 'Lake Eyasi Cultural Experience',
    'Visit the Hadzabe (Hadza) hunter-gatherers and Datoga blacksmith tribe.',
    'Early morning hunting expedition with Hadzabe using traditional bows',
    'Visit Datoga tribe, observe metalwork and brass jewelry making',
    NULL, NULL,
    'Lake Eyasi salt lake, Great Rift Valley landscapes',
    'Unique cultural experience with one of Africa''s last hunter-gatherer tribes',
    'Manyara Area', 'Lake Eyasi Area', NULL, 1, 'B,L,D', @now, @now);
SET @day_0_6 = LAST_INSERT_ID();

-- Day 7: Lake Eyasi to Ngorongoro
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 7, 'Day 7', 'Lake Eyasi to Ngorongoro Crater Safari',
    'Full-day game drive in the Ngorongoro Crater - the world''s largest intact volcanic caldera.',
    'Transfer to Ngorongoro, descend 610m into crater',
    'Extensive game drives - Big Five viewing, Lake Magadi, Hippo Pool',
    NULL,
    'Big Five guaranteed - lion, leopard, elephant, buffalo, black rhino. 25,000 large animals.',
    'World''s largest intact volcanic caldera, 19km across, 264 sq km',
    NULL,
    'Lake Eyasi Area', 'Ngorongoro Crater Rim', 100, 1, 'B,L,D', @now, @now);
SET @day_0_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at)
VALUES (@day_0_7, @park_ngorongoro, 'DAY_TRIP', 1, '08:00', '17:00', NULL, @now);

-- Day 8: Ngorongoro to Central Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 8, 'Day 8', 'Ngorongoro to Central Serengeti',
    'Drive to the legendary Serengeti National Park with game viewing en route. Explore Seronera area.',
    'Transfer through Ngorongoro Conservation Area',
    'Game drives in Central Serengeti - kopjes, riverine forests, plains',
    NULL,
    'Lions on kopjes, leopards in riverine areas, cheetahs, elephants, giraffes',
    'Endless savannah plains - ''Serengeti'' means ''endless plains'' in Maasai',
    NULL,
    'Ngorongoro Crater Rim', 'Central Serengeti (Seronera)', 150, 1, 'B,L,D', @now, @now);
SET @day_0_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at)
VALUES (@day_0_8, @park_serengeti, 'SLEEP_OVER', 1, '12:00', NULL, NULL, @now);

-- Day 9: Full Day Serengeti - Great Migration
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 9, 'Day 9', 'Full Day Serengeti: Great Migration & Mara River',
    'Witness the Great Migration and dramatic Mara River crossings in Northern Serengeti.',
    'Early drive to Northern Serengeti / Mara River area',
    'Strategic positioning for river crossings, game drives',
    NULL,
    '2 million wildebeest, 200,000 zebras crossing Mara River. Crocodiles hunting. Predator action.',
    NULL,
    'One of nature''s greatest spectacles - the Great Migration river crossings',
    'Central Serengeti', 'Northern Serengeti', NULL, 1, 'B,L,D', @now, @now);
SET @day_0_9 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at)
VALUES (@day_0_9, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, NULL, @now);

-- Day 10: Serengeti to Lake Natron (no park visit)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 10, 'Day 10', 'Serengeti to Lake Natron',
    'Scenic journey to Lake Natron. Afternoon hike to Ngare Sero Waterfalls.',
    'Transfer from Serengeti through remote Maasai villages',
    'Guided walk to Ngare Sero Waterfalls, swimming in pools',
    NULL, NULL,
    'Ol Doinyo Lengai volcano, alkaline lake with red/pink waters, dramatic escarpment',
    NULL,
    'Northern Serengeti', 'Lake Natron', 200, 1, 'B,L,D', @now, @now);
SET @day_0_10 = LAST_INSERT_ID();

-- Day 11: Full Day Lake Natron (no park visit)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 11, 'Day 11', 'Full Day Lake Natron Exploration',
    'Explore Lake Natron''s unique landscapes, flamingo colonies, and Maasai culture.',
    'Optional Ol Doinyo Lengai climb or lakeshore walks',
    'Birdwatching, Maasai boma visit, waterfall return',
    NULL,
    'Lesser flamingo breeding colonies (2.5 million birds), zebras, wildebeest, Grant''s gazelles',
    'Active carbonatite volcano, soda-encrusted shores, otherworldly landscape',
    NULL,
    'Lake Natron', 'Lake Natron', NULL, 1, 'B,L,D', @now, @now);
SET @day_0_11 = LAST_INSERT_ID();

-- Day 12: Lake Natron to Mto wa Mbu (no park visit)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 12, 'Day 12', 'Lake Natron to Mto wa Mbu',
    'Transfer to Mto wa Mbu village for immersive cultural experience - biking, walking, local life.',
    'Transfer through Rift Valley landscapes',
    'Bicycle tour, market walk, local workshops, traditional lunch with family',
    NULL, NULL,
    'Great Rift Valley escarpment, banana plantations, rice paddies',
    'Over 120 different tribes represented in Mto wa Mbu - cultural melting pot',
    'Lake Natron', 'Mto wa Mbu', 150, 1, 'B,L,D', @now, @now);
SET @day_0_12 = LAST_INSERT_ID();

-- Day 13: Transfer to Zanzibar (no park visit)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 13, 'Day 13', 'Transfer to Zanzibar',
    'Transfer to Arusha Airport, flight to Zanzibar, and check into beach resort.',
    'Leisurely breakfast, transfer to Arusha Airport',
    '1.5 hour flight to Zanzibar, transfer to beach resort',
    NULL, NULL,
    'Aerial views of Tanzania, Indian Ocean coastline',
    NULL,
    'Mto wa Mbu', 'Zanzibar Beach Resort', NULL, 1, 'B', @now, @now);
SET @day_0_13 = LAST_INSERT_ID();

-- Day 14: Zanzibar Beach Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 14, 'Day 14', 'Zanzibar Beach Holiday - Day 1',
    'Leisure day at beach resort with optional excursions available.',
    NULL,
    'Beach relaxation, water sports, spa treatments',
    NULL, NULL,
    'Optional: Stone Town tour, Spice farm visit, Snorkeling, Prison Island',
    NULL,
    'Zanzibar Beach Resort', 'Zanzibar Beach Resort', NULL, 1, 'B', @now, @now);
SET @day_0_14 = LAST_INSERT_ID();

-- Day 15: Zanzibar Beach Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 15, 'Day 15', 'Zanzibar Beach Holiday - Day 2',
    'Leisure day at beach resort',
    NULL, NULL, NULL, NULL, NULL, NULL,
    'Zanzibar Beach Resort', 'Zanzibar Beach Resort', NULL, 1, 'B', @now, @now);
SET @day_0_15 = LAST_INSERT_ID();

-- Day 16: Departure
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_0, 16, 'Day 16', 'Departure from Zanzibar',
    'Final morning in Zanzibar, transfer to airport for international flight home.',
    'Leisurely breakfast, last beach time',
    'Transfer to Zanzibar Airport',
    NULL, NULL, NULL, NULL,
    'Zanzibar Beach Resort', 'Zanzibar Airport', NULL, 0, 'B', @now, @now);
SET @day_0_16 = LAST_INSERT_ID();

-- ============================================================
-- ITINERARY 1: Ngorongoro Crater Discovery (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_1, 1, 'Day 1', 'Ngorongoro Crater Floor Exploration',
    'Depart Arusha early morning and drive to the Ngorongoro Crater rim. Descend 600 meters into the world''s largest intact volcanic caldera for a full day of game viewing among the densest concentration of wildlife in Africa.',
    'Early morning departure from Arusha at 6:00 AM. Drive through the Great Rift Valley escarpment to Ngorongoro Conservation Area. Arrive at the crater rim and descend to the crater floor. Begin morning game drive through the Lerai Forest area, spotting elephants, buffalo, and various antelope species.',
    'Picnic lunch near the Ngoitokitok Springs hippo pool. Continue game drive across the crater floor, visiting Lake Magadi for flamingos, searching for black rhino in the open grasslands, and observing lion prides resting near the marsh areas.',
    'Ascend the crater rim in late afternoon. Drive back to Arusha arriving by evening.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, spotted hyena, zebra, wildebeest, Grant''s and Thomson''s gazelle, jackal, serval cat',
    'Panoramic views from the 600m crater rim, Lerai Forest canopy, Lake Magadi''s pink flamingo shores, the vast crater floor grasslands stretching to the walls',
    'Ngorongoro Crater has a vehicle limit — early arrival ensures the best experience. Bring warm layers for the crater rim (2,300m altitude). Crater floor temperatures are warmer.',
    'Arusha', 'Arusha', 190, 0, 'L', @now, @now);
SET @day_1_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_1_1, @park_ngorongoro, 'DAY_TRIP', 1, '09:00', '17:00', 'Full day crater floor game drive', @now);

-- ============================================================
-- ITINERARY 2: Tarangire Baobab & Elephant Safari (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_2, 1, 'Day 1', 'Tarangire River & Baobab Safari',
    'Drive from Arusha to Tarangire National Park for a full day among ancient baobab trees and massive elephant herds. The Tarangire River draws hundreds of elephants, creating one of northern Tanzania''s most iconic wildlife spectacles.',
    'Depart Arusha at 7:00 AM and drive to Tarangire National Park (2.5 hours). Enter the park and begin morning game drive along the Tarangire River, where elephant herds gather to drink and bathe. Spot tree-climbing pythons and leopards in the ancient baobabs.',
    'Picnic lunch in the shade of a massive baobab tree. Afternoon game drive through the southern circuits, spotting giraffe, zebra, wildebeest, and large herds of buffalo. Visit the swamp areas for waterbird viewing.',
    'Exit the park in late afternoon and drive back to Arusha.',
    'African elephant (largest herds in northern Tanzania), lion, leopard, giraffe, zebra, wildebeest, buffalo, fringe-eared oryx, lesser kudu, python, over 550 bird species',
    'Ancient baobab tree landscapes, Tarangire River valley, savanna grasslands with termite mounds, Masai Steppe views',
    'Tarangire is especially spectacular during the dry season (June-October) when animals concentrate along the river. Bring binoculars for excellent bird watching.',
    'Arusha', 'Arusha', 120, 0, 'L', @now, @now);
SET @day_2_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_2_1, @park_tarangire, 'DAY_TRIP', 1, '09:30', '17:00', 'Full day game drive along Tarangire River', @now);

-- ============================================================
-- ITINERARY 3: Lake Manyara Flamingo Safari (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_3, 1, 'Day 1', 'Lake Manyara — Flamingos & Tree-Climbing Lions',
    'Explore the compact but incredibly diverse Lake Manyara National Park. From the dense groundwater forest teeming with primates to the alkaline lake shore lined with thousands of flamingos, this park offers extraordinary variety in a single day.',
    'Depart Arusha at 7:00 AM. Drive along the Great Rift Valley escarpment to Lake Manyara (3 hours). Enter through the groundwater forest, spotting baboon troops, blue monkeys, and bushbuck. Continue to the open grasslands searching for the famous tree-climbing lions.',
    'Picnic lunch with views of the Rift Valley. Afternoon drive along the lake shore for flamingos, pelicans, and hippo pools. Visit the hot springs area and explore the southern acacia woodland for giraffes and elephants.',
    'Exit the park and drive back to Arusha arriving by evening.',
    'Tree-climbing lions, flamingos, hippo, baboon, blue monkey, elephant, giraffe, buffalo, bushbuck, pelican, fish eagle, over 400 bird species',
    'Great Rift Valley escarpment views, alkaline Lake Manyara stretching to the horizon, lush groundwater forest canopy, hot springs area',
    'Lake Manyara is one of Tanzania''s best birding destinations. The tree-climbing lion behavior is unique to this park and Ishasha in Uganda.',
    'Arusha', 'Arusha', 130, 0, 'L', @now, @now);
SET @day_3_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_3_1, @park_manyara, 'DAY_TRIP', 1, '10:00', '17:00', 'Full day game drive', @now);

-- ============================================================
-- ITINERARY 4: Arusha Wilderness Family Escape (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_4, 1, 'Day 1', 'Arusha National Park Family Adventure',
    'A wonderful family day out at Arusha National Park, just 45 minutes from town. Mount Meru provides a stunning backdrop as children delight in spotting giraffes, zebras, and playful colobus monkeys around the beautiful Momella Lakes.',
    'Short drive from Arusha to the park gate (45 minutes). Morning game drive through the fig tree forest spotting black-and-white colobus monkeys, giraffes, and buffalo. Visit the Ngurdoto Crater viewpoint — a miniature Ngorongoro.',
    'Picnic lunch at the Momella Lakes. Walk along the lake shores spotting flamingos and waterfowl. Optional canopy walkway adventure (suitable for children). Afternoon game drive through open meadows with Mount Meru towering above.',
    'Return to Arusha in late afternoon.',
    'Black-and-white colobus monkey, giraffe, zebra, buffalo, bushbuck, dik-dik, flamingo, African fish eagle, augur buzzard, silvery-cheeked hornbill',
    'Mount Meru''s snow-capped peak, Ngurdoto Crater panorama, Momella Lakes with flamingo reflections, lush montane forest canopy',
    'Perfect for families with young children — gentle terrain, close wildlife encounters, and short driving distances. The canopy walkway is an exciting adventure for kids aged 5+.',
    'Arusha', 'Arusha', 35, 0, 'L', @now, @now);
SET @day_4_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_4_1, @park_arusha, 'DAY_TRIP', 1, '08:30', '16:30', 'Family game drive and Momella Lakes walk', @now);

-- ============================================================
-- ITINERARY 5: Kilimanjaro Rainforest Trek (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_5, 1, 'Day 1', 'Kilimanjaro Rainforest Day Hike',
    'Trek through the lush montane rainforest belt of Kilimanjaro without committing to a full summit climb. The Marangu Route offers a well-maintained trail through a world of giant ferns, moss-draped trees, and endemic wildlife.',
    'Drive from Arusha to Marangu Gate (2 hours). Register and meet your guide. Begin trekking through the rainforest zone, passing through towering tropical trees, giant ferns, and hanging moss. Spot blue monkeys, colobus monkeys, and exotic birds.',
    'Reach Mandara Hut (2,700m) for a packed lunch. Optional side hike to Maundi Crater for panoramic views of the surrounding landscape and, on clear days, the Kibo peak. Begin descent through the rainforest.',
    'Return to Marangu Gate and drive back to Arusha.',
    'Blue monkey, black-and-white colobus monkey, bushbuck, duiker, Hartlaub''s turaco, silvery-cheeked hornbill, various sunbird species',
    'Kilimanjaro rainforest canopy, giant tree ferns, moss-covered ancient trees, Maundi Crater panorama, glimpses of Kibo peak through the clouds',
    'Moderate fitness required. The trek to Mandara Hut is approximately 8km each way. Bring rain gear as the rainforest zone receives frequent afternoon showers.',
    'Arusha', 'Arusha', 95, 0, 'L', @now, @now);
SET @day_5_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_5_1, @park_kilimanjaro, 'DAY_TRIP', 1, '09:00', '16:00', 'Day hike Marangu Gate to Mandara Hut and back', @now);

-- ============================================================
-- ITINERARY 6: Ngorongoro Crater Through the Lens (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_6, 1, 'Day 1', 'Ngorongoro Crater Photography Safari',
    'A premium photography safari into the crater timed for the best light. Early descent for golden hour on the crater floor, extended stops at prime wildlife locations, and expert positioning for dramatic wildlife portraits against the caldera backdrop.',
    'Pre-dawn departure from Arusha at 5:00 AM to catch first light on the crater rim. Descend at gate opening for golden hour photography on the crater floor. Focus on lion prides in the early morning light, black rhino in the open grasslands, and elephant silhouettes against the crater walls.',
    'Extended photography session at Lake Magadi for flamingo reflections and wading birds. Afternoon positioning for big cat activity near the marsh areas. Late afternoon golden hour for landscape shots of the crater walls bathed in warm light.',
    'Final crater rim sunset shots before driving back to Arusha.',
    'Black rhino (best photography location in Tanzania), lion prides, elephant, flamingo, crowned crane, secretary bird, hyena, jackal, buffalo herds',
    'Golden hour crater rim panoramas, flamingo reflections on Lake Magadi, dramatic caldera walls in afternoon light, Lerai Forest atmospheric shots',
    'Bring a telephoto lens (200-400mm minimum) for wildlife and a wide-angle for landscapes. The crater rim provides stunning panoramic shots at sunrise and sunset. Vehicle positioning is key — your guide knows the best angles.',
    'Arusha', 'Arusha', 190, 0, 'L', @now, @now);
SET @day_6_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_6_1, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '17:30', 'Extended photography game drive', @now);

-- ============================================================
-- ITINERARY 7: Mkomazi Rhino & Wilderness Safari (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_7, 1, 'Day 1', 'Mkomazi Rhino Sanctuary & Wilderness Drive',
    'Escape the crowds at remote Mkomazi National Park, where the dramatic Pare and Usambara Mountains frame a wild landscape of dry savanna and riverine bush. Visit the Mkomazi Rhino Sanctuary and search for rare species like African wild dog.',
    'Depart Arusha at 6:30 AM and drive east to Mkomazi National Park (2 hours). Enter the park and begin morning game drive through the Acacia-Commiphora bushland. Visit the Mkomazi Rhino Sanctuary for a guided tour of the black rhino conservation program.',
    'Picnic lunch in the bush. Afternoon game drive through the eastern circuits, searching for lesser kudu, gerenuk, fringe-eared oryx, and African wild dog. Explore the riverine areas for birdlife including the endemic Friedmann''s lark.',
    'Exit the park and drive back to Arusha.',
    'Black rhino (sanctuary), African wild dog, lesser kudu, gerenuk, fringe-eared oryx, eland, hartebeest, dik-dik, Friedmann''s lark, martial eagle',
    'Pare Mountains dramatic backdrop, Usambara Mountain range views, dry savanna landscapes, seasonal river valleys, remote wilderness atmosphere',
    'Mkomazi is one of Tanzania''s least-visited parks — you may have the entire park to yourself. The rhino sanctuary visit requires advance booking. Best visited June to October.',
    'Arusha', 'Arusha', 110, 0, 'L', @now, @now);
SET @day_7_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_7_1, @park_mkomazi, 'DAY_TRIP', 1, '08:30', '16:30', 'Game drive and rhino sanctuary visit', @now);

-- ============================================================
-- ITINERARY 8: Jozani Forest & Spice Island Explorer (1 day, 0 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_8, 1, 'Day 1', 'Jozani Forest & Zanzibar Spice Tour',
    'Discover Zanzibar''s natural and cultural treasures in a single day. Walk through the ancient Jozani Forest to meet the rare red colobus monkey, stroll the mangrove boardwalk, and visit a traditional spice plantation.',
    'Morning drive from Stone Town to Jozani Chwaka Bay National Park (45 minutes). Guided forest walk through the indigenous groundwater forest, encountering troops of the endemic Zanzibar red colobus monkey — one of Africa''s rarest primates. Walk the mangrove boardwalk over Chwaka Bay.',
    'Visit a traditional Zanzibar spice plantation — taste cloves, nutmeg, cinnamon, vanilla, and black pepper fresh from the tree. Learn about Zanzibar''s rich spice trade history. Optional visit to a local village.',
    'Return to Stone Town or beach hotel.',
    'Zanzibar red colobus monkey (endemic), Sykes monkey, Ader''s duiker, bush pig, over 50 butterfly species, mangrove crabs, mudskippers',
    'Ancient indigenous forest canopy, mangrove boardwalk over tidal waters, tropical spice plantation, Chwaka Bay coastal views',
    'The red colobus monkeys are habituated and very approachable — excellent for photography. Wear closed shoes for the forest walk. Spice plantation tours are interactive and engaging for all ages.',
    'Zanzibar', 'Zanzibar', 35, 0, 'L', @now, @now);
SET @day_8_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_8_1, @park_jozani, 'DAY_TRIP', 1, '08:30', '12:00', 'Forest walk and mangrove boardwalk', @now);

-- ============================================================
-- ITINERARY 9: Tarangire & Ngorongoro Express Safari (2 days, 1 night)
-- ============================================================
-- Day 1: Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_9, 1, 'Day 1', 'Tarangire — Land of the Elephants',
    'Drive from Arusha to Tarangire National Park for a full day of game viewing among the ancient baobab forests and the largest elephant herds in northern Tanzania.',
    'Depart Arusha at 7:00 AM. Drive to Tarangire National Park (2.5 hours). Morning game drive along the Tarangire River watching elephant herds drink and bathe.',
    'Picnic lunch under a baobab tree. Afternoon game drive through the southern circuits spotting giraffe, zebra, lion, and tree-climbing pythons. Visit the swamp for waterbirds.',
    'Drive to Ngorongoro area for overnight. Dinner and overnight at crater rim lodge.',
    'Elephant herds, lion, leopard, giraffe, zebra, wildebeest, buffalo, fringe-eared oryx, python, 550+ bird species',
    'Ancient baobab landscapes, Tarangire River valley, termite mound-dotted savanna, Masai Steppe views',
    'The drive from Tarangire to Ngorongoro passes through Mto wa Mbu town — a melting pot of Tanzanian cultures.',
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_9_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_9_1, @park_tarangire, 'DAY_TRIP', 1, '09:30', '16:00', 'Full day game drive', @now);

-- Day 2: Ngorongoro
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_9, 2, 'Day 2', 'Ngorongoro Crater Descent & Return',
    'Descend into the Ngorongoro Crater for a morning of Big Five game viewing, then ascend and drive back to Arusha.',
    'Early morning crater descent at first light. Game drive across the crater floor — search for black rhino, lion prides, and elephant herds. Visit Lake Magadi for flamingos and the Ngoitokitok hippo pool.',
    'Ascend the crater rim after lunch. Drive back to Arusha arriving by late afternoon.',
    NULL,
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, hyena, zebra, wildebeest, jackal',
    'Crater rim panorama at dawn, crater floor grasslands, Lake Magadi pink flamingo shores, Lerai Forest',
    'Early descent is essential for the best wildlife activity and to avoid crowds.',
    'Ngorongoro', 'Arusha', 190, 0, 'B,L', @now, @now);
SET @day_9_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_9_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '13:00', 'Morning crater floor game drive', @now);

-- ============================================================
-- ITINERARY 10: Lake Manyara & Ngorongoro Luxury Retreat (2 days, 1 night)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_10, 1, 'Day 1', 'Lake Manyara — Forest, Flamingos & Lions',
    'Drive from Arusha to Lake Manyara National Park for a full afternoon of game viewing, then continue to your luxury lodge on the Ngorongoro Crater rim.',
    'Late morning departure from Arusha. Drive along the Rift Valley escarpment to Lake Manyara National Park.',
    'Enter the park for afternoon game drive through the groundwater forest and along the lake shore. Search for tree-climbing lions, flamingos, elephants, and hippo pools.',
    'Drive to Ngorongoro Crater rim lodge. Sunset drinks overlooking the crater. Luxury dinner.',
    'Tree-climbing lions, flamingo, elephant, baboon, blue monkey, hippo, giraffe, buffalo',
    'Great Rift Valley escarpment, Lake Manyara shoreline, groundwater forest canopy, crater rim sunset',
    NULL,
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_10_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_10_1, @park_manyara, 'DAY_TRIP', 1, '12:00', '17:00', 'Afternoon game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_10, 2, 'Day 2', 'Ngorongoro Crater Luxury Safari & Return',
    'Full morning exploring the Ngorongoro Crater floor with a luxury picnic, then return to Arusha.',
    'Early descent into the crater. Extended morning game drive focusing on Big Five encounters. Luxury picnic lunch by the hippo pool.',
    'Ascend the crater and drive back to Arusha.',
    NULL,
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, crowned crane, serval',
    'Dawn light on crater walls, Lake Magadi reflections, Lerai Forest elephants, crater rim panorama',
    NULL,
    'Ngorongoro', 'Arusha', 190, 0, 'B,L', @now, @now);
SET @day_10_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_10_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '13:00', 'Luxury morning crater game drive', @now);

-- ============================================================
-- ITINERARY 11: Serengeti Fly-In Exclusive Safari (2 days, 1 night)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_11, 1, 'Day 1', 'Fly into the Heart of the Serengeti',
    'Fly directly from Arusha into the Serengeti for an immediate immersion in the endless plains. Afternoon game drive in the Seronera Valley — the predator capital of Africa.',
    'Morning flight from Arusha airstrip to Seronera Airstrip (1 hour scenic flight). Meet your guide and transfer to luxury tented camp.',
    'Afternoon game drive through the Seronera Valley, home to resident lion prides, leopards in the kopje rocks, and vast herds of wildebeest and zebra.',
    'Sundowner cocktails on the plains. Gourmet dinner at luxury tented camp under the stars.',
    'Lion, leopard, cheetah, elephant, wildebeest, zebra, giraffe, topi, eland, hippo, crocodile',
    'Endless Serengeti plains stretching to the horizon, granite kopje rock formations, Seronera River valley, dramatic African sunset',
    'The bush flight offers spectacular aerial views of the Ngorongoro Crater and Great Rift Valley.',
    'Arusha', 'Serengeti', 0, 1, 'L,D', @now, @now);
SET @day_11_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_11_1, @park_serengeti, 'SLEEP_OVER', 1, '10:00', NULL, 'Fly-in, afternoon game drive, overnight in park', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_11, 2, 'Day 2', 'Serengeti Sunrise Safari & Return Flight',
    'Early morning game drive to catch the Serengeti at its most active, then fly back to Arusha.',
    'Pre-dawn game drive to catch predators on the hunt. Follow lion prides, search for cheetah on the open plains, and watch the Serengeti come alive at sunrise.',
    'Return to camp for breakfast. Transfer to Seronera Airstrip. Afternoon flight back to Arusha.',
    NULL,
    'Lion, cheetah, leopard, hyena, jackal, wildebeest, zebra, Thomson''s gazelle, vultures',
    'Serengeti sunrise over the plains, morning mist on the grasslands, kopje silhouettes',
    NULL,
    'Serengeti', 'Arusha', 0, 0, 'B,L', @now, @now);
SET @day_11_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_11_2, @park_serengeti, 'DAY_TRIP', 1, '06:00', '11:00', 'Dawn game drive before return flight', @now);

-- ============================================================
-- ITINERARY 12: Arusha & Tarangire Family Wildlife Safari (2 days, 1 night)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_12, 1, 'Day 1', 'Arusha National Park Family Safari',
    'A gentle start to the family safari at Arusha National Park, just 45 minutes from town. Children love the close encounters with colobus monkeys and giraffes.',
    'Drive to Arusha National Park. Morning game drive through the forest spotting colobus monkeys, giraffes, and buffalo. Visit Ngurdoto Crater viewpoint.',
    'Picnic lunch at Momella Lakes. Walk along the shore for flamingos. Optional canopy walkway. Afternoon game drive with Mount Meru views.',
    'Drive to Tarangire area lodge. Family dinner and overnight.',
    'Colobus monkey, giraffe, zebra, buffalo, flamingo, bushbuck, dik-dik, fish eagle',
    'Mount Meru backdrop, Ngurdoto Crater, Momella Lakes reflections, montane forest',
    'Short driving distances and gentle terrain make this ideal for families with young children.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_12_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_12_1, @park_arusha, 'DAY_TRIP', 1, '08:30', '15:00', 'Family game drive and lake walk', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_12, 2, 'Day 2', 'Tarangire Elephant Encounters & Return',
    'A full morning in Tarangire watching enormous elephant herds, then return to Arusha.',
    'Morning game drive in Tarangire National Park along the river. Watch elephant families interact, spot giraffes between the baobab trees, and search for lions.',
    'Exit the park after lunch. Drive back to Arusha.',
    NULL,
    'Elephant herds, giraffe, lion, zebra, wildebeest, warthog, vervet monkey, hornbills',
    'Baobab tree landscapes, Tarangire River, savanna grasslands',
    NULL,
    'Tarangire', 'Arusha', 120, 0, 'B,L', @now, @now);
SET @day_12_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_12_2, @park_tarangire, 'DAY_TRIP', 1, '07:00', '13:00', 'Morning game drive', @now);

-- ============================================================
-- ITINERARY 13: Kilimanjaro Foothills & Arusha Adventure (2 days, 1 night)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_13, 1, 'Day 1', 'Kilimanjaro Rainforest Day Hike',
    'Trek through Kilimanjaro''s enchanting rainforest zone to Mandara Hut and back.',
    'Drive from Arusha to Marangu Gate (2 hours). Begin trek through the montane rainforest to Mandara Hut (2,700m). Spot blue monkeys and exotic birds.',
    'Lunch at Mandara Hut. Optional side hike to Maundi Crater. Descend through the forest.',
    'Drive to Arusha area lodge for overnight.',
    'Blue monkey, colobus monkey, bushbuck, duiker, turacos, hornbills',
    'Kilimanjaro rainforest, giant ferns, moss-draped trees, Maundi Crater views',
    'Moderate fitness required. Bring rain gear for the rainforest.',
    'Arusha', 'Arusha', 95, 1, 'L,D', @now, @now);
SET @day_13_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_13_1, @park_kilimanjaro, 'DAY_TRIP', 1, '09:00', '16:00', 'Rainforest day hike to Mandara Hut', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_13, 2, 'Day 2', 'Arusha National Park Wildlife Safari',
    'Explore Arusha National Park for a morning of wildlife viewing with Mount Meru as your backdrop.',
    'Morning game drive in Arusha NP. Visit Momella Lakes for flamingos, spot colobus monkeys in the forest, and enjoy Mount Meru views.',
    'Return to Arusha after lunch.',
    NULL,
    'Colobus monkey, giraffe, buffalo, flamingo, zebra, bushbuck',
    'Mount Meru, Momella Lakes, Ngurdoto Crater, montane forest',
    NULL,
    'Arusha', 'Arusha', 35, 0, 'B,L', @now, @now);
SET @day_13_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_13_2, @park_arusha, 'DAY_TRIP', 1, '07:30', '13:00', 'Morning game drive', @now);

-- ============================================================
-- ITINERARY 14: Serengeti Express — Endless Plains Safari (3 days, 2 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_14, 1, 'Day 1', 'Arusha to the Serengeti Plains',
    'Drive from Arusha through the Ngorongoro highlands to reach the Serengeti by afternoon. En route game viewing as you transit through Lake Manyara and across the Ngorongoro Conservation Area.',
    'Early departure from Arusha. Drive through Mto wa Mbu and past Lake Manyara. Ascend the Ngorongoro highlands with stops for panoramic views.',
    'Cross the Ngorongoro Conservation Area with wildlife spotting en route. Descend into the Serengeti via the Naabi Hill Gate. Afternoon game drive to your camp in the Seronera Valley.',
    'Sundowner and dinner at Serengeti camp.',
    'Wildebeest, zebra, giraffe, topi, eland, lion, hyena along the route',
    'Ngorongoro highlands panorama, descent into Serengeti endless plains, Naabi Hill viewpoint',
    'Long driving day (7-8 hours total) but with game viewing throughout.',
    'Arusha', 'Serengeti', 335, 1, 'L,D', @now, @now);
SET @day_14_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_14_1, @park_manyara, 'TRANSIT', 1, NULL, NULL, 'Pass through Lake Manyara area en route', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_14_1, @park_ngorongoro, 'TRANSIT', 2, NULL, NULL, 'Transit through Ngorongoro Conservation Area', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_14_1, @park_serengeti, 'SLEEP_OVER', 3, '15:00', NULL, 'Afternoon arrival, game drive, overnight', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_14, 2, 'Day 2', 'Full Day Serengeti Game Drives',
    'A full day exploring the Serengeti plains with morning and afternoon game drives in the Seronera Valley.',
    'Dawn game drive in the Seronera Valley. Track big cats on the hunt, visit the hippo pools along the Seronera River, and explore the kopje rock formations where leopards rest.',
    'Picnic lunch in the bush. Afternoon game drive through different sectors of the central Serengeti, following the movement of wildebeest herds and searching for cheetah on the open plains.',
    'Return to camp for sundowner and dinner.',
    'Lion, leopard, cheetah, elephant, hippo, crocodile, wildebeest, zebra, Thomson''s gazelle, topi, eland, hyena, jackal',
    'Endless plains panorama, Seronera River valley, granite kopje formations, vast wildebeest herds',
    'Full day in the park — bring sun protection, plenty of water, and binoculars.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_14_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_14_2, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives, overnight in park', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_14, 3, 'Day 3', 'Serengeti Sunrise & Return to Arusha',
    'Final sunrise game drive in the Serengeti, then drive back to Arusha via the Ngorongoro Conservation Area.',
    'Early morning game drive for last wildlife encounters. Return to camp for breakfast and pack up.',
    'Drive back through the Ngorongoro Conservation Area and down to Arusha, arriving by evening.',
    NULL,
    'Morning predator activity, vultures, wildebeest, zebra',
    'Serengeti sunrise, Ngorongoro highlands views on return',
    NULL,
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_14_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_14_3, @park_serengeti, 'DAY_TRIP', 1, '06:00', '10:00', 'Dawn game drive before departure', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_14_3, @park_ngorongoro, 'TRANSIT', 2, NULL, NULL, 'Transit through on return to Arusha', @now);

-- ============================================================
-- ITINERARY 15: Ngorongoro, Tarangire & Manyara Highlights (3 days, 2 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_15, 1, 'Day 1', 'Tarangire — Elephants & Baobabs',
    'Drive to Tarangire for a full day of game viewing among ancient baobabs and elephant herds.',
    'Drive from Arusha to Tarangire (2.5 hours). Morning game drive along the Tarangire River.',
    'Picnic lunch. Afternoon game drive through southern circuits.',
    'Drive to Lake Manyara area lodge. Dinner and overnight.',
    'Elephant herds, lion, leopard, giraffe, zebra, buffalo, oryx',
    'Baobab landscapes, Tarangire River valley, savanna views',
    NULL,
    'Arusha', 'Manyara', 130, 1, 'L,D', @now, @now);
SET @day_15_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_15_1, @park_tarangire, 'DAY_TRIP', 1, '09:30', '16:30', 'Full day game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_15, 2, 'Day 2', 'Ngorongoro Crater — The Eighth Wonder',
    'Full day in the Ngorongoro Crater with luxury picnic lunch.',
    'Drive to Ngorongoro. Descend into the crater at first light. Morning game drive across the crater floor.',
    'Luxury picnic lunch. Afternoon game drive focusing on black rhino and big cat areas. Ascend in late afternoon.',
    'Overnight at crater rim luxury lodge. Sunset over the caldera.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, hyena, serval',
    'Crater rim panorama, crater floor grasslands, Lake Magadi, Lerai Forest',
    NULL,
    'Manyara', 'Ngorongoro', 60, 1, 'B,L,D', @now, @now);
SET @day_15_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_15_2, @park_ngorongoro, 'SLEEP_OVER', 1, '07:00', '17:00', 'Full day crater, overnight on rim', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_15, 3, 'Day 3', 'Lake Manyara & Return to Arusha',
    'Morning game drive at Lake Manyara, then return to Arusha.',
    'Drive to Lake Manyara NP. Morning game drive through the groundwater forest and along the lake shore.',
    'Exit the park. Drive back to Arusha arriving by afternoon.',
    NULL,
    'Tree-climbing lions, flamingo, baboon, elephant, hippo, blue monkey',
    'Rift Valley escarpment, Lake Manyara shoreline, groundwater forest',
    NULL,
    'Ngorongoro', 'Arusha', 190, 0, 'B,L', @now, @now);
SET @day_15_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_15_3, @park_manyara, 'DAY_TRIP', 1, '07:00', '12:00', 'Morning game drive', @now);

-- ============================================================
-- ITINERARY 16: Serengeti Wildlife Photography Expedition (3 days, 2 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_16, 1, 'Day 1', 'Arrival in the Serengeti — First Light',
    'Drive from Arusha through the highlands and arrive in the Serengeti for late afternoon golden hour photography.',
    'Early departure from Arusha. Drive through the Ngorongoro highlands to the Serengeti.',
    'Arrive Serengeti mid-afternoon. Golden hour game drive — position for big cat portraits and landscape photography in the warm afternoon light.',
    'Overnight at photography-friendly camp with open views.',
    'Lion, wildebeest, zebra, topi, giraffe, hyena',
    'Serengeti plains at golden hour, kopje silhouettes, dramatic skies',
    'Long drive day but rewarded with afternoon photography session.',
    'Arusha', 'Serengeti', 335, 1, 'L,D', @now, @now);
SET @day_16_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_16_1, @park_serengeti, 'SLEEP_OVER', 1, '14:00', NULL, 'Afternoon arrival and golden hour photography', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_16, 2, 'Day 2', 'Full Day Serengeti Photography',
    'Dawn to dusk photography across the Serengeti plains. Extended game drives timed for the best light.',
    'Pre-dawn departure for sunrise on the plains. Morning focused on predator behavior — lion hunts, cheetah stalking, and leopard in kopjes.',
    'Mid-day break at camp. Afternoon session focusing on herd movements, bird photography at the river, and late golden hour landscapes.',
    'Night photography session (if available) for star trails over the Serengeti.',
    'Lion, leopard, cheetah, elephant, hippo, crocodile, wildebeest herds, secretary bird, martial eagle',
    'Sunrise over endless plains, kopje landscapes, Seronera River scenes, dramatic African sky',
    'Full day dedicated to photography — flexible schedule follows the light and wildlife action.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_16_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_16_2, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Dawn to dusk photography', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_16, 3, 'Day 3', 'Serengeti Sunrise & Departure',
    'Final sunrise photography session before departing the Serengeti.',
    'Pre-dawn game drive for sunrise over the plains. Last chance for wildlife photography. Return to camp for breakfast.',
    'Drive back to Arusha via the Ngorongoro highlands.',
    NULL,
    'Sunrise predator activity, birds of prey, plains game',
    'Serengeti sunrise, morning mist, Ngorongoro highlands on return',
    NULL,
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_16_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_16_3, @park_serengeti, 'DAY_TRIP', 1, '05:30', '10:00', 'Dawn photography session', @now);

-- ============================================================
-- ITINERARY 17: Northern Circuit Budget Explorer (3 days, 2 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_17, 1, 'Day 1', 'Lake Manyara — Birds, Lions & Primates',
    'Drive to Lake Manyara for an afternoon of game viewing, then continue to budget campsite near Ngorongoro.',
    'Depart Arusha late morning. Drive to Lake Manyara National Park.',
    'Afternoon game drive through the groundwater forest and lake shore. Search for tree-climbing lions and flamingos.',
    'Drive to Ngorongoro area campsite. Dinner around the campfire.',
    'Tree-climbing lions, flamingo, baboon, elephant, hippo, blue monkey',
    'Rift Valley escarpment, Lake Manyara, groundwater forest',
    NULL,
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_17_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_17_1, @park_manyara, 'DAY_TRIP', 1, '12:00', '17:00', 'Afternoon game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_17, 2, 'Day 2', 'Ngorongoro Crater — Into the Caldera',
    'Full morning in the Ngorongoro Crater, then drive to Tarangire area.',
    'Descend into Ngorongoro Crater at dawn. Game drive across the crater floor — Big Five viewing, flamingos, hippo pools.',
    'Picnic lunch in the crater. Ascend and drive to Tarangire area campsite.',
    'Campfire dinner under the stars near Tarangire.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, zebra, wildebeest',
    'Crater rim panorama, crater floor, Lake Magadi, Lerai Forest',
    NULL,
    'Ngorongoro', 'Tarangire', 190, 1, 'B,L,D', @now, @now);
SET @day_17_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_17_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '14:00', 'Morning and early afternoon crater game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_17, 3, 'Day 3', 'Tarangire Elephants & Return',
    'Morning game drive in Tarangire, then return to Arusha.',
    'Morning game drive in Tarangire along the river. Watch elephant herds, spot giraffes among the baobabs.',
    'Exit the park and drive back to Arusha.',
    NULL,
    'Elephant, giraffe, lion, zebra, wildebeest, oryx, hornbills',
    'Baobab landscapes, Tarangire River valley',
    NULL,
    'Tarangire', 'Arusha', 120, 0, 'B,L', @now, @now);
SET @day_17_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_17_3, @park_tarangire, 'DAY_TRIP', 1, '07:00', '12:00', 'Morning game drive', @now);

-- ============================================================
-- ITINERARY 18: Kilimanjaro Machame — Shira Plateau Trek (3 days, 2 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_18, 1, 'Day 1', 'Machame Gate to Machame Camp',
    'Begin the Machame Route trek through lush montane rainforest to Machame Camp at 3,000m.',
    'Drive from Arusha to Machame Gate (1.5 hours). Register and begin trek through the rainforest zone. Spot blue monkeys, colobus monkeys, and exotic birds in the canopy.',
    'Continue ascending through the forest. Arrive at Machame Camp (3,000m) by late afternoon.',
    'Hot dinner at camp. Acclimatization briefing from guide. Early rest.',
    'Blue monkey, colobus monkey, bushbuck, turacos, sunbirds',
    'Rainforest canopy, giant ferns, moss-draped trees, streams and small waterfalls',
    'Trek distance: 11km, 5-7 hours. Altitude gain: 1,200m. Moderate difficulty.',
    'Arusha', 'Kilimanjaro', 95, 1, 'L,D', @now, @now);
SET @day_18_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_18_1, @park_kilimanjaro, 'SLEEP_OVER', 1, '09:00', NULL, 'Machame Gate to Machame Camp trek', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_18, 2, 'Day 2', 'Machame Camp to Shira Plateau',
    'Ascend from the rainforest through heather moorland to the spectacular Shira Plateau at 3,800m.',
    'Breakfast at camp. Begin ascending through the heath and moorland zone. The vegetation transitions dramatically from forest to shrubs and giant heather.',
    'Arrive at Shira Plateau Camp with stunning views of Kibo peak. Afternoon acclimatization walk.',
    'Dinner at Shira Camp with sunset views over the plateau.',
    'Eland, white-necked raven, alpine chat, four-striped grass mouse',
    'Shira Plateau panorama, Kibo peak views, moorland landscapes, dramatic cloud formations',
    'Trek distance: 5km, 4-6 hours. Altitude gain: 800m. Drink plenty of water for acclimatization.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_18_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_18_2, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Machame Camp to Shira Plateau trek', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_18, 3, 'Day 3', 'Shira Plateau Sunrise & Descent',
    'Watch sunrise over the Shira Plateau, then descend back to Machame Gate via the rainforest.',
    'Early morning sunrise viewing over the plateau with Kibo peak glowing in first light. Breakfast and begin descent.',
    'Trek down through moorland and rainforest to Machame Gate. Drive back to Arusha.',
    NULL,
    'Raptors, alpine birds during descent',
    'Sunrise over Shira Plateau, Kibo peak in morning light, changing vegetation zones on descent',
    'Descent takes 4-5 hours. Knees will be tested — bring trekking poles.',
    'Kilimanjaro', 'Arusha', 95, 0, 'B,L', @now, @now);
SET @day_18_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_18_3, @park_kilimanjaro, 'DAY_TRIP', 1, '06:00', '14:00', 'Descent from Shira to Machame Gate', @now);

-- ============================================================
-- ITINERARY 19: Family Safari — Arusha, Tarangire & Manyara (3 days, 2 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_19, 1, 'Day 1', 'Arusha National Park Family Fun',
    'Begin with kid-friendly Arusha NP — close animal encounters and short drives.',
    'Drive to Arusha NP (45 min). Game drive through the forest spotting colobus monkeys, giraffes. Visit Ngurdoto Crater viewpoint.',
    'Momella Lakes walk for flamingos. Optional canopy walkway. Drive to Tarangire area lodge.',
    'Family dinner at lodge.',
    'Colobus monkey, giraffe, zebra, flamingo, buffalo, dik-dik',
    'Mount Meru, Ngurdoto Crater, Momella Lakes',
    'Short distances and gentle terrain — ideal for young children.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_19_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_19_1, @park_arusha, 'DAY_TRIP', 1, '08:30', '14:00', 'Family game drive and lake walk', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_19, 2, 'Day 2', 'Tarangire — Giants of the Savanna',
    'Full day in Tarangire watching elephant families and exploring baobab forests.',
    'Morning game drive along the Tarangire River. Watch elephant herds — kids will love the baby elephants playing.',
    'Picnic lunch under baobabs. Afternoon game drive for giraffes, zebras, and birds.',
    'Drive to Lake Manyara area. Overnight at family lodge.',
    'Elephant families, giraffe, zebra, lion, warthog, hornbills, starlings',
    'Baobab trees, Tarangire River, savanna grasslands',
    NULL,
    'Tarangire', 'Manyara', 70, 1, 'B,L,D', @now, @now);
SET @day_19_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_19_2, @park_tarangire, 'DAY_TRIP', 1, '07:30', '16:00', 'Full day family game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_19, 3, 'Day 3', 'Lake Manyara Flamingos & Home',
    'Morning at Lake Manyara for flamingos and forest primates, then back to Arusha.',
    'Morning game drive at Lake Manyara. Through the forest for baboons and blue monkeys, along the lake for flamingos and hippos.',
    'Drive back to Arusha.',
    NULL,
    'Flamingo, baboon, blue monkey, hippo, tree-climbing lion, pelican',
    'Lake Manyara shore, Rift Valley escarpment, groundwater forest',
    NULL,
    'Manyara', 'Arusha', 130, 0, 'B,L', @now, @now);
SET @day_19_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_19_3, @park_manyara, 'DAY_TRIP', 1, '07:00', '12:00', 'Morning game drive', @now);

-- ============================================================
-- ITINERARY 20: Northern Circuit Classic Safari (4 days, 3 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_20, 1, 'Day 1', 'Lake Manyara — Gateway to the Northern Circuit',
    'Drive to Lake Manyara for an afternoon game drive, then to your lodge near Ngorongoro.',
    'Depart Arusha. Drive to Lake Manyara National Park.',
    'Afternoon game drive — groundwater forest, lake shore flamingos, tree-climbing lions.',
    'Drive to Ngorongoro area lodge. Dinner and overnight.',
    'Tree-climbing lions, flamingo, elephant, baboon, hippo, blue monkey',
    'Rift Valley escarpment, Lake Manyara, groundwater forest canopy',
    NULL,
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_20_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_20_1, @park_manyara, 'DAY_TRIP', 1, '12:00', '17:00', 'Afternoon game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_20, 2, 'Day 2', 'Ngorongoro Crater to Serengeti',
    'Morning in the Ngorongoro Crater, then drive to the Serengeti for afternoon game drives.',
    'Descend into the crater at dawn. Morning Big Five game drive on the crater floor.',
    'Ascend and drive to Serengeti via the Ngorongoro highlands. Afternoon game drive in the Seronera Valley.',
    'Sundowner and dinner at Serengeti camp.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo (crater); wildebeest, zebra, cheetah (Serengeti)',
    'Crater floor panorama, Ngorongoro-Serengeti highland transition, Serengeti plains at sunset',
    NULL,
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_20_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_20_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '12:00', 'Morning crater game drive', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_20_2, @park_serengeti, 'SLEEP_OVER', 2, '15:00', NULL, 'Afternoon game drive, overnight', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_20, 3, 'Day 3', 'Full Day Serengeti Exploration',
    'A full day of game drives across the endless Serengeti plains.',
    'Dawn game drive — track big cats, visit hippo pools, explore kopje formations.',
    'Picnic lunch. Afternoon game drive through different Serengeti sectors.',
    'Sundowner on the plains. Dinner at camp.',
    'Lion, leopard, cheetah, elephant, hippo, crocodile, wildebeest, zebra, giraffe, topi',
    'Endless plains, kopje rock formations, Seronera River, vast herds',
    NULL,
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_20_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_20_3, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives, overnight', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_20, 4, 'Day 4', 'Serengeti Sunrise & Return to Arusha',
    'Final sunrise game drive, then drive back to Arusha.',
    'Dawn game drive for last wildlife encounters. Return to camp for breakfast.',
    'Drive back through Ngorongoro Conservation Area to Arusha arriving by evening.',
    NULL,
    'Morning predator activity, vultures, wildebeest, zebra',
    'Serengeti sunrise, Ngorongoro highlands on return',
    NULL,
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_20_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_20_4, @park_serengeti, 'DAY_TRIP', 1, '06:00', '09:00', 'Dawn game drive', @now);

-- ============================================================
-- ITINERARY 21: Serengeti Great Migration Safari (4 days, 3 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_21, 1, 'Day 1', 'Ngorongoro Crater — Big Five Immersion',
    'Drive to Ngorongoro for a full day in the crater, then overnight on the rim.',
    'Drive from Arusha to Ngorongoro. Descend into the crater. Morning Big Five game drive.',
    'Picnic lunch. Afternoon game drive — black rhino, lion prides, flamingo lake. Ascend.',
    'Luxury dinner on the crater rim.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, hyena, zebra',
    'Crater rim panorama, crater floor, Lake Magadi, Lerai Forest',
    NULL,
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_21_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_21_1, @park_ngorongoro, 'SLEEP_OVER', 1, '09:00', NULL, 'Full day crater, overnight rim', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_21, 2, 'Day 2', 'Journey to the Serengeti Plains',
    'Drive from Ngorongoro through the highlands into the Serengeti. Afternoon game drive.',
    'Morning drive through the Ngorongoro Conservation Area with Masai encounters.',
    'Enter the Serengeti. Afternoon game drive in the Seronera Valley tracking big cats and migration herds.',
    'Sundowner cocktails on the plains. Luxury camp dinner.',
    'Wildebeest herds, zebra, lion, cheetah, giraffe, topi, eland',
    'Highland to plain transition, Serengeti endless grasslands, Seronera Valley',
    NULL,
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_21_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_21_2, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon game drive, overnight', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_21, 3, 'Day 3', 'Full Day Migration Tracking',
    'A full day following the Great Migration across the Serengeti.',
    'Dawn drive to follow the migration herds. Morning focused on river crossing points and predator-prey interactions.',
    'Picnic lunch. Afternoon tracking the herds through different Serengeti sectors.',
    'Sundowner and camp dinner.',
    'Wildebeest herds, zebra, lion, crocodile (at crossings), cheetah, hyena, vultures',
    'Vast migration columns, river crossing drama, endless plains filled with wildebeest',
    'Migration patterns vary by season — your guide tracks the herds to the best current location.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_21_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_21_3, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day migration tracking, overnight', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_21, 4, 'Day 4', 'Serengeti Dawn & Departure',
    'Final dawn game drive, then return to Arusha.',
    'Pre-dawn game drive for last Serengeti encounters. Breakfast at camp.',
    'Drive back to Arusha through the highlands.',
    NULL,
    'Dawn predators, vultures, plains game',
    'Serengeti at first light, highland views on return',
    NULL,
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_21_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_21_4, @park_serengeti, 'DAY_TRIP', 1, '06:00', '09:00', 'Dawn game drive', @now);

-- ============================================================
-- ITINERARY 22: Romantic Crater & Serengeti Escape (4 days, 3 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_22, 1, 'Day 1', 'Romance on the Crater Rim',
    'Arrive at your ultra-luxury crater rim lodge with champagne welcome and sunset over the caldera.',
    'Leisurely departure from Arusha. Scenic drive through the highlands to Ngorongoro.',
    'Arrive at ultra-luxury crater rim lodge. Couples spa treatment. Private afternoon tea overlooking the crater.',
    'Champagne sunset over the crater. Private candlelit dinner.',
    NULL,
    'Ngorongoro Crater rim panorama, highland forests, sunset over the caldera',
    'This is a slow-paced, romantic experience — no rushing between parks.',
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_22_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_22_1, @park_ngorongoro, 'SLEEP_OVER', 1, '13:00', NULL, 'Arrival and crater rim relaxation', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_22, 2, 'Day 2', 'Crater Descent & Journey to Serengeti',
    'Private morning game drive in the Ngorongoro Crater, then onward to the Serengeti.',
    'Private crater descent. Exclusive game drive — Big Five encounters without the crowds. Champagne picnic.',
    'Drive to the Serengeti. Settle into ultra-luxury tented camp.',
    'Private bush dinner under the stars in the Serengeti.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo',
    'Crater floor beauty, transition to endless Serengeti plains',
    NULL,
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_22_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_22_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '12:00', 'Private crater game drive', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_22_2, @park_serengeti, 'SLEEP_OVER', 2, '16:00', NULL, 'Afternoon arrival, overnight luxury camp', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_22, 3, 'Day 3', 'Serengeti — Love on the Endless Plains',
    'Full day on the Serengeti with private sundowner experience.',
    'Dawn game drive. Track big cats in the golden morning light.',
    'Afternoon game drive. Private sundowner cocktails on a kopje overlooking the plains.',
    'Romantic bush dinner with traditional African entertainment.',
    'Lion, leopard, cheetah, elephant, wildebeest herds, giraffe, hippo',
    'Endless plains at sunrise, kopje sundowner views, Serengeti sunset',
    NULL,
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_22_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_22_3, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives, overnight', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_22, 4, 'Day 4', 'Serengeti Farewell & Return',
    'Leisurely morning and return to Arusha.',
    'Optional dawn game drive. Relaxed breakfast at camp.',
    'Fly or drive back to Arusha.',
    NULL,
    'Morning wildlife encounters',
    'Serengeti morning light',
    NULL,
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_22_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_22_4, @park_serengeti, 'DAY_TRIP', 1, '06:00', '09:00', 'Optional dawn drive before departure', @now);

-- ============================================================
-- ITINERARY 23: Northern Tanzania Backpacker Safari (4 days, 3 nights)
-- ============================================================
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_23, 1, 'Day 1', 'Lake Manyara — Into the Wild',
    'Drive to Lake Manyara for afternoon game viewing, camp near Ngorongoro.',
    'Depart Arusha. Drive to Lake Manyara.',
    'Afternoon game drive — forest, lake shore, flamingos, tree-climbing lions.',
    'Drive to campsite near Ngorongoro. Campfire dinner.',
    'Tree-climbing lions, flamingo, baboon, elephant, hippo',
    'Rift Valley escarpment, Lake Manyara, groundwater forest',
    NULL,
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_23_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_23_1, @park_manyara, 'DAY_TRIP', 1, '12:00', '17:00', 'Afternoon game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_23, 2, 'Day 2', 'Ngorongoro Crater — The Caldera',
    'Full morning in the Ngorongoro Crater.',
    'Descend into the crater. Morning game drive — Big Five, flamingos, hippo pools.',
    'Picnic lunch. Ascend. Drive to Tarangire area campsite.',
    'Campfire and stargazing near Tarangire.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo',
    'Crater panorama, crater floor, Lake Magadi',
    NULL,
    'Ngorongoro', 'Tarangire', 190, 1, 'B,L,D', @now, @now);
SET @day_23_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_23_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '14:00', 'Morning/early afternoon crater drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_23, 3, 'Day 3', 'Tarangire — Elephant Paradise',
    'Full day in Tarangire with elephant herds and baobab trees.',
    'Morning game drive along the river. Watch elephant herds.',
    'Picnic lunch. Afternoon drive through southern circuits.',
    'Final night at campsite. Campfire stories.',
    'Elephant herds, giraffe, lion, zebra, wildebeest, oryx',
    'Baobab landscapes, Tarangire River, savanna views',
    NULL,
    'Tarangire', 'Tarangire', 0, 1, 'B,L,D', @now, @now);
SET @day_23_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_23_3, @park_tarangire, 'DAY_TRIP', 1, '07:00', '17:00', 'Full day game drive', @now);

INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_23, 4, 'Day 4', 'Tarangire Morning & Return',
    'Final morning game drive, then back to Arusha.',
    'Early morning game drive in Tarangire for last encounters.',
    'Drive back to Arusha.',
    NULL,
    'Elephant, giraffe, birds of prey',
    'Morning light on baobabs',
    NULL,
    'Tarangire', 'Arusha', 120, 0, 'B,L', @now, @now);
SET @day_23_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_23_4, @park_tarangire, 'DAY_TRIP', 1, '06:30', '10:00', 'Morning game drive', @now);
-- ============================================================
-- ITINERARY 24: Northern Circuit Grand Safari (5 days, 4 nights)
-- ============================================================
-- Day 1: Arusha → Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_24, 1, 'Day 1', 'Arusha to Tarangire — Land of the Giants',
    'Begin the grand northern circuit adventure with a drive to Tarangire National Park, home to the largest elephant herds in northern Tanzania. Ancient baobab trees tower over the savanna as thousands of animals congregate along the Tarangire River.',
    'Depart Arusha after breakfast. Drive south to Tarangire National Park (2.5 hours, 120km). Enter the park and begin morning game drive along the iconic Tarangire River, watching elephant herds drink and bathe in the shallows. Search the ancient baobab groves for tree-climbing pythons and leopards resting in the forks of giant trees.',
    'Picnic lunch under a colossal baobab tree. Afternoon game drive through the southern circuits — vast swamps teeming with waterbirds, giraffe striding between the trees, and zebra and wildebeest moving across the open grasslands. Visit Silale Swamp for remarkable bird diversity.',
    'Drive to your luxury lodge in the Tarangire area. Welcome dinner with views across the savanna as the last light fades over the baobab silhouettes.',
    'African elephant (largest herds in northern Tanzania), lion, leopard, giraffe, Burchell''s zebra, wildebeest, fringe-eared oryx, lesser kudu, gerenuk, python, over 550 bird species including yellow-collared lovebird and ashy starling',
    'Ancient baobab tree landscapes stretching to the horizon, Tarangire River valley teeming with wildlife, Silale Swamp panorama, termite mound-dotted savanna with Masai Steppe views',
    'Tarangire is at its most spectacular during the dry season when thousands of animals converge on the river. The park boasts more elephant per square kilometre than anywhere else in East Africa.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_24_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_24_1, @park_tarangire, 'SLEEP_OVER', 1, '09:30', NULL, 'Game drive and overnight in Tarangire area', @now);

-- Day 2: Tarangire → Manyara
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_24, 2, 'Day 2', 'Tarangire to Lake Manyara — Forest, Flamingos & Lions',
    'A morning game drive in Tarangire before driving north to Lake Manyara National Park for an afternoon of extraordinary biodiversity. The groundwater forest, alkaline lake shore, and Rift Valley escarpment create one of Tanzania''s most diverse single-park ecosystems.',
    'Early morning game drive in Tarangire — catch lions on the hunt in the dawn light, watch elephant herds emerge from the bush to the river, and enjoy birds in the golden morning air. Breakfast at lodge before departing north.',
    'Arrive at Lake Manyara National Park (70km drive). Enter through the lush groundwater forest, spotting baboon troops, blue monkeys, and bushbuck in the dense canopy. Drive along the lake shore searching for the famous tree-climbing lions, vast flamingo flocks on the alkaline water, and hippo pools.',
    'Drive to your lodge overlooking Lake Manyara or near the Ngorongoro escarpment. Sundowner drinks on the terrace. Dinner with Rift Valley views.',
    'Tree-climbing lions (unique behavior), greater and lesser flamingo, hippo, baboon, blue monkey, elephant, giraffe, buffalo, bushbuck, African fish eagle, pelican, over 400 bird species',
    'Great Rift Valley escarpment looming above the park, alkaline Lake Manyara stretching to the southern horizon, dense groundwater forest canopy, hot springs area',
    'The drive between Tarangire and Manyara passes through Mto wa Mbu village, a fascinating multicultural Tanzanian market town worth a brief stop.',
    'Tarangire', 'Manyara', 70, 1, 'B,L,D', @now, @now);
SET @day_24_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_24_2, @park_tarangire, 'DAY_TRIP', 1, '06:30', '09:00', 'Early morning game drive before departure', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_24_2, @park_manyara, 'DAY_TRIP', 2, '12:00', '17:30', 'Afternoon game drive', @now);

-- Day 3: Manyara → Ngorongoro
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_24, 3, 'Day 3', 'Ngorongoro Crater — Descending into the Eighth Wonder',
    'Drive from Lake Manyara to the Ngorongoro Conservation Area and descend 600 metres into the world''s largest intact volcanic caldera. The crater floor shelters the densest concentration of wildlife in Africa — a self-contained ecosystem where predators and prey live in extraordinary proximity.',
    'Depart after breakfast and drive up the Ngorongoro highlands (60km). Pause at the crater rim viewpoint — your first glimpse of the vast caldera floor 600m below, stretching 20km across. Descend via the steep rim road and begin the morning game drive through the Lerai Forest, spotting elephant bulls and various antelope species.',
    'Picnic lunch beside the Ngoitokitok Springs hippo pool. Afternoon game drive across the crater floor — seek out the rare black rhino in the open grasslands, observe lion prides in the marsh areas, and drive to Lake Magadi''s pink flamingo shores.',
    'Ascend to the crater rim lodge. Dine with sweeping views over the caldera as the African night settles in. Overnight on the Ngorongoro rim.',
    'Black rhino, lion, spotted hyena, elephant, Cape buffalo, hippo, greater and lesser flamingo, crowned crane, secretary bird, jackal, zebra, wildebeest, Grant''s and Thomson''s gazelle, serval cat',
    'Panoramic 360-degree crater rim views, the Lerai Forest of yellow fever acacias, Lake Magadi''s vivid pink flamingo flocks, volcanic caldera walls rising dramatically from the grassland floor',
    'Ngorongoro has strict vehicle limits — your guide will plan the route for maximum wildlife encounters with minimum congestion. Bring warm layers for the crater rim at 2,300m altitude.',
    'Manyara', 'Ngorongoro', 60, 1, 'B,L,D', @now, @now);
SET @day_24_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_24_3, @park_ngorongoro, 'SLEEP_OVER', 1, '09:00', NULL, 'Crater descent, full game drive, overnight on rim', @now);

-- Day 4: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_24, 4, 'Day 4', 'Ngorongoro to the Serengeti — Into the Endless Plains',
    'Leave the Ngorongoro highlands and cross into the Serengeti National Park, the greatest wildlife spectacle on Earth. The transition from highland to endless plain is breathtaking — suddenly the land opens up into a horizon-to-horizon grassland teeming with life.',
    'Early morning optional rim walk before breakfast with panoramic crater views. Drive from the Ngorongoro rim across the conservation area, passing through open woodland and encountering Masai communities along the route. Cross the boundary into the Serengeti via the Naabi Hill Gate.',
    'Afternoon game drive in the central Serengeti''s Seronera Valley — the predator capital of Africa. Track resident lion prides, search for leopards draped over kopje rocks, and watch vast wildebeest herds streaming across the plains. Visit the Seronera River for hippo pools and large Nile crocodiles.',
    'Sundowner cocktails on the open plains as the Serengeti sun drops behind the horizon. Gourmet bush dinner at luxury tented camp under an unpolluted night sky blazing with stars.',
    'Lion, leopard, cheetah, wildebeest, Burchell''s zebra, giraffe, topi, eland, Thomson''s and Grant''s gazelle, hippo, Nile crocodile, hyena, black-backed jackal, bat-eared fox',
    'The dramatic Ngorongoro-to-Serengeti transition at Naabi Hill, the Seronera River valley winding through golden grasslands, granite kopje formations rising from the plains, Africa''s largest mammal migration herds',
    'The Serengeti is the world''s longest unbroken terrestrial mammal migration route. Over 1.5 million wildebeest and 250,000 zebra participate in the annual cycle.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_24_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_24_4, @park_ngorongoro, 'TRANSIT', 1, NULL, NULL, 'Transit through Ngorongoro Conservation Area', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_24_4, @park_serengeti, 'SLEEP_OVER', 2, '14:00', NULL, 'Afternoon arrival and game drive, overnight in park', @now);

-- Day 5: Serengeti → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_24, 5, 'Day 5', 'Serengeti Sunrise & Return to Arusha',
    'A final dawn game drive on the Serengeti plains — often the most memorable of the safari as predators are still active and the light is extraordinary. Then fly or drive back to Arusha with memories to last a lifetime.',
    'Pre-dawn departure from camp. Watch the Serengeti come alive at sunrise — lions returning from a night hunt, cheetahs scanning from termite mounds, and herds of wildebeest and zebra silhouetted against the golden horizon. Return to camp for a full safari breakfast.',
    'Transfer to Seronera Airstrip for a light aircraft flight back to Arusha (scenic 1-hour flight over the Ngorongoro Crater), or overland drive via the Ngorongoro highlands. Arrive Arusha by early afternoon. Lunch on arrival.',
    NULL,
    'Sunrise predator activity — lion, cheetah, leopard; dawn vulture activity, wildebeest and zebra herds, birds of prey on the morning thermals',
    'Serengeti sunrise over the endless plains, morning mist on the Seronera River, kopje silhouettes against a golden sky',
    'Flight option: scenic air transfer over Ngorongoro Crater adds a spectacular aerial perspective to the journey. Book in advance.',
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_24_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_24_5, @park_serengeti, 'DAY_TRIP', 1, '05:30', '10:00', 'Dawn game drive before departure', @now);

-- ============================================================
-- ITINERARY 25: Serengeti & Ngorongoro Romantic Getaway (5 days, 4 nights)
-- ============================================================
-- Day 1: Arusha → Ngorongoro
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_25, 1, 'Day 1', 'Arusha to the Ngorongoro Rim — Arrival in Paradise',
    'Begin your romantic escape with a scenic drive through the Great Rift Valley to the Ngorongoro highlands. Settle into your ultra-luxury crater rim lodge and enjoy a private champagne sunset overlooking one of the world''s most magnificent natural wonders.',
    'Leisurely departure from Arusha after a relaxed morning. Scenic drive through the Rift Valley escarpment passing the village of Mto wa Mbu and the shimmering surface of Lake Manyara far below. Ascend through the lush Ngorongoro highlands, passing through montane forest with colobus monkeys visible in the tree canopy.',
    'Arrive at your ultra-luxury crater rim lodge. Private check-in with personalised welcome. Couples spa treatment overlooking the forested rim. A romantic afternoon tea with crater views.',
    'Private candlelit dinner on the lodge terrace as the crater below fills with shadow and the sky above blazes with stars. Champagne toast to the journey ahead.',
    'Colobus monkey in highland forest en route, Masai cattle in the conservation area, first views of the crater below',
    'Great Rift Valley panorama from the highlands, Lake Manyara visible in the valley far below, dense montane forest along the crater rim, the first awe-inspiring view of the Ngorongoro Crater',
    'Ngorongoro is a UNESCO World Heritage Site and one of the Seven Natural Wonders of Africa. Your rim lodge sits at 2,300m — bring a warm layer for cool evenings.',
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_25_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_25_1, @park_ngorongoro, 'SLEEP_OVER', 1, '13:00', NULL, 'Arrival at crater rim, overnight', @now);

-- Day 2: Ngorongoro Crater
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_25, 2, 'Day 2', 'Ngorongoro Crater — Private Descent into the Caldera',
    'Descend exclusively into the Ngorongoro Crater for an intimate day of Big Five game viewing. The caldera''s self-contained ecosystem ensures exceptional wildlife density — lion, elephant, buffalo, black rhino, and hippo all within a 260 square kilometre arena.',
    'Early morning private crater descent at first light. Begin the game drive through the Lerai Forest of yellow fever acacias, where elephant bulls move silently between the trees. Search for the elusive black rhino on the open crater floor grasslands — one of the best places in Africa to see this critically endangered species.',
    'Private champagne picnic lunch at the Ngoitokitok Springs hippo pool, watching hippos wallow and yawn just metres away. Afternoon leisurely game drive focusing on big cats — lion prides with cubs, and the occasional leopard moving through the rocky outcrops. Drive to Lake Magadi for the mesmerising spectacle of thousands of pink flamingos.',
    'Ascend to the rim as the sun sets. Romantic dinner at the lodge with crater views. Overnight on the Ngorongoro rim.',
    'Black rhino (excellent visibility on crater floor), lion with cubs, elephant bulls, Cape buffalo, hippo, spotted hyena, greater flamingo, crowned crane, secretary bird, serval cat, zebra, wildebeest, Grant''s gazelle',
    'The caldera at sunrise filled with morning mist, Lerai Forest canopy glowing in early light, Lake Magadi blanketed in pink flamingos, crater wall reflections in the springs',
    'The crater floor is a closed ecosystem — animals cannot leave the caldera, guaranteeing extraordinary wildlife density year-round. Request a private vehicle for the most intimate experience.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_25_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_25_2, @park_ngorongoro, 'SLEEP_OVER', 1, '06:30', NULL, 'Full day private crater game drive, overnight rim', @now);

-- Day 3: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_25, 3, 'Day 3', 'Ngorongoro to the Serengeti — Endless Horizons',
    'Drive from the Ngorongoro highlands into the breathtaking expanse of the Serengeti. The landscape unfolds dramatically as the highland forests give way to infinite golden grasslands. Reach your ultra-luxury tented camp in the Seronera Valley in time for a golden hour sunset game drive.',
    'Leisurely breakfast on the lodge terrace above the crater. Morning drive through the Ngorongoro Conservation Area, passing Masai enkiamas (homesteads) and wildlife-rich open plains. Encounter Masai herders moving their cattle alongside wild zebra and wildebeest — a scene unchanged for centuries.',
    'Cross into the Serengeti via the Naabi Hill Gate. Afternoon game drive in the Seronera Valley — track lion prides on the hunt, spot cheetahs stalking across the open grasslands, and search kopje rocks for leopards silhouetted against the sky. Arrive at your ultra-luxury camp as the sun colours the plains amber.',
    'Private bush dinner under the infinite Serengeti stars. Guided star-gazing session with your naturalist guide. Champagne toast to Africa''s most romantic wilderness.',
    'Lion prides, cheetah, leopard on kopjes, wildebeest herds, Burchell''s zebra, giraffe, topi, eland, hippo in Seronera River, Nile crocodile',
    'The dramatic Ngorongoro-to-Serengeti landscape transition, Naabi Hill panorama over the plains, Seronera River valley winding through golden grassland, the legendary Serengeti sunset',
    'The Serengeti''s name derives from the Masai ''Siringet'' — ''the land that runs on forever.'' This sunset game drive will show you exactly why.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_25_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_25_3, @park_ngorongoro, 'TRANSIT', 1, NULL, NULL, 'Transit through Ngorongoro Conservation Area to Serengeti', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_25_3, @park_serengeti, 'SLEEP_OVER', 2, '14:30', NULL, 'Afternoon game drive and overnight in Serengeti', @now);

-- Day 4: Full Day Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_25, 4, 'Day 4', 'Serengeti — A Full Day on the Endless Plains',
    'An indulgent full day exploring the Serengeti at your own pace. Morning game drives, a private bush breakfast on the plains, afternoon wildlife viewing, and an unforgettable private evening bush dinner beneath the acacia trees.',
    'Pre-dawn departure from camp. Game drive in the magical Serengeti dawn light — lions returning from a night hunt, hyenas cackling over a kill, and the plains transforming from silver to gold as the sun rises. Private bush breakfast served on a folding table in the open savanna — the most memorable meal of the safari.',
    'Return to camp for a midday rest. Afternoon game drive following cheetah activity on the open grasslands, visiting the Seronera hippo pools, and exploring different sectors of the central Serengeti. Watch the enormous wildebeest herds flowing across the landscape.',
    'Private bush dinner served under a canopy of acacia trees on the open plains, accompanied by the sounds of the African night — lions calling in the distance, crickets, and the hoot of a pearl-spotted owl.',
    'Lion, leopard, cheetah, elephant, hippo, Nile crocodile, wildebeest, Burchell''s zebra, giraffe, topi, eland, Thomson''s and Grant''s gazelle, hyena, jackal, bat-eared fox, secretary bird, kori bustard',
    'Sunrise over the Serengeti plains, the Seronera River at golden hour, vast wildebeest herds stretching to the horizon, kopje rock formations against a dramatic sky',
    'The private bush breakfast and bush dinner experiences elevate this day into something truly extraordinary. Advance booking required for special dining setups.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_25_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_25_4, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day game drives, bush breakfast and dinner, overnight', @now);

-- Day 5: Serengeti → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_25, 5, 'Day 5', 'Serengeti Farewell — Hot Air Balloon & Flight to Arusha',
    'The perfect finale to a romantic African escape — an optional hot air balloon flight over the Serengeti at dawn, drifting silently above lion prides and wildebeest herds as the plains glow in the first light of morning. Then fly back to Arusha.',
    'Optional pre-dawn transfer to the balloon launch site. Drift silently over the Serengeti in the soft morning light — an utterly romantic and unforgettable experience. Post-flight champagne breakfast in the bush. For non-balloon guests, a final dawn game drive from camp.',
    'Transfer to Seronera Airstrip. Scenic charter flight back to Arusha (1 hour, with aerial views over the Ngorongoro Crater). Arrive Arusha by midday.',
    NULL,
    'Aerial views of lion prides, wildebeest herds and elephants from the balloon; final Serengeti wildlife encounters on the game drive',
    'Serengeti at dawn from the air — arguably the most spectacular view in Africa, morning mist on the plains from balloon altitude',
    'Balloon safari must be booked and paid separately in advance — highly recommended and extremely popular. Minimum 2 passengers. Weather dependent.',
    'Serengeti', 'Arusha', 0, 0, 'B', @now, @now);
SET @day_25_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_25_5, @park_serengeti, 'DAY_TRIP', 1, '05:00', '10:00', 'Optional balloon safari and return flight to Arusha', @now);

-- ============================================================
-- ITINERARY 26: Big Five Photography Safari (5 days, 4 nights)
-- ============================================================
-- Day 1: Arusha → Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_26, 1, 'Day 1', 'Tarangire — Elephants & Baobabs Through the Lens',
    'Arrive in Tarangire National Park — a photographer''s dream landscape of ancient baobab trees, enormous elephant herds, and a golden-lit savanna. The combination of giant trees and giant animals creates iconic African compositions at every turn.',
    'Depart Arusha after an early breakfast. Drive to Tarangire National Park (2.5 hours, 120km). Enter the park and begin the photography session along the Tarangire River — the perfect setting for elephant portraits as herds cross the sandy riverbed, bathe in the shallows, and interact with calves.',
    'Picnic lunch in the shade of a 1,000-year-old baobab tree — itself a compelling photographic subject. Afternoon photography circuit through the park''s southern sectors, focusing on giraffe moving between baobabs, zebra in the golden light, fringe-eared oryx against the savanna, and lesser kudu at the forest edge.',
    'Drive to your photography-dedicated lodge in the Tarangire area. Equipment download and image review session. Dinner with guide discussing next day''s strategy.',
    'African elephant herds and calves (best in East Africa), lion, leopard, giraffe, Burchell''s zebra, fringe-eared oryx, lesser kudu, gerenuk, warthog, rock hyrax, python in baobabs, superb starling, lilac-breasted roller, yellow-throated sandgrouse',
    'Ancient baobab tree landscapes (some over 1,000 years old), Tarangire River sandy riverbed scenes, termite mound silhouettes, Masai Steppe backdrop at dusk, golden savanna light in late afternoon',
    'Tarangire offers unmatched opportunities for elephant photography — bring a telephoto (200-400mm) for close portraits and a wide-angle for landscape-elephant compositions. Late afternoon light on the baobabs is extraordinary.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_26_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_1, @park_tarangire, 'SLEEP_OVER', 1, '09:30', NULL, 'Photography game drive, overnight in Tarangire area', @now);

-- Day 2: Tarangire → Ngorongoro (via Manyara transit)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_26, 2, 'Day 2', 'Tarangire to Ngorongoro — Portraits in the Highlands',
    'A morning of photography in Tarangire before driving north through Lake Manyara and ascending the Ngorongoro highlands. The journey itself provides compelling landscape photography opportunities as the terrain shifts dramatically from lowland savanna to misty montane forest.',
    'Dawn photography session in Tarangire — golden hour light on the river and elephant herds creates the day''s first outstanding images. Focus on family group interactions, young elephant calves playing, and elephant silhouettes against the rising sun.',
    'Drive north through Mto wa Mbu and past Lake Manyara — brief stop for Rift Valley escarpment landscape shots and a glimpse of flamingo flocks on the lake. Continue ascending to the Ngorongoro Conservation Area through dense montane forest with black-and-white colobus monkeys visible in the trees.',
    'Arrive at crater rim lodge in late afternoon. Photography session from the rim — the caldera fills with shadow while the rim glows golden. Briefing dinner on tomorrow''s crater photography plan.',
    'Elephant herds in Tarangire morning light, black-and-white colobus monkey on forest canopy en route, Masai herder portraits (with permission), first views of Ngorongoro Crater',
    'Dawn baobab and elephant compositions in Tarangire, Lake Manyara distant Rift Valley views, Ngorongoro highland forest, spectacular first crater rim vista',
    'This is a transit day with photography stops. Drive total approximately 130km. Pre-arrange permission to photograph Masai communities en route if desired.',
    'Tarangire', 'Ngorongoro', 130, 1, 'B,L,D', @now, @now);
SET @day_26_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_2, @park_tarangire, 'DAY_TRIP', 1, '06:00', '09:00', 'Dawn photography session before departure', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_2, @park_manyara, 'TRANSIT', 2, NULL, NULL, 'Brief transit and landscape photography stop', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_2, @park_ngorongoro, 'SLEEP_OVER', 3, '16:00', NULL, 'Arrival and crater rim photography, overnight', @now);

-- Day 3: Ngorongoro Crater (full photography day)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_26, 3, 'Day 3', 'Ngorongoro Crater — Big Five Photography in the Caldera',
    'A full day dedicated to Big Five photography in the Ngorongoro Crater — the most reliable location in Tanzania to photograph all five species in a single day. The contained environment, excellent light conditions, and extraordinary wildlife density make this a photographer''s pinnacle experience.',
    'Pre-dawn crater descent timed for golden hour on the crater floor. Morning session focused on lion prides in the soft first light, black rhino emerging from cover in the early morning, and elephant bulls moving through the Lerai Forest. Position the vehicle for atmospheric shots with the crater walls providing a dramatic backdrop.',
    'Extended photography session at Lake Magadi for flamingo abstracts, crowned crane portraits, and hippo close-ups at the Ngoitokitok pool. Afternoon focused on predator-prey interactions, hyena clan activity near the marsh, and the full sweep of Big Five compositions.',
    'Ascend the crater rim for sunset silhouette shots of the caldera. Dinner at lodge with image review. Overnight on the Ngorongoro rim.',
    'Black rhino (excellent sightings — one of Africa''s top rhino photography destinations), lion prides (crater resident prides are well-habituated), African elephant bulls, Cape buffalo herds, hippo (above-water portraits), spotted hyena, crowned crane, greater flamingo, secretary bird, serval',
    'Golden hour crater floor light with volcanic walls as backdrop, Lake Magadi flamingo reflection abstracts, Lerai Forest elephant silhouettes, volcanic caldera panorama from rim at sunset',
    'This is arguably the single best day for Big Five photography in Tanzania. Bring a range of lenses: 600mm for big cat portraits, 100-400mm for general wildlife, 24-70mm for crater landscape compositions. Bean bag or window mount recommended.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_26_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_3, @park_ngorongoro, 'DAY_TRIP', 1, '06:30', '17:30', 'Full day Big Five photography in crater', @now);

-- Day 4: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_26, 4, 'Day 4', 'Into the Serengeti — Predator Photography on the Plains',
    'Cross from Ngorongoro into the Serengeti and spend the afternoon in the predator-rich Seronera Valley. The open grasslands and low scrub of the central Serengeti provide unobstructed shooting lines for cheetah, lion, and leopard photography.',
    'Morning drive from the Ngorongoro rim across the conservation area. Wildlife photography en route — wide landscape shots of the highland transition, Masai and wildlife coexistence scenes, and the iconic first view of the Serengeti plains stretching to the horizon from Naabi Hill.',
    'Enter the Serengeti and head directly to the Seronera Valley. Afternoon dedicated to predator photography — position for cheetah on termite mounds scanning the plains, leopards draped over kopje rocks, and lion prides shading under acacia trees. The Seronera kopjes are one of Africa''s most reliable leopard photography spots.',
    'Sundowner photography session as the last light turns the plains copper and gold. Camp dinner with guide discussion of tomorrow''s dawn strategy. Overnight at photography-friendly Serengeti camp.',
    'Cheetah on termite mounds (excellent perch photography), leopard on kopje rocks, lion prides with cubs, wildebeest and zebra herds, giraffe at various focal lengths, Nile crocodile and hippo on Seronera River, topi on termite mounds, martial eagle',
    'The Ngorongoro to Serengeti landscape transition, Naabi Hill panoramic first-look over the Serengeti, Seronera kopje formations in afternoon light, predator photography with the plains as backdrop',
    'The Seronera River area has the highest density of resident big cats in the Serengeti. Arrive by 14:00 for a full afternoon session. Extended stops for predator photography mean patience — be prepared to wait at the right location.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_26_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_4, @park_ngorongoro, 'TRANSIT', 1, NULL, NULL, 'Transit through Ngorongoro Conservation Area', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_4, @park_serengeti, 'SLEEP_OVER', 2, '13:00', NULL, 'Afternoon predator photography, overnight in Serengeti', @now);

-- Day 5: Serengeti → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_26, 5, 'Day 5', 'Serengeti Dawn Photography & Return to Arusha',
    'The final photography session in the magical pre-dawn Serengeti light — often when the most extraordinary images are captured. Then drive or fly back to Arusha with a full memory card and memories that will last a lifetime.',
    'Pre-dawn departure (5:00 AM) for the golden hour photography session — the most sought-after light of the entire safari. Track active predators, shoot dramatic backlit wildebeest silhouettes against the dawn sky, and capture the Serengeti landscape in its most otherworldly light. Return to camp for full breakfast.',
    'Drive or fly back to Arusha. Overland route passes through the Ngorongoro Conservation Area for landscape photography on the return. Arrive Arusha by early afternoon. Lunch on arrival.',
    NULL,
    'Dawn predator activity — lion and cheetah most active at first light; backlit wildebeest and zebra herds, vultures gathering at a kill, birds of prey on morning thermals',
    'Serengeti at pre-dawn and sunrise — arguably the most photographed landscape in Africa, morning mist effects on the Seronera River, kopje silhouettes against a multi-coloured dawn sky',
    'Allocate time on return for final equipment download and a toast to the images captured. Most photographers rate this as the best photography safari in Africa.',
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_26_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_26_5, @park_serengeti, 'DAY_TRIP', 1, '05:00', '10:00', 'Dawn photography session before departure', @now);

-- ============================================================
-- ITINERARY 27: Kilimanjaro Marangu — Roof of Africa Trek (5 days, 4 nights)
-- ============================================================
-- Day 1: Arusha → Marangu Gate → Mandara Hut
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_27, 1, 'Day 1', 'Marangu Gate to Mandara Hut — Into the Rainforest',
    'Begin the Marangu Route ascent of Mount Kilimanjaro, Africa''s highest mountain and the world''s highest free-standing volcano. The first day''s trek leads through a magical montane rainforest alive with birdsong, giant ferns, and endemic wildlife.',
    'Drive from Arusha to Kilimanjaro National Park Marangu Gate (95km, approximately 2 hours). Complete registration, meet your mountain guide and porters, and receive a comprehensive pre-trek briefing. Begin trekking through the lush montane rainforest zone — the air thick with moisture and the canopy alive with bird calls.',
    'Trek through the rainforest for approximately 5-6 hours, covering 8km. The well-maintained trail winds upward through a world of giant tree ferns, draped mosses, and twisted ancient trees. Spot blue monkeys swinging through the canopy and white-necked ravens calling overhead. Arrive at Mandara Hut (2,700m) by mid-afternoon.',
    'Hot meal served at the Mandara Hut dining room. Briefing on altitude acclimatization and tomorrow''s route. Early rest in preparation for the days ahead. Optional short evening walk to the edge of the forest.',
    'Blue monkey, black-and-white colobus monkey, bushbuck in forest clearings, white-necked raven, Hartlaub''s turaco, silvery-cheeked hornbill, various sunbird species, chameleons',
    'Dense montane rainforest canopy forming a green tunnel above the trail, giant tree ferns creating a Jurassic atmosphere, hanging mosses and lichens, small forest streams, first views of Mawenzi peak above the treeline',
    'Trek distance: 8km. Duration: 5-7 hours. Altitude gained: 830m (1,870m to 2,700m). Difficulty: Moderate. Pole pole (slowly, slowly) is the golden rule — do not rush. Hydrate constantly.',
    'Arusha', 'Kilimanjaro', 95, 1, 'L,D', @now, @now);
SET @day_27_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_27_1, @park_kilimanjaro, 'SLEEP_OVER', 1, '09:00', NULL, 'Marangu Gate to Mandara Hut — 8km rainforest trek', @now);

-- Day 2: Mandara Hut → Horombo Hut
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_27, 2, 'Day 2', 'Mandara to Horombo Hut — Moorland & Mountain Horizons',
    'Ascend from the rainforest through the heath and moorland zone to Horombo Hut at 3,720m. The vegetation transforms dramatically as altitude increases — giant heather gives way to giant groundsel and lobelia in an otherworldly highland landscape.',
    'Early breakfast at Mandara Hut. Begin the day''s trek (11km) ascending above the treeline into the heather zone. The forest gives way to open moorland where giant heather trees, covered in old man''s beard lichens, create a misty highland atmosphere. Clear views of Mawenzi peak appear ahead.',
    'Continue ascending through the moorland into the semi-desert zone, passing through giant groundsel forests — prehistoric-looking plants found only on East Africa''s high mountains. The landscape becomes increasingly dramatic as the altitude builds. Arrive at Horombo Hut (3,720m) by early afternoon.',
    'Hot dinner at Horombo Hut. Acclimatization walk on the slopes above the huts. Early rest with Kibo peak visible on clear evenings. Night temperatures drop significantly — stay warm.',
    'Eland occasionally seen in moorland, white-necked raven, alpine chat, four-striped grass mouse, augur buzzard, lammergeier (bearded vulture) possible on slopes',
    'Emerging from rainforest to open moorland — a dramatic landscape transition, giant heather with old man''s beard lichens, first clear views of both Kibo and Mawenzi peaks, vast moorland views stretching to the Kenyan border',
    'Trek distance: 11km. Duration: 6-8 hours. Altitude gained: 1,020m (2,700m to 3,720m). Difficulty: Moderate-strenuous. Altitude effects may begin — headache is normal. Drink 3-4 litres of water. No alcohol.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_27_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_27_2, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Mandara Hut to Horombo Hut — 11km moorland trek', @now);

-- Day 3: Acclimatization at Horombo
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_27, 3, 'Day 3', 'Horombo Acclimatization — Zebra Rocks Day Hike',
    'A vital acclimatization day at Horombo Hut, following the essential mountaineering principle of ''climb high, sleep low.'' A rewarding day hike to Zebra Rocks at 4,000m dramatically improves summit success rates while offering spectacular mountain views.',
    'Relaxed breakfast at Horombo. Begin the acclimatization day hike to Zebra Rocks (4,000m) — a 2-hour ascent through the alpine desert zone, where the landscape becomes increasingly stark and lunar in character. The air thins noticeably at this altitude, and the views of Kibo''s ice cap and Mawenzi''s jagged peaks are spectacular.',
    'Reach Zebra Rocks (named for distinctive white and black mineral streaks on the rocky outcrops). Spend time at altitude, allowing the body to produce more red blood cells. Enjoy the extraordinary panorama — on clear days, the Kenyan plains stretch to the north and Lake Jipe shimmers far below. Descend back to Horombo.',
    'Rest at Horombo Hut. Hot dinner. Briefing on tomorrow''s route to Kibo Hut. Prepare gear for the higher altitude camp. Early to bed for the crucial Kibo day ahead.',
    'Alpine chat in rocky areas, four-striped grass mouse near the huts, white-necked raven, augur buzzard and possible lammergeier',
    'Zebra Rocks'' distinctive striped mineral formations, panoramic views of both Kibo (5,895m) and Mawenzi (5,149m) peaks, the vast alpine desert zone, the curvature of the Kilimanjaro massif, and distant views of the Kenyan plains',
    'Acclimatization day distance: approximately 8km round trip to Zebra Rocks. Duration: 4-5 hours. Altitude reached: 4,000m. This day is crucial for summit success — do not skip it. Rest, hydrate, and eat well.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_27_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_27_3, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Acclimatization hike to Zebra Rocks (4,000m), overnight Horombo', @now);

-- Day 4: Horombo → Kibo Hut
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_27, 4, 'Day 4', 'Horombo to Kibo Hut — The Alpine Desert Crossing',
    'The final approach to Kibo Hut at 4,703m — a long slog across the stark alpine desert known as the ''Saddle.'' The vast volcanic landscape between Mawenzi and Kibo is barren, dramatic, and utterly unlike anywhere else on Earth. Rest well at Kibo before the midnight summit push.',
    'Early breakfast and departure from Horombo. Begin the long ascent across the Saddle — the high alpine desert plateau between the twin peaks of Mawenzi and Kibo. The vegetation thins to almost nothing; only specialized grasses and everlasting flowers survive in the thin, cold air. The terrain is open and exposed.',
    'Cross the Saddle for approximately 5 hours (10km). Arrive at Kibo Hut (4,703m) by early afternoon. Rest immediately — the next departure is at midnight. Dinner served early (typically 6 PM). Guides distribute altitude medication if required.',
    'Brief rest, pack essentials for the summit push (head torch, warm layers, energy snacks, water). Lights out by 8 PM for a midnight wake-up. The pre-summit hours are quiet and tense with anticipation.',
    'Essentially no wildlife at this altitude — the alpine desert is near-sterile. White-necked raven may follow trekkers hoping for scraps near the huts.',
    'The vast Saddle plateau between Mawenzi and Kibo — a surreal lunar landscape, the dramatic jagged towers of Mawenzi peak to the east, the ice-capped dome of Kibo ahead, the Kilimanjaro massif at near-full altitude',
    'Trek distance: 10km. Duration: 5-6 hours. Altitude gained: 983m (3,720m to 4,703m). Difficulty: Strenuous at altitude. Sleep as much as possible before midnight wake-up. Drink water despite lack of thirst. Diamox can be taken with guide advice.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_27_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_27_4, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Horombo to Kibo Hut across the Saddle — 10km alpine desert', @now);

-- Day 5: Summit push → descent → Marangu Gate → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_27, 5, 'Day 5', 'Uhuru Peak Summit & Descent — Roof of Africa',
    'The ultimate day — a midnight summit push through the night to reach Uhuru Peak (5,895m) at sunrise. Standing on the Roof of Africa above the clouds, surrounded by glaciers, is one of the great achievements in adventure travel. Then descend all the way back to Marangu Gate.',
    'Wake up at midnight. Dress in full summit gear — every layer. Begin the night ascent from Kibo Hut in single file, torches creating a line of light up the mountain. The 5-7 hour ascent to Uhuru Peak crosses scree slopes and ash desert in the dark and cold. Reach Gilman''s Point (5,685m, crater rim) for the first light of dawn. Continue to Uhuru Peak (5,895m) as the sun rises over the clouds far below.',
    'Photograph at the famous Uhuru Peak sign surrounded by Kilimanjaro''s retreating glaciers. Begin the long descent back to Kibo Hut, then continue past Horombo to Horombo for a brief rest. Descend through the moorland and back into the rainforest to reach Marangu Gate.',
    'Collect summit certificates at Marangu Gate. Drive back to Arusha arriving by evening. Celebratory dinner in Arusha.',
    'At the summit: virtually no wildlife — a sterile high-altitude environment. On descent through the forest: blue monkey, colobus monkey, forest birds',
    'Uhuru Peak at sunrise — the most dramatic panorama in Africa, the Kilimanjaro glaciers glowing in first light, the curvature of the Earth visible from 5,895m, the sea of clouds below the summit stretching in every direction, Kilimanjaro''s caldera and ash pit',
    'Summit to Marangu Gate descent: approximately 25km total. This is the hardest day of the trek — the summit push covers 1,192m of vertical gain to 5,895m in the dark and cold. Success rate is approximately 65% on Marangu Route. Go slow, breathe deeply, and trust your guide.',
    'Kilimanjaro', 'Arusha', 95, 0, 'B,L', @now, @now);
SET @day_27_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_27_5, @park_kilimanjaro, 'DAY_TRIP', 1, '00:00', '18:00', 'Midnight summit push to Uhuru Peak, full descent to Marangu Gate', @now);

-- ============================================================
-- ITINERARY 28: Northern Tanzania Budget Explorer (5 days, 4 nights)
-- ============================================================
-- Day 1: Arusha → Manyara
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_28, 1, 'Day 1', 'Arusha to Lake Manyara — Welcome to the Northern Circuit',
    'Begin the budget northern circuit adventure with a drive to Lake Manyara National Park. This compact and spectacularly diverse park delivers extraordinary value — dense groundwater forest, alkaline lake, tree-climbing lions, and masses of flamingos in a single afternoon.',
    'Depart Arusha after an early lunch. Drive along the Great Rift Valley to Lake Manyara National Park (130km, approximately 2.5 hours). Enter the park gate and begin the afternoon game drive.',
    'Afternoon game drive through the groundwater forest — baboon troops, blue monkeys, and bushbuck in the dense shade. Drive along the lake shore searching for tree-climbing lions in the acacia trees, vast flamingo flocks on the alkaline water, and hippos wallowing in the shallow pools. Visit the hot springs area.',
    'Exit the park and drive to your budget campsite or guesthouse outside the park. Group campfire dinner under the Rift Valley stars. Route briefing for tomorrow''s Ngorongoro day.',
    'Tree-climbing lions (unique behavior in Africa), greater and lesser flamingo, hippo, baboon, blue monkey, elephant, giraffe, buffalo, African fish eagle, pelican, cormorant, stork varieties',
    'Great Rift Valley escarpment towering above the park, Lake Manyara''s glassy alkaline surface stretching south, dense groundwater forest canopy, Rift Valley panorama from the park entrance',
    'Lake Manyara offers outstanding value — high wildlife density in a compact area. The famous tree-climbing lions are unique to this park and Ishasha in Uganda. Excellent first afternoon of the safari.',
    'Arusha', 'Manyara', 130, 1, 'L,D', @now, @now);
SET @day_28_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_1, @park_manyara, 'DAY_TRIP', 1, '13:00', '17:30', 'Afternoon game drive', @now);

-- Day 2: Manyara → Ngorongoro
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_28, 2, 'Day 2', 'Ngorongoro Crater — Big Five on a Budget',
    'Drive to the Ngorongoro Crater for a full day of game driving on the crater floor. The world''s largest intact volcanic caldera shelters some of Africa''s densest wildlife populations — and the budget campsite on the rim makes this an accessible experience for every traveller.',
    'Early departure after breakfast. Drive from Manyara to the Ngorongoro crater rim (60km, 1 hour). Brief stop at the crater viewpoint before descending. Descend into the caldera and begin the morning game drive — elephant bulls in the Lerai Forest, lion prides in the grasslands, and the search for the elusive black rhino.',
    'Picnic lunch at the Ngoitokitok Springs, watching hippos wallow just metres from your vehicle. Afternoon game drive across the crater floor — Lake Magadi''s pink flamingo shores, spotted hyena clan interactions, zebra and wildebeest herds, and late-afternoon lion sightings near the marsh.',
    'Ascend to the crater rim by late afternoon. Set up camp at the budget crater rim campsite. Campfire dinner with the roar of lions drifting up from the caldera below. Overnight at crater rim campsite.',
    'Black rhino, lion, elephant, Cape buffalo, hippo, spotted hyena, greater flamingo, crowned crane, zebra, wildebeest, Grant''s and Thomson''s gazelle, jackal, serval',
    'Sweeping crater rim panorama (20km diameter caldera), Lerai Forest acacia canopy, Lake Magadi flamingo flocks, volcanic crater walls rising 600m from the floor',
    'Budget travellers stay in the Simba A or Simba B crater rim campsites — basic but with extraordinary views. Crater descent fees apply per vehicle and per person. Book in advance during peak season.',
    'Manyara', 'Ngorongoro', 60, 1, 'B,L,D', @now, @now);
SET @day_28_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_2, @park_ngorongoro, 'SLEEP_OVER', 1, '08:00', NULL, 'Full day crater game drive, overnight rim campsite', @now);

-- Day 3: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_28, 3, 'Day 3', 'Ngorongoro to the Serengeti — Entering the Endless Plains',
    'Drive from the Ngorongoro Conservation Area into the vast Serengeti National Park, Africa''s most famous wildlife sanctuary. Arrive by afternoon for your first Serengeti game drive and witness the scale of wildlife that makes this park legendary.',
    'Early breakfast at the crater rim campsite with morning views over the caldera. Break camp and drive west through the Ngorongoro Conservation Area toward the Serengeti. Wildlife spotting en route through the open highlands — Masai herders with cattle, zebra, and wildebeest sharing the same plains.',
    'Cross the Naabi Hill Gate into the Serengeti National Park. Afternoon game drive in the central Serengeti — wildebeest and zebra herds flowing across the grasslands, giraffe striding between the acacia trees, and the first sightings of Serengeti lion prides. Arrive at the budget campsite by late afternoon.',
    'Set up camp at the public campsite. Group campfire dinner. The sounds of the Serengeti night — hyena calls, lion roars, and the bark of zebra — create an authentic African bush experience.',
    'Wildebeest herds, Burchell''s zebra, giraffe, lion, topi, eland, Thomson''s and Grant''s gazelle, cheetah possible, warthog, hyena',
    'The highland-to-plain landscape transition descending into the Serengeti, Naabi Hill panoramic view of the endless plains, first encounter with vast Serengeti wildebeest herds',
    'The public campsites in the Serengeti are unfenced — wildlife can wander through camp at night. Store food in the vehicle and never leave camp on foot after dark. This is an authentic wild camping experience.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_28_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_3, @park_ngorongoro, 'TRANSIT', 1, NULL, NULL, 'Transit through Ngorongoro Conservation Area to Serengeti', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_3, @park_serengeti, 'SLEEP_OVER', 2, '13:30', NULL, 'Afternoon game drive, overnight public campsite', @now);

-- Day 4: Full Day Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_28, 4, 'Day 4', 'Full Day Serengeti — Africa''s Greatest Wildlife Show',
    'A complete day on the Serengeti plains — morning and afternoon game drives across the world''s most famous wildlife sanctuary. The combination of sheer animal numbers and open terrain makes every hour of driving rewarding.',
    'Pre-dawn game drive departing camp before sunrise. The Serengeti awakens dramatically at first light — lions padding back toward the kopjes after a night hunt, cheetahs scanning from termite mounds, and the plains filling with wildebeest and zebra moving to their morning grazing grounds. Visit the Seronera River for hippo and crocodile.',
    'Packed lunch in the bush. Afternoon game drive exploring different sectors of the central Serengeti — the Seronera Valley kopjes for leopard, open grasslands for cheetah, and the river system for birds and large herbivores. Follow the movement of the migration herds.',
    'Return to camp for the final campfire night. Cook a group meal. Share stories and photographs from the day. The Serengeti night sky, untouched by light pollution, is one of the most spectacular views of the Milky Way imaginable.',
    'Lion, leopard on kopjes, cheetah, elephant, hippo, Nile crocodile, wildebeest herds, Burchell''s zebra, giraffe, topi, eland, Thomson''s and Grant''s gazelle, spotted hyena, African wild dog (possible), kori bustard, secretary bird',
    'Endless Serengeti plains with wildlife in every direction, Seronera River valley and kopje formations, vast wildebeest migration columns, the legendary Serengeti horizon',
    'Full day in the Serengeti is essential to fully appreciate the scale of this ecosystem. The migration timing varies — your guide will direct drives to where the herds currently are. This is peak safari value.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_28_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_4, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day game drives, overnight public campsite', @now);

-- Day 5: Serengeti → Tarangire → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_28, 5, 'Day 5', 'Serengeti to Tarangire — A Final Farewell Drive',
    'A long final day completing the northern circuit loop — an early game drive in the Serengeti, then a long drive back through the Ngorongoro Conservation Area and a brief game drive in Tarangire before returning to Arusha. The circuit comes full circle.',
    'Early morning game drive departing camp at dawn for final Serengeti wildlife encounters. Catch the last of the morning predator activity and watch the wildebeest herds stirring at sunrise. Breakfast at camp, then break camp and begin the long drive south and east.',
    'Drive through the Ngorongoro Conservation Area (wildlife spotting en route through the highlands) and southeast toward Tarangire. Brief afternoon game drive in Tarangire National Park along the river — elephant herds provide a fitting finale, echoing the grandeur of the parks visited throughout the circuit. Exit Tarangire and drive to Arusha.',
    NULL,
    'Final Serengeti encounters — predators and plains game at dawn; Ngorongoro conservation area wildlife en route; elephant herds and giraffe in Tarangire for the farewell game drive',
    'Serengeti sunrise on the last morning, Ngorongoro highlands viewed from the road, Tarangire''s iconic baobab and elephant landscape closing the circuit',
    'This is a long driving day — approximately 300km total. The Tarangire stop is brief (2-3 hours). Arrive Arusha by evening. Budget-friendly option: skip Tarangire entrance and drive directly to Arusha saving park fees.',
    'Serengeti', 'Arusha', 300, 0, 'B,L', @now, @now);
SET @day_28_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_5, @park_serengeti, 'DAY_TRIP', 1, '05:30', '09:00', 'Dawn game drive before departure', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_5, @park_ngorongoro, 'TRANSIT', 2, NULL, NULL, 'Transit through Ngorongoro Conservation Area on return', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_28_5, @park_tarangire, 'DAY_TRIP', 3, '14:00', '17:00', 'Brief farewell game drive in Tarangire', @now);

-- ============================================================
-- ITINERARY 29: Kilimanjaro Machame — Whiskey Route Trek (6 days, 5 nights)
-- ============================================================

-- Day 1: Arusha → Machame Gate → Machame Camp (3,000m)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_29, 1, 'Day 1', 'Machame Gate to Machame Camp — Into the Rainforest',
    'Begin the legendary Machame Route, known as the Whiskey Route for its challenging and rewarding character. The trek opens with a dramatic passage through Kilimanjaro''s dense montane rainforest, ascending steeply to Machame Camp at 3,000 metres as the great mountain reveals itself above the canopy.',
    'Early morning departure from Arusha. Drive to Machame Gate on the southern slopes of Kilimanjaro (95km, approximately 2 hours). Complete registration and equipment checks at the gate. Meet your team of experienced mountain guides and porters. Begin the trek through the dense rainforest zone at approximately 1,500m.',
    'Trek through the lush montane rainforest — giant tree ferns, moss-draped Podocarpus trees, and endemic Impatiens kilimanjari flowers carpet the slopes. Listen for black-and-white colobus monkeys crashing through the canopy and spot the endemic Kilimanjaro bird species. The track is steep and well-worn, climbing steadily through the forest.',
    'Arrive at Machame Camp (3,000m) in late afternoon. Porters will have camp set up and hot tea ready. Enjoy the first views of Shira Ridge glowing above the tree line. Hot dinner served in the mess tent. Acclimatisation briefing from the lead guide. First night on the mountain.',
    'Black-and-white colobus monkey, blue monkey, white-necked raven, Hartlaub''s turaco, Kilimanjaro endemic birds, mountain buzzard, serval cat tracks in the mud',
    'Dense montane rainforest canopy draped in hanging moss and giant ferns, Machame Camp clearing with first views north toward the Shira Plateau, Kibo''s white glaciers glimpsed through breaks in the cloud',
    'Machame Gate to Machame Camp is approximately 11km, taking 5–7 hours. The ascent is steep and the trail can be muddy — waterproof boots and gaiters are essential. Altitude sickness is rare at this stage but drink plenty of water.',
    'Arusha', 'Kilimanjaro', 95, 1, 'L,D', @now, @now);
SET @day_29_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_29_1, @park_kilimanjaro, 'SLEEP_OVER', 1, '09:00', NULL, 'Machame Gate to Machame Camp via rainforest, overnight camp', @now);

-- Day 2: Machame Camp → Shira Camp (3,840m)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_29, 2, 'Day 2', 'Machame Camp to Shira Plateau — Moorland and Cloud',
    'Leave the rainforest behind and ascend through the heath and moorland zone onto the spectacular Shira Plateau. The vegetation transforms dramatically as giant heather and lobelia take over from the forest canopy, and the full scale of Kilimanjaro''s volcanic summit cone comes into view above.',
    'Early breakfast at camp. Begin ascending through the upper forest zone, which gradually gives way to heath and moorland. Giant heather trees, draped in old man''s beard lichen, line the track. The first Kilimanjaro lobelia and senecio plants appear — alien-looking giant groundsels found only in the high-altitude zones of East Africa''s peaks.',
    'Emerge onto the Shira Plateau — a vast, ancient caldera now carpeted in moorland vegetation at 3,840m. The plateau stretches westward and northward, with extraordinary views of Kibo''s ice fields catching the afternoon light. Optional acclimatisation walk to Shira Cathedral rock formation.',
    'Arrive at Shira Camp. Porters have hot soup and tea waiting. As the sun drops, the plateau turns golden then purple. Dinner and overnight at 3,840m — the first signs of altitude (mild headache, slight breathlessness) may appear; rest and hydrate well.',
    'White-necked raven, alpine swift, scarce swift, common swift at high altitude, Kilimanjaro giant lobelia, giant groundsel (endemic to Kilimanjaro), eland tracks in moorland soil',
    'Transition from forest to moorland as the vegetation changes zone by zone, the Shira Plateau stretching vast and brown-gold under the sky, Kibo''s summit glaciers gleaming to the northeast, Shira Cathedral pinnacles rising from the plateau',
    'Machame Camp to Shira Camp is approximately 5km, taking 4–6 hours. The final section onto the plateau is exposed — bring windproof layers. This is the first night at serious altitude; headaches are normal. Drink 3–4 litres of water through the day.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_29_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_29_2, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Machame Camp to Shira Camp via moorland, overnight at 3,840m', @now);

-- Day 3: Shira Camp → Lava Tower → Barranco Camp (3,960m)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_29, 3, 'Day 3', 'Lava Tower Acclimatisation & Barranco Camp — Climb High, Sleep Low',
    'The most important acclimatisation day on the Machame Route. Ascend to the Lava Tower at 4,630m — higher than any point in the Alps — before descending to Barranco Camp at 3,960m. The ''climb high, sleep low'' principle is critical for successful acclimatisation and maximises summit success rates.',
    'Depart Shira Camp and cross the plateau toward the base of Kibo''s southern flank. Ascend through the alpine desert zone — virtually no vegetation now, just volcanic scree, lava formations, and the stark beauty of the high mountain. The Lava Tower, a volcanic plug rising dramatically from the slope, serves as the acclimatisation high point at 4,630m.',
    'Reach the Lava Tower for lunch and a rest — most trekkers feel the altitude here with noticeable breathlessness and possible headache. Descend steeply into the spectacular Barranco Valley, a dramatic volcanic gorge sheltering giant Senecio kilimanjari groundsels up to 5 metres tall. The descent reveals a hidden lush world amid the high desert.',
    'Arrive at Barranco Camp (3,960m) and relax over hot drinks. The Barranco Wall rises directly from the camp — an impressive 300m scrambling section awaiting tomorrow''s climb. Dinner and overnight in the dramatic valley.',
    'White-necked raven, Alpine chat, scarce swifts riding thermals, Kilimanjaro giant groundsel (up to 5m tall), Kilimanjaro lobelia, occasional eland tracks at altitude',
    'The Lava Tower volcanic plug at 4,630m against Kibo''s glaciated summit cone, alpine desert moonscape of volcanic scree and lava formations, the dramatic Barranco Valley filled with giant groundsels, the towering Barranco Wall glowing orange at sunset',
    'This is the critical acclimatisation day. The Lava Tower altitude is 4,630m — trekkers will feel this. Descending to 3,960m to sleep dramatically improves acclimatisation. This day determines summit success more than any other. Take it slowly, breathe deeply, and stay hydrated.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_29_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_29_3, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Lava Tower acclimatisation then descent to Barranco Camp', @now);

-- Day 4: Barranco Camp → Karanga Camp → Barafu Camp (4,673m)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_29, 4, 'Day 4', 'Barranco Wall Scramble & Barafu High Camp',
    'The most exhilarating day of climbing on the Machame Route. Scale the Barranco Wall — a dramatic 300-metre rock scramble with breathtaking exposure — before continuing up to Barafu High Camp at 4,673m. This is the last camp before the summit push, and the excitement of the approaching summit is palpable.',
    'Early breakfast and pack camp. Approach the base of the iconic Barranco Wall — a 300m near-vertical rock scramble that looks intimidating but follows a clear route with hand- and footholds throughout. Your guide leads the way up the wall, which provides thrilling exposure with extraordinary views back over the Barranco Valley and across southern Kilimanjaro. No technical climbing experience required — only a head for heights.',
    'Reach the top of the Barranco Wall and continue east along the high slopes above the Breach. Pass through Karanga Camp and continue ascending through volcanic scree and lava terrain toward the base of Kibo''s summit crater. Arrive at Barafu High Camp (4,673m) in the afternoon — a dramatic exposed ridge with views across the clouds.',
    'Early dinner at Barafu Camp — 17:00–18:00. Rest as much as possible before the midnight summit attempt. Guide''s final briefing on route, signs of altitude sickness, and summit protocol. Lights out by 19:00 to attempt sleep before the 23:00 wake-up for summit push. The anticipation is extraordinary.',
    'White-necked raven (the highest-altitude bird on Kilimanjaro), alpine chat, occasional lammergeyer vulture soaring on thermals above the slopes',
    'The Barranco Wall scramble with vertigo-inducing views back across the valley, the vast southern ice field of Kilimanjaro glittering above, Barafu Camp perched on an exposed ridge at 4,673m with cloud sea below and stars above',
    'The Barranco Wall is a hands-and-feet scramble — gloves are recommended. The route is safe but exposed; follow your guide closely. Barafu is the coldest camp on the route — temperatures can drop to -10°C overnight. Eat dinner even if you have no appetite; calories are critical for the summit push.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_29_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_29_4, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Barranco Wall scramble to Barafu High Camp at 4,673m', @now);

-- Day 5: Summit Day — Barafu → Uhuru Peak (5,895m) → Mweka Camp (3,100m)
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_29, 5, 'Day 5', 'Uhuru Peak — The Roof of Africa at Sunrise',
    'The summit day — the culmination of every step climbed and every altitude metre gained. Depart Barafu at midnight, ascending 1,200 metres through the night by headtorch to reach Stella Point on the crater rim as the sun rises over the Kenyan plains. Continue 45 minutes to Uhuru Peak — 5,895 metres, the highest point in Africa.',
    'Wake-up at 23:00 on Day 4 night. Hot tea, light snacks, and full summit clothing layered on. Depart Barafu at midnight by headtorch, ascending the steep scree slope toward Stella Point in the biting cold (-15 to -25°C at summit). The slow, rhythmic pace (''pole pole'' — slowly slowly) is essential. The night sky above Kilimanjaro is extraordinary — the Milky Way visible above, the lights of distant Tanzania visible below. Reach Stella Point (5,756m) on the crater rim at dawn.',
    'From Stella Point, follow the crater rim to Uhuru Peak — 45 minutes along the glaciated rim with the ancient ice fields rising to the right and the vast crater stretching to the left. Stand on the iconic summit sign at Uhuru Peak (5,895m) — the Roof of Africa. Summit photographs, certificate, and celebration. Begin the long descent back to Barafu Camp for hot food and rest.',
    'Descend from Barafu to Mweka Camp (3,100m) via the steep Mweka trail through the moorland and upper forest zone. The descent takes 3–4 hours. Arrive at Mweka Camp exhausted and elated. Hot dinner and the deepest sleep of the mountain.',
    'White-necked raven at the summit, the shadow of Kilimanjaro stretching across the clouds at sunrise, Crater Camp frozen lake and fumaroles within the summit crater',
    'The summit glaciers of the Northern and Southern Ice Fields glowing at first light, the shadow of Kilimanjaro''s perfect volcanic cone stretching 200km across the Tanzanian and Kenyan plains at sunrise, the Uhuru Peak summit sign at 5,895m with the crater beyond, sunrise painting the cloud sea below in shades of coral and gold',
    'Summit night temperatures can reach -25°C with wind chill. Every layer is essential — down jacket, fleece, thermal base layers, wind shell, balaclava, gloves and mitts. The ascent from Barafu to Uhuru Peak takes 5–8 hours depending on fitness. Your guide will assess summit conditions and safety throughout. Many trekkers turn back at Stella Point — reaching Stella Point (5,756m) is still a remarkable achievement.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_29_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_29_5, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Summit push midnight to Uhuru Peak at dawn, descend to Mweka Camp', @now);

-- Day 6: Mweka Camp → Mweka Gate → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_29, 6, 'Day 6', 'Descent to Mweka Gate & Return to Arusha',
    'The final descent through the rainforest to Mweka Gate, then the triumphant return to Arusha. The forest that seemed so dense and mysterious on Day 1 now feels welcoming and warm as you descend from the summit world back to the lowlands.',
    'Final breakfast on the mountain. Break camp and begin the descent through the moorland toward the Mweka trail''s forest section. The descent is steep but well-graded — trekking poles are invaluable here. Re-enter the montane rainforest and listen again for the colobus monkeys that accompanied you on the approach.',
    'Arrive at Mweka Gate. Receive official Kilimanjaro summit certificates from the KINAPA authority — the green certificate for reaching Stella Point and the coveted gold certificate for Uhuru Peak. Farewell tips and celebration with the porter and guide team. Transfer to Moshi or Arusha.',
    'Return to Arusha. Celebration dinner and overnight in Arusha hotel. Hot shower, comfortable bed, and the satisfaction of one of East Africa''s greatest achievements.',
    'Black-and-white colobus monkey returning through the rainforest canopy, blue monkey, exotic forest birds, and the rich smell of the forest floor',
    'Final views back up through the rainforest canopy to the moorland above, Mweka Gate park boundary, the cultural return from the wilderness of the summit world to the warmth of the Tanzania lowlands',
    'Tipping the porter and guide team generously is important — they make the summit possible. Standard tip guidelines: head guide USD 20–30/day, assistant guide USD 15–20/day, cook USD 10–15/day, porter USD 8–12/day for the full trek. Certificates are issued at the gate on descent.',
    'Kilimanjaro', 'Arusha', 95, 0, 'B,L', @now, @now);
SET @day_29_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_29_6, @park_kilimanjaro, 'DAY_TRIP', 1, '07:00', '12:00', 'Final descent Mweka Camp to Mweka Gate, certificates issued', @now);

-- ============================================================
-- ITINERARY 30: Ultimate Northern Circuit Safari (6 days, 5 nights)
-- ============================================================

-- Day 1: Arusha → Arusha National Park → Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_30, 1, 'Day 1', 'Arusha National Park & Tarangire — A Grand Opening',
    'Begin the most comprehensive northern circuit experience available, visiting five of Tanzania''s finest protected areas over six days. Start with a morning in Arusha National Park — dramatic scenery, diverse wildlife, and the shadow of Mount Meru — before driving to the elephant paradise of Tarangire for an afternoon arrival game drive.',
    'Early morning departure from Arusha. Enter Arusha National Park — a compact but extraordinarily diverse park at the foot of Mount Meru, Tanzania''s second-highest peak. Game drive through the Momela Lakes and Ngurdoto Crater lookout. Watch giraffe, zebra, warthog, buffalo, and black-and-white colobus monkeys against the dramatic backdrop of Mount Meru. Flamingos gather on the alkaline Momela Lakes.',
    'Drive south to Tarangire National Park (2 hours). Afternoon game drive along the Tarangire River, where hundreds of elephants gather to drink and bathe. Watch elephant herds interact with young calves in the shallow water. The ancient baobab trees tower above the savanna as giraffe, zebra, and lion move through the golden afternoon light.',
    'Arrive at your luxury lodge in the Tarangire area for sundowners on the terrace. Welcome dinner with views over the savanna as the last baobab silhouettes fade against the darkening sky. Overnight in Tarangire.',
    'Arusha NP: giraffe, buffalo, zebra, hippo (Momela Lakes), flamingo, colobus monkey, klipspringer; Tarangire: African elephant (largest herds in northern Tanzania), lion, leopard, giraffe, fringe-eared oryx, lesser kudu, python, 550+ bird species',
    'Mount Meru rising 4,566m above Arusha NP with its perfect volcanic profile, Momela Lakes reflecting the sky, Ngurdoto Crater ''Africa''s Little Ngorongoro'', Tarangire River valley lined with ancient baobab trees at golden hour',
    'This opening day combines two very different parks — the compact drama of Arusha NP and the elephant-rich savanna of Tarangire. Total driving is approximately 3 hours. Arusha NP is often overlooked but offers spectacular scenery and reliable wildlife close to town.',
    'Arusha', 'Tarangire', 170, 1, 'L,D', @now, @now);
SET @day_30_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_1, @park_arusha, 'DAY_TRIP', 1, '07:00', '10:30', 'Morning game drive in Arusha NP before driving south', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_1, @park_tarangire, 'SLEEP_OVER', 2, '13:00', NULL, 'Afternoon arrival game drive, overnight luxury lodge', @now);

-- Day 2: Full Day Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_30, 2, 'Day 2', 'Full Day Tarangire — Elephants, Baobabs & the Savanna',
    'A full uninterrupted day in Tarangire National Park — one of Tanzania''s most underrated wilderness areas and home to some of the most dramatic elephant concentrations in Africa. Cover the park''s diverse circuits from the iconic river to the remote southern swamps.',
    'Pre-dawn wake-up for the dawn game drive along the Tarangire River. Watch elephant families emerge from the bush to drink and bathe in the morning light, calves scrambling down the riverbank to the water''s edge. Search baobab trees for leopard resting on branches and tree-climbing pythons coiled in the giant forks. The early morning light on the baobab landscape is extraordinary.',
    'Picnic lunch in the shade of a centuries-old baobab tree. Afternoon drive through the park''s southern circuits — the Silale Swamp draws enormous concentrations of waterbirds and large mammal herds. Visit the Gursi area for fringe-eared oryx, lesser kudu, and gerenuk. The park''s remote southern sectors feel truly wild and uncrowded.',
    'Return to the luxury lodge for a sundowner on the deck. The baobab horizon catches the last light as elephants move silently into the distance. Dinner and overnight at the Tarangire lodge.',
    'African elephant (hundreds at the river simultaneously in dry season), lion, leopard, cheetah, giraffe, Burchell''s zebra, wildebeest, fringe-eared oryx, lesser kudu, gerenuk, impala, warthog, ground hornbill, Ashy starling, yellow-collared lovebird, 550+ bird species',
    'The Tarangire River with elephant herds stretching along the banks, baobab tree silhouettes at dawn and dusk — some over 1,000 years old, Silale Swamp teeming with waterbirds, the remote Masai Steppe beyond the park boundary',
    'A full day in Tarangire is rare on northern circuit itineraries — this luxury version justifies it. Dry season (June–October) concentrates wildlife most dramatically along the river. The southern circuits require a knowledgeable guide to navigate and reward those who venture off the main tracks.',
    'Tarangire', 'Tarangire', 0, 1, 'B,L,D', @now, @now);
SET @day_30_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_2, @park_tarangire, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives, overnight luxury lodge', @now);

-- Day 3: Tarangire → Lake Manyara → Ngorongoro Rim
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_30, 3, 'Day 3', 'Tarangire to Lake Manyara & the Ngorongoro Rim',
    'A rich transit day combining a morning game drive in Tarangire, a midday visit to Lake Manyara National Park — home of the famous tree-climbing lions — and an afternoon ascent to the Ngorongoro Crater rim for a spectacular highland sunset.',
    'Early morning game drive in Tarangire, the golden dawn light illuminating the baobab landscape and the last elephant herds at the river. Drive north toward Lake Manyara National Park (2 hours) passing through the colourful Mto wa Mbu market town — a vibrant African cultural encounter on the edge of the Rift Valley.',
    'Enter Lake Manyara National Park for a focused afternoon game drive. Search the acacia woodland for the park''s famous tree-climbing lions. Walk the groundwater forest edge for blue monkeys and baboon troops. Drive the lake shore for flamingo and pelican flocks, hippo in the shallows, and giraffe drinking elegantly at the water''s edge. Excellent birding throughout.',
    'Drive from Lake Manyara through the Rift Valley and ascend the spectacular escarpment road to the Ngorongoro Conservation Area. Arrive at your luxury crater rim lodge at dusk — the first views of the vast caldera are staggering. Dinner at the rim lodge with the entire crater visible below.',
    'Tarangire: dawn elephant herds; Lake Manyara: tree-climbing lion (unique), greater flamingo, pelican, hippo, elephant, buffalo, giraffe, blue monkey, baboon, over 400 bird species; Ngorongoro rim: colobus monkey, buffalo',
    'Lake Manyara''s alkaline water reflecting the Rift Valley escarpment, tree-climbing lion in yellow fever acacia trees, the dramatic rim road ascending through montane forest, the first breathtaking view of the Ngorongoro Crater from the rim at 2,300m',
    'Lake Manyara is a compact and highly productive park — 2–3 hours is sufficient for a great game drive. Tree-climbing lions are not guaranteed but resident prides are known to use the acacia trees regularly. The crater rim arrival at dusk is one of the most dramatic moments of the entire safari.',
    'Tarangire', 'Ngorongoro', 180, 1, 'B,L,D', @now, @now);
SET @day_30_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_3, @park_tarangire, 'DAY_TRIP', 1, '06:00', '09:30', 'Dawn game drive before driving to Manyara', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_3, @park_manyara, 'DAY_TRIP', 2, '12:00', '15:30', 'Lake shore and forest game drive', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_3, @park_ngorongoro, 'SLEEP_OVER', 3, '17:30', NULL, 'Evening arrival at crater rim, overnight luxury rim lodge', @now);

-- Day 4: Full Day Ngorongoro Crater
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_30, 4, 'Day 4', 'Ngorongoro Crater — The Big Five in the Caldera',
    'A full day in the world''s greatest wildlife arena. The Ngorongoro Crater — a 260 sq km intact volcanic caldera — shelters the densest concentration of wildlife in Africa within its natural walls. All of the Big Five inhabit the crater floor, and a full day gives the best chance of encountering them all.',
    'Pre-dawn breakfast at the rim lodge. Descend into the crater at first gate opening for the golden hour game drive. The caldera floor catches the dawn light while the rim walls still cast shadow — an extraordinary atmospheric start. Morning drive focuses on the Lerai Forest of yellow fever acacias, where massive elephant bulls feed quietly, and the open short-grass plains where black rhino emerge in the early morning cool.',
    'Luxury picnic lunch served beside the Ngoitokitok Springs hippo pool — watch hippos wallow, yawn, and submerge just metres away. Afternoon game drive targeting lion prides resting near the Mungi marsh, cheetah scanning the open grasslands from termite mounds, and the flamingo carpet of Lake Magadi. The crater rim provides the dramatic backdrop for every wildlife encounter.',
    'Ascend to the crater rim for sundowners on the lodge terrace. The caldera below fills with purple shadow as the last light catches the escarpment walls. Gourmet dinner at the rim lodge. Overnight on the Ngorongoro rim.',
    'Black rhino (one of the best viewing locations in Africa), lion (resident crater prides), African elephant bulls, Cape buffalo herds, spotted hyena, hippo, cheetah, greater flamingo, black-backed jackal, serval, wildebeest, zebra, crowned crane',
    'The vast 260 sq km crater floor enclosed by 600m volcanic walls, Lerai Forest ancient acacia canopy, Lake Magadi''s flamingo-pink shores, Ngoitokitok Springs lush hippo pool oasis, the sweeping short-grass plains at the caldera centre',
    'A full day crater game drive is significantly better than a half day. The caldera''s vehicle limits mean strategic positioning is important — your guide will optimise the route for wildlife sightings. Bring warm layers for the early morning descent as crater rim temperatures are cold.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_30_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_4, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '17:00', 'Full day crater floor game drive, overnight rim lodge', @now);

-- Day 5: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_30, 5, 'Day 5', 'Into the Serengeti — The Endless Plains',
    'Cross from the Ngorongoro highlands into the Serengeti — Africa''s most iconic wilderness and the setting for the greatest wildlife spectacle on earth. The landscape transformation is extraordinary as highland moorland gives way to the infinite golden grasslands of the Serengeti plains.',
    'Morning departure from the rim lodge after breakfast. Drive west across the Ngorongoro Conservation Area with wildlife viewing en route — Maasai and their cattle share this land with wild animals in a remarkable coexistence. Optional 30-minute stop at Olduvai Gorge — where fossil evidence of our earliest human ancestors was discovered 1.8 million years ago.',
    'Cross through Naabi Hill Gate into the Serengeti. The first view of the plains from Naabi Hill is one of the defining African moments — the savanna stretching endlessly in every direction. Afternoon game drive through the Seronera Valley, the predator capital of Africa, tracking lion prides, cheetah on open ground, and leopards draped over kopje rocks.',
    'Arrive at your luxury tented camp for cocktails under the acacia trees. Private bush dinner served on the open plains as lions call from across the darkness and the Milky Way blazes overhead. Overnight in the Serengeti.',
    'Ngorongoro Conservation Area: Maasai and cattle coexisting with wildlife; Serengeti: cheetah, lion, leopard, wildebeest herds, zebra, giraffe, topi, eland, Thomson''s gazelle, hippo in Seronera River, Nile crocodile, bat-eared fox',
    'The Ngorongoro-to-Serengeti landscape transition across the highlands, Olduvai Gorge''s Rift Valley panorama, the iconic first view of the Serengeti plains from Naabi Hill, Seronera kopje rock formations in the golden afternoon light',
    'The Serengeti covers 14,763 sq km — more than twice the size of the English county of Yorkshire. Your guide will position for the best wildlife encounters based on the season and current animal movements. The migration (June–October and January–March) determines where the greatest concentrations of herbivores are found.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_30_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_5, @park_ngorongoro, 'TRANSIT', 1, NULL, NULL, 'Transit through NCA, optional Olduvai Gorge stop', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_5, @park_serengeti, 'SLEEP_OVER', 2, '13:30', NULL, 'Afternoon game drive and overnight in Serengeti', @now);

-- Day 6: Full Day Serengeti → Return to Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_30, 6, 'Day 6', 'Final Serengeti Game Drive & Return to Arusha',
    'A full final morning in the Serengeti for an extended dawn game drive before driving back to Arusha, completing the most comprehensive northern circuit experience available. Five parks in six days — every major ecosystem of northern Tanzania explored in a single unforgettable safari.',
    'Pre-dawn departure from camp for the last game drive in the magical Serengeti dawn. Lion prides are most active in the early morning, cheetah mothers move with cubs through the dewy grass, and the plains are alive with movement as the day''s animal activity begins. Extended morning drive maximises the final Serengeti encounters.',
    'Return to camp for a late brunch. Begin the drive back to Arusha via the Serengeti exit and the Ngorongoro Conservation Area (4–5 hours). Game viewing continues en route — the NCA is full of wildlife including elephant, buffalo, lion, and the ever-present zebra and wildebeest. Cross the highlands back to Arusha.',
    'Arrive back in Arusha in the early evening. Hotel check-in, hot shower, and the deeply satisfying reflection on five parks explored in six days. Farewell dinner celebrating the completion of the ultimate northern circuit safari.',
    'Final Serengeti morning: lion, cheetah, leopard, elephant, giraffe, zebra, wildebeest, topi, eland, hippo, Nile crocodile; NCA en route: elephant, buffalo, zebra, wildebeest, Maasai cattle herds',
    'Serengeti dawn light over the endless golden plains, acacia silhouettes in the early morning, the sweeping NCA highlands on the return drive, Mount Meru above Arusha at dusk welcoming you home',
    'The return drive from Serengeti to Arusha is approximately 5–6 hours including game viewing stops. Depart camp no later than 09:00 after brunch to arrive Arusha comfortably before evening. This is a long drive day but remains rewarding — the NCA en route delivers excellent wildlife viewing.',
    'Serengeti', 'Arusha', 330, 0, 'B,L', @now, @now);
SET @day_30_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_6, @park_serengeti, 'DAY_TRIP', 1, '05:30', '10:00', 'Final dawn game drive before departure', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_30_6, @park_ngorongoro, 'TRANSIT', 2, NULL, NULL, 'Transit through NCA with wildlife viewing en route to Arusha', @now);

-- ============================================================
-- ITINERARY 31: Family Northern Tanzania Safari Adventure (6 days, 5 nights)
-- ============================================================

-- Day 1: Arusha → Arusha National Park
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_31, 1, 'Day 1', 'Arusha National Park — The Perfect Family Introduction',
    'Begin the family safari adventure with a gentle introduction to Tanzania''s wildlife in Arusha National Park — compact, diverse, and ideal for families with children of all ages. The varied landscapes of forest, lakes, and savanna deliver immediate wildlife excitement without overwhelming long drives on the first day.',
    'Morning safari briefing with the whole family — learn about animal behaviour, safety in the bush, and how to use binoculars. Drive to Arusha National Park gate (30 minutes from Arusha town). Begin a canoe safari on the alkaline Momela Lakes — paddle quietly among flamingos, hippos, and a variety of waterbirds. Children are captivated by the close encounters from the water.',
    'Game drive through the park''s savanna and forest zones. Spot giraffe, zebra, warthog, and Cape buffalo against the dramatic backdrop of Mount Meru. Drive to the edge of Ngurdoto Crater — called ''Africa''s Little Ngorongoro'' — for panoramic views down into the crater floor below. Watch black-and-white colobus monkeys leaping through the canopy above.',
    'Return to your family-friendly lodge in the Arusha area for early dinner and a full briefing on the coming days. The children can share their wildlife journal drawings from the first day. Early bedtime as the big Tarangire days begin tomorrow.',
    'Giraffe, Cape buffalo, zebra, warthog, waterbuck, hippo (Momela Lakes), greater and lesser flamingo, black-and-white colobus monkey, blue monkey, African fish eagle, Hartlaub''s turaco, augur buzzard',
    'Mount Meru''s 4,566m peak rising above the parkland (second highest in Tanzania), Momela Lakes reflecting the morning sky, Ngurdoto Crater''s forest-filled caldera, colobus monkeys in the forest canopy',
    'Arusha NP is ideal for families — short drives, varied activities, and no long days in the vehicle. The canoe safari is a highlight for children. The park has no lions or leopards, making it a relaxed and safe introduction for younger children. Baboon encounters can happen at the lodge — follow staff guidance.',
    'Arusha', 'Arusha', 30, 1, 'L,D', @now, @now);
SET @day_31_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_1, @park_arusha, 'DAY_TRIP', 1, '08:00', '17:00', 'Canoe safari and game drive, family introduction', @now);

-- Day 2: Arusha → Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_31, 2, 'Day 2', 'Tarangire — Meeting the Elephant Families',
    'Drive to Tarangire National Park — the elephant family destination of Tanzania. Children are often most captivated by Tarangire''s elephant encounters: calves learning to drink, family groups dust-bathing, and the extraordinary sight of hundreds of elephants gathered at the river. The ancient baobab trees add a magical, fairy-tale quality to the landscape.',
    'Morning departure from Arusha after breakfast. Drive south to Tarangire National Park (2.5 hours, 120km). Enter the park and begin the game drive along the iconic Tarangire River. The first elephant sighting generates incredible excitement among children — and adults alike. Watch elephant families with tiny calves navigate the riverbank, splash in the water, and interact with each other.',
    'Family-friendly picnic lunch under a giant baobab tree — the children can try to guess how old the tree might be (some are over 1,000 years old). Afternoon game drive through the park''s central circuits. Spot giraffe ''necking'' in competition, zebra foals staying close to their mothers, and warthogs trotting with their tails held upright — a favourite with children.',
    'Drive to your family-friendly Tarangire lodge. Children''s activity at the lodge: wildlife spotting list competition and junior ranger programme. Family dinner on the lodge terrace as the sun sets over the savanna. Overnight in Tarangire.',
    'African elephant with calves (highlight for children), giraffe, Burchell''s zebra with foals, warthog, lion, leopard, buffalo, fringe-eared oryx, lesser kudu, impala, superb starling, lilac-breasted roller, 550+ bird species',
    'Tarangire River with elephant herds visible from horizon to horizon, ancient baobab trees rising from the savanna some over 1,000 years old, giraffe silhouettes against the sky, golden savanna light in late afternoon',
    'Children''s junior ranger programmes are available at several Tarangire lodges — these are excellent for engaging young safari-goers with the environment. Tarangire''s elephant concentrations are among the best in Tanzania for young children as sightings are frequent and often very close.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_31_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_2, @park_tarangire, 'SLEEP_OVER', 1, '10:30', NULL, 'Arrival game drive, overnight family lodge', @now);

-- Day 3: Full Day Tarangire
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_31, 3, 'Day 3', 'Tarangire Full Day — Safari Discovery for the Whole Family',
    'A full day exploring every corner of Tarangire — the elephant paradise of northern Tanzania. With an entire day available, cover the park''s diverse habitats from the river circuits to the remote southern swamps, with plenty of time for stops, wildlife watching, and the family experiences that make a safari unforgettable for children.',
    'Dawn game drive at first light — the most magical time in the African bush. Elephant families move to the river in the golden morning light, lions survey the plain from termite mounds, and the air is filled with birdsong. Children who wake early for this game drive invariably speak of it years later. Morning birdwatching with field guide — learning to identify the lilac-breasted roller, superb starling, and ground hornbill.',
    'Family bush picnic lunch in the shade of a colossal baobab. Educational game drive in the afternoon focusing on animal behaviour — how do elephants communicate? Why do zebra have stripes? What do lions eat? Your guide provides age-appropriate explanations that make the wildlife come alive for young minds. Visit the Silale Swamp for extraordinary waterbird diversity.',
    'Return to lodge for family activities: guided nature walk around the lodge grounds, traditional Tanzanian storytelling session for children, and a boma campfire dinner with the night sounds of the bush surrounding the family. Overnight in Tarangire.',
    'African elephant family groups (with calves of various ages), lion, leopard, giraffe, Burchell''s zebra, wildebeest, Cape buffalo, warthog family groups, ground hornbill, lilac-breasted roller, superb starling, yellow-collared lovebird, fringe-eared oryx',
    'Dawn baobab silhouettes against the rising sun, the Tarangire River teeming with wildlife at first light, Silale Swamp shimmering in the afternoon heat, the vast savanna stretching to the Masai Steppe',
    'A full day in Tarangire avoids the rushed experience of a single afternoon. The guide''s educational approach makes it engaging for children aged 5 and above. Junior ranger activity booklets enhance the experience further. Evening storytelling sessions are available at several lodges — an authentic cultural experience for the family.',
    'Tarangire', 'Tarangire', 0, 1, 'B,L,D', @now, @now);
SET @day_31_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_3, @park_tarangire, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day family game drives, overnight lodge', @now);

-- Day 4: Tarangire → Ngorongoro Crater
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_31, 4, 'Day 4', 'Ngorongoro Crater — Africa''s Greatest Wildlife Bowl',
    'Drive from Tarangire to the Ngorongoro Conservation Area and descend into the crater for an afternoon game drive. Children invariably describe the descent into the caldera as entering ''another world'' — the 600-metre walls rising around them, the vast crater floor spreading below, and the density of wildlife immediately apparent.',
    'Early departure from Tarangire. Drive north through the Rift Valley corridor past Lake Manyara and through the lively Mto wa Mbu market town — a cultural stop where the family can see local produce, traditional crafts, and the vibrant trading of a real Tanzanian market. Continue ascending to the Ngorongoro Conservation Area through dense highland forest.',
    'Arrive at the crater rim and descend into the caldera. Begin the afternoon game drive on the crater floor — lion prides are often visible early, elephant bulls move through the Lerai Forest, and the alkaline Lake Magadi shimmers with flamingos. The crater''s enclosed nature makes wildlife viewing extremely reliable — the animals cannot leave.',
    'Ascend to the crater rim at sunset. Arrive at your family-friendly rim lodge perched at 2,300m. Hot chocolate and biscuits for the children on the terrace as the crater below fills with shadow. Dinner and overnight on the Ngorongoro rim.',
    'Lion prides (highly visible), black rhino (the crater has one of East Africa''s best black rhino populations), African elephant bulls, hippo, spotted hyena, greater flamingo, crowned crane, zebra, wildebeest, Grant''s and Thomson''s gazelle, black-backed jackal, serval',
    'The Ngorongoro Crater rim panorama at 2,300m altitude, the descent road winding 600m down the crater wall into another world, the crater floor wildlife visible from the rim before entry, Lake Magadi''s pink flamingo carpet seen from above',
    'The Ngorongoro Crater is excellent for families because wildlife sightings are virtually guaranteed — the enclosed caldera ensures high wildlife density. Children aged 6 and above thoroughly enjoy the experience. Pack warm clothing for the rim (2,300m can be very cool, especially in the evening and early morning).',
    'Tarangire', 'Ngorongoro', 180, 1, 'B,L,D', @now, @now);
SET @day_31_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_4, @park_ngorongoro, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon crater game drive, overnight rim lodge', @now);

-- Day 5: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_31, 5, 'Day 5', 'Into the Serengeti — The Endless Plains Adventure',
    'Drive from the Ngorongoro highlands into the world-famous Serengeti — the stage for the greatest wildlife spectacle on earth. For children, the Serengeti''s wide-open plains and extraordinary predator activity create some of the most vivid memories of a lifetime. Arrive in time for an afternoon game drive as the plains come alive.',
    'Morning descent into the Ngorongoro Crater for the dawn game drive — children who were too tired last evening will wake enthusiastically now. Golden hour in the crater, following lion prides as they move across the grasslands in the morning light, watching baby zebra feeding beside their mothers, and searching for the black rhino on the open plains.',
    'Ascend the crater rim and drive west across the Ngorongoro Conservation Area into the Serengeti. Cross through Naabi Hill Gate and begin the afternoon game drive on the famous short-grass Serengeti plains. The first cheetah sighting is often a transformative moment for children — these sleek, daytime hunters are the embodiment of speed and elegance.',
    'Arrive at your family-friendly Serengeti camp in time for sundowners on the viewing deck. Children''s evening activity: night sky identification with a guide — the Serengeti Milky Way is extraordinary. Family dinner in camp and overnight in the Serengeti.',
    'Ngorongoro Crater morning: lion, black rhino, elephant, hippo, flamingo; Serengeti afternoon: cheetah, lion, wildebeest, zebra, giraffe, topi, Thomson''s and Grant''s gazelle, eland, kori bustard, secretary bird',
    'The golden crater floor in dawn light with volcanic walls rising above, the Naabi Hill panorama of the Serengeti plains stretching to the horizon, cheetah cubs playing on the open Serengeti grasslands',
    'The morning crater game drive combined with the afternoon Serengeti arrival makes this a full and exciting day for children. Pack snacks and water for the vehicle — it is a long day but never dull. The Serengeti''s open plains make cheetah sightings particularly good for children as there is nowhere for these cats to hide.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_31_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_5, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '11:00', 'Morning crater game drive before driving to Serengeti', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_5, @park_serengeti, 'SLEEP_OVER', 2, '14:00', NULL, 'Afternoon arrival game drive, overnight family camp', @now);

-- Day 6: Full Day Serengeti → Return to Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_31, 6, 'Day 6', 'Final Serengeti Morning & Return to Arusha',
    'A final morning on the Serengeti plains — the family''s last game drive before returning to Arusha. The Serengeti has a way of saving its most dramatic moments for last: a lion hunt at dawn, cubs playing in the morning light, or a cheetah coalition on the hunt. The drive home carries the full weight of extraordinary African memories.',
    'Pre-dawn wake-up for the final family game drive on the open Serengeti plains. Morning is the best time for predator activity — your guide will have followed radio updates from other vehicles overnight to position for the best sightings. Final wildlife journal entries for the children — recording the last encounters of the safari.',
    'Return to camp for a family brunch. Award junior ranger certificates to the children — a treasured memento of their first Tanzania safari. Begin the drive back to Arusha via the Ngorongoro Conservation Area (4–5 hours). Game viewing continues en route through the NCA — elephant, buffalo, and zebra alongside Maasai and their cattle.',
    'Arrive in Arusha early evening. Hotel check-in and final family safari dinner — everyone shares their favourite wildlife encounter from the six days. Safari albums, certificates, and photographs to remember the adventure. Tanzania has created safari-lovers for life.',
    'Final Serengeti morning: lion with cubs, cheetah, wildebeest, zebra, giraffe, topi, bat-eared fox, jackal; NCA en route: elephant, buffalo, zebra, Maasai cattle',
    'Final Serengeti dawn over the golden plains, giraffe silhouettes in the last morning light, the triumphant return to Arusha with Mount Kilimanjaro and Mount Meru on the horizon',
    'Junior ranger certificates can be prepared in advance by the lodge and awarded at a small ceremony at brunch — this is a wonderful family moment. The return drive from Serengeti to Arusha is approximately 5 hours; pack entertainment for children for the road. Depart camp no later than 09:30 after brunch.',
    'Serengeti', 'Arusha', 330, 0, 'B,L', @now, @now);
SET @day_31_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_6, @park_serengeti, 'DAY_TRIP', 1, '06:00', '10:00', 'Final morning family game drive before departure', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_31_6, @park_ngorongoro, 'TRANSIT', 2, NULL, NULL, 'Transit through NCA with game viewing en route to Arusha', @now);

-- ============================================================
-- ============================================================
-- ITINERARY 32: Bush to Beach — Northern Circuit & Zanzibar (7 days/6 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_32, 1, 'Day 1', 'Arusha to Ngorongoro Crater Rim',
    'Depart Arusha and drive through the Great Rift Valley escarpment to the world-famous Ngorongoro Conservation Area. The journey climbs through Maasai highland villages and lush montane forest before arriving at the crater rim for a spectacular sunset over Africa''s largest intact volcanic caldera.',
    'Morning departure from Arusha after breakfast briefing. Drive through Mto wa Mbu market town and along the Rift Valley floor, watching for roadside Maasai herders and distant wildebeest. Ascend the steep rim road through dense cloud forest.',
    'Arrive at the Ngorongoro Crater rim in early afternoon. Check in to your luxury rim lodge perched 2,300 metres above sea level. Enjoy panoramic views of the crater floor 600 metres below. Optional short walk along the rim for first wildlife spotting.',
    'Settle in for a gourmet dinner at the rim lodge. Watch the sun sink below the crater wall, painting the caldera in shades of copper and gold — one of Tanzania''s most iconic sunset views.',
    'Elephant and buffalo often seen on the crater rim road, Maasai cattle herds, augur buzzard, lammergeyer vulture, crowned crane over crater',
    'Panoramic crater rim views spanning 260 sq km of pristine caldera, dense highland montane forest on the rim slopes, Rift Valley escarpment vista looking south toward Lake Natron',
    'Arrive at the rim in good time for the sunset — it is spectacular from the lodge terrace. Pack a warm fleece as rim temperatures drop sharply at altitude after dark. Rim lodges are among Tanzania''s most atmospheric accommodations.',
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_32_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_32_1, @park_ngorongoro, 'SLEEP_OVER', 1, '14:00', NULL, 'Arrive rim, rim walk, overnight luxury rim lodge', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_32, 2, 'Day 2', 'Full Crater Floor Game Drive',
    'Descend 600 metres into the Ngorongoro Crater for a full day among the greatest concentration of wildlife on earth. The crater floor shelters all of the Big Five within its natural walls — a 26 km-wide sanctuary that functions as a natural zoo without fences.',
    'Pre-dawn breakfast at the lodge. Descend to the crater floor at gate opening for golden hour game viewing. Begin morning drive through the Lerai Forest where large elephant bulls feed on fever trees. Search the open grasslands for black rhino — one of the best places on earth to see this critically endangered species.',
    'Picnic lunch beside the Ngoitokitok Springs hippo pool, watching hippos wallow and resident Egyptian geese. Afternoon game drive targeting lion prides resting near the Mungi marsh, flamingo flocks on Lake Magadi, and big herds of buffalo on the short grass plains.',
    'Ascend the crater rim for sunset drinks on the lodge terrace. Dinner and overnight at rim lodge under a sky blazing with stars.',
    'Black rhino, lion, elephant, buffalo, spotted hyena, hippo, flamingo, crowned crane, black-backed jackal, serval, wildebeest, zebra, Grant''s and Thomson''s gazelle, cheetah',
    'The vast 260 sq km crater floor enclosed by 600m walls, Lerai acacia yellow-fever tree forest, Lake Magadi''s flamingo-pink shores, Ngoitokitok Springs lush oasis, sweeping short-grass plains',
    'This is a full day crater floor drive — one of the most rewarding wildlife days in Africa. Crater vehicles are limited so your guide will position strategically for the best encounters. Bring warm layers for the descent as mornings are cold.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_32_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_32_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '17:00', 'Full day crater floor game drive, overnight on rim', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_32, 3, 'Day 3', 'Ngorongoro to the Serengeti Plains',
    'Leave the crater highlands behind and drive west across the Ngorongoro Conservation Area into the legendary Serengeti National Park. The landscape transforms dramatically from highland montane to the open short-grass plains that stretch to the horizon — the setting for the greatest wildlife spectacle on earth.',
    'Early morning departure after breakfast. Drive west across the Ngorongoro highlands with wildlife spotting in the conservation area — Maasai and their cattle share the land with wild animals here. Descend through the Olduvai Gorge area, birthplace of humanity, with an optional stop at the visitor centre.',
    'Pass through Naabi Hill Gate into the Serengeti. Begin afternoon game drive through the short-grass plains of the southern Serengeti, scanning for cheetah on termite mounds, lion prides, and the vast wildebeest and zebra herds that migrate across these plains seasonally.',
    'Arrive at your luxury tented camp for sundowner cocktails on the plains. Gourmet bush dinner under the stars — the Serengeti night sky is extraordinary far from city lights.',
    'Cheetah, lion, wildebeest, zebra, Thomson''s gazelle, topi, eland, giraffe, elephant, secretary bird, lilac-breasted roller, kori bustard',
    'Serengeti endless plains stretching to the horizon at Naabi Hill, Olduvai Gorge Great Rift Valley panorama, granite kopje rock formations, dramatic African sunset over the plains',
    'The drive from Ngorongoro to Serengeti passes the Olduvai Gorge — where human fossils 1.8 million years old were discovered. The gorge visit adds 30 minutes but is highly worthwhile. Arrival in the Serengeti is a transformative moment.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_32_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_32_3, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Arrive Serengeti, afternoon game drive, overnight luxury camp', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_32, 4, 'Day 4', 'Full Day Serengeti — Big Cats & the Migration',
    'An entire day dedicated to the Serengeti''s astonishing wildlife. Morning and afternoon game drives cover the Seronera Valley — the predator capital of Africa — and the kopje-studded plains where leopards drape themselves over acacia branches and cheetah mothers teach their cubs to hunt.',
    'Pre-dawn wake-up for the dawn game drive when predators are most active. Drive through the Seronera Valley tracking lion prides returning from a night hunt, scan the kopje rocks for leopard silhouettes at first light, and follow cheetah as they warm up on termite mounds scanning for prey on the open plains.',
    'Bush picnic lunch at a scenic kopje. Afternoon game drive following the wildebeest migration herds, visiting the Seronera River hippo pools and crocodile sandbanks, and searching the riverine woodland for elusive leopards. Extended stop at a lion pride for behavioural observation.',
    'Return to camp for hot shower and sundowner. Hear hyena whoops and lion roars from your tent as the Serengeti night comes alive. Dinner and overnight under canvas.',
    'Lion, leopard, cheetah, spotted hyena, wildebeest, zebra, topi, eland, Thomson''s gazelle, Grant''s gazelle, giraffe, elephant, hippo, Nile crocodile, serval, African wild cat',
    'Seronera Valley grasslands, granite kopje formations rising from the plains, Seronera River and its gallery forest, vast wildebeest migration herds, uninterrupted sky from horizon to horizon',
    'The Serengeti is best explored slowly — spend time with individual animals rather than rushing between sightings. Your luxury camp is inside the park, giving you first and last access to the best game drive hours at dawn and dusk.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_32_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_32_4, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day game drives, overnight luxury camp', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_32, 5, 'Day 5', 'Serengeti Sunrise & Fly to Zanzibar',
    'One final exhilarating game drive at dawn in the Serengeti before flying to the spice-scented island of Zanzibar. By afternoon you will be trading savanna dust for Indian Ocean breezes — the ultimate bush-to-beach transition.',
    'Early morning game drive from camp as the sun rises over the Serengeti plains. Last chance to observe big cat activity, photograph the golden light illuminating the acacia trees, and savour the sounds and stillness of the Serengeti awakening.',
    'Return to camp for a leisurely breakfast and check-out. Transfer to the Seronera airstrip for your bush flight to Zanzibar (with connection via Arusha or Dar es Salaam). Arrive Zanzibar in the early afternoon. Check in to your beachfront luxury hotel.',
    'Stroll along the pristine white-sand beach as the Indian Ocean glows copper in the late afternoon light. Seafood dinner on the beach under swaying coconut palms — a world away from the Serengeti just hours ago.',
    'Morning: lion, cheetah, wildebeest, zebra, giraffe in the golden light. Afternoon: sea turtles, tropical fish, dhow boats on the Indian Ocean',
    'Serengeti golden sunrise over the plains, aerial views of the Ngorongoro Crater and Rift Valley during the flight, turquoise Indian Ocean and coral-sand beaches of Zanzibar',
    'The bush-to-beach experience is one of the most dramatic transitions in travel. Book your Zanzibar flights well in advance as bush flight schedules are limited. Your guide will transfer you to the airstrip with time to spare.',
    'Serengeti', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_32_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_32_5, @park_serengeti, 'DAY_TRIP', 1, '05:30', '10:00', 'Dawn game drive before airstrip transfer', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_32, 6, 'Day 6', 'Zanzibar — Jozani Forest, Spice Farm & Stone Town',
    'Explore the extraordinary natural and cultural heritage of Zanzibar Island. Walk through the ancient Jozani Forest to meet the endemic red colobus monkey, visit a traditional spice plantation bursting with cloves and vanilla, then wander the labyrinthine alleyways of UNESCO-listed Stone Town.',
    'Morning drive to Jozani Chwaka Bay National Park (45 minutes from the beach). Guided forest walk through ancient indigenous forest to encounter the rare Zanzibar red colobus monkey — one of Africa''s most endangered primates. Walk the mangrove boardwalk over Chwaka Bay tidal waters.',
    'Visit a traditional Zanzibar spice plantation — taste fresh cloves, nutmeg, cinnamon, lemongrass, and vanilla straight from the plant. Learn about Zanzibar''s centuries-old role at the heart of the Indian Ocean spice trade.',
    'Explore Stone Town at sunset: the House of Wonders, the old Arab Fort, the slave market memorial, and Forodhani Gardens night market for freshly grilled Zanzibar seafood. Return to beach hotel.',
    'Zanzibar red colobus monkey (endemic), Sykes monkey, mangrove crabs, mudskippers, Ader''s duiker, bush pig, over 50 butterfly species, sea birds over Chwaka Bay',
    'Jozani ancient forest canopy, mangrove boardwalk over tidal waters, tropical spice plantation, Stone Town''s coral-stone carved-door architecture and Arabic-Swahili streetscapes',
    'The Zanzibar red colobus is found nowhere else on earth. The Jozani forest walk takes approximately 1.5 hours. Stone Town is best explored on foot in the cooler late afternoon — its alleyways are too narrow for vehicles.',
    'Zanzibar', 'Zanzibar', 60, 1, 'B,L,D', @now, @now);
SET @day_32_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_32_6, @park_jozani, 'DAY_TRIP', 1, '09:00', '12:00', 'Jozani forest red colobus walk and mangrove boardwalk', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_32, 7, 'Day 7', 'Zanzibar Beach Morning & Departure',
    'A leisurely final morning on the white-sand beaches of Zanzibar before transferring to the airport or ferry terminal. Swim in the warm Indian Ocean, watch colourful dhow fishing boats drift past the reef, and savour a last Swahili coffee before heading home.',
    'Breakfast at the beachfront hotel. Free time on the beach — swim in the crystal-clear Indian Ocean, snorkel over the coral reef, or simply relax on the white sand under coconut palms. Optional sunrise yoga or guided kayak paddle on the glassy early-morning sea.',
    'Check out and transfer to Zanzibar International Airport or ferry terminal for onward connections.',
    NULL,
    'Tropical fish over the coral reef, spinner dolphins often visible from the beach, coconut crabs, sea turtles occasionally spotted offshore',
    'White coral-sand beach stretching in both directions, turquoise Indian Ocean shading to deep blue at the reef edge, traditional wooden dhow sailboats on the horizon',
    'Zanzibar flights to Dar es Salaam connect to international departures. Allow at least 2 hours before your flight for airport formalities. The ferry to Dar es Salaam is an alternative option and takes about 2 hours.',
    'Zanzibar', 'Zanzibar', 0, 0, 'B', @now, @now);
SET @day_32_7 = LAST_INSERT_ID();

-- ============================================================
-- ITINERARY 33: Kilimanjaro Lemosho — Summit the Roof of Africa (7 days/6 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_33, 1, 'Day 1', 'Lemosho Gate to Big Tree Camp (2,780m)',
    'Begin your Kilimanjaro summit adventure on the remote and scenic Lemosho Route. Drive from Arusha to the Lemosho Gate at 2,100m, then trek through primeval rainforest alive with colobus monkeys and forest birds to reach Big Tree Camp — Mti Mkubwa — in a cathedral of ancient trees.',
    'Early morning departure from Arusha, driving west past Moshi to the Londorossi Gate administrative checkpoint. Complete all registration and permit formalities. Transfer to Lemosho Gate (2,100m) to meet your mountain crew.',
    'Begin trekking through dense rainforest on a well-maintained trail. The forest is lush and atmospheric, with giant Hagenia trees draped in old-man''s-beard lichen, giant tree ferns, and wild impatiens flowering in the undergrowth. Climb steadily to Big Tree Camp at 2,780m.',
    'Set up camp at Mti Mkubwa — Big Tree Camp — named for the enormous Hagenia tree at its centre. Hot camp dinner served by your mountain crew. First mountain briefing from your lead guide. Early to bed in preparation for the days ahead.',
    'Black-and-white colobus monkey, blue monkey, white-necked raven, Hartlaub''s turaco, silvery-cheeked hornbill, sunbird species, bushbuck, duiker',
    'Primeval rainforest canopy towering overhead, giant Hagenia and Podocarpus trees, moss-draped branches, glimpses of Kibo cone through breaks in the cloud, forest floor carpeted in wild flowers',
    'The Lemosho Route starts from a remote gate with very few other trekkers — a far more peaceful start than the Marangu or Machame routes. The first day is a relatively gentle introduction; your porters carry all heavy equipment. Altitude sickness is unlikely at 2,780m but drink plenty of water.',
    'Arusha', 'Kilimanjaro', 95, 1, 'L,D', @now, @now);
SET @day_33_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_33_1, @park_kilimanjaro, 'SLEEP_OVER', 1, '11:00', NULL, 'Trek rainforest to Mti Mkubwa Big Tree Camp 2,780m', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_33, 2, 'Day 2', 'Big Tree Camp to Shira 2 Camp (3,840m)',
    'Trek out of the rainforest into the open heath and moorland zone of Kilimanjaro. The vegetation changes dramatically as you ascend onto the Shira Plateau — an ancient collapsed volcanic crater now covered in giant heather and lobelia. Wide open views emerge for the first time as cloud gives way to clear highland air.',
    'Wake up to mountain birdsong. Breakfast at camp, then begin trekking uphill through the forest-heath transition zone where giant heather trees take over from the rainforest canopy. Cross the Shira Plateau with its extraordinary moonscape of giant lobelias, groundsels, and silver everlasting flowers.',
    'Arrive at Shira 2 Camp (3,840m) on the high plateau in early afternoon. The views from Shira are extraordinary on a clear day — the summit cone of Kibo rises above to the east while the plateau stretches westward toward the glaciated Shira Plateau ridge.',
    'Acclimatization walk after lunch to Shira Cave (optional) for additional altitude gain before descending back to sleep low. Hot dinner at camp under a vast star-filled sky — at this altitude the Milky Way is extraordinary.',
    'White-necked raven, Alpine chat, augur buzzard, lammergeyer, leopard occasionally seen crossing the moorland, eland grazing on the plateau',
    'Shira Plateau moorland with giant heather trees reaching 5m tall, giant lobelia plants unique to Kilimanjaro, Kibo summit cone rising dramatically to the east, endless views across the Tanzanian plains to the south',
    'The gain in altitude today from 2,780m to 3,840m is significant. Walk pole pole — slowly, slowly — the Swahili mantra of the mountain. Drink at least 3 litres of water today. Mild headache is normal; severe headache, vomiting, or confusion means descend immediately.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_33_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_33_2, @park_kilimanjaro, 'SLEEP_OVER', 1, '06:30', NULL, 'Trek to Shira 2 Camp 3,840m, acclimatization walk', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_33, 3, 'Day 3', 'Shira 2 to Barranco Camp via Lava Tower (4,630m)',
    'The most important acclimatization day on the route. Trek to the dramatic Lava Tower at 4,630m — above the altitude of Mont Blanc — for a high-altitude acclimatization stop, then descend to Barranco Camp at 3,950m. This classic "climb high, sleep low" profile dramatically improves summit success rates.',
    'After breakfast trek east across the upper Shira Plateau toward the towering Lava Tower, a 90-metre volcanic plug rising from the alpine desert. This is the highest point you will reach until summit night — the altitude here feels thin and the views are staggering.',
    'Reach Lava Tower (4,630m) for lunch in the alpine desert zone where no vegetation grows except hardy mosses in rock crevices. The glaciated Western Breach wall of Kibo looms above. Then descend steeply to Barranco Camp at 3,950m, entering the giant groundsel zone.',
    'Camp at Barranco is spectacularly situated below the 300-metre Barranco Wall. Dinner at camp, listening to the calls of white-necked ravens echoing off the wall. The groundsel trees here reach 6m in height — prehistoric-looking plants found only on high East African mountains.',
    'White-necked raven, Alpine chat, four-striped grass mouse, eland occasionally at lower elevations, nest of a Kilimanjaro sunbird in the groundsel zone',
    'Dramatic Lava Tower volcanic plug against the blue sky, Western Breach glaciated wall of Kibo cone, giant groundsel and giant lobelia forest below Barranco Wall, panoramic views across to Mount Meru floating above the clouds',
    'The Lava Tower altitude of 4,630m is above the threshold where altitude sickness becomes a real risk. If you feel very unwell at the tower, inform your guide immediately. The descent to Barranco at 3,950m usually brings significant relief. Eat well tonight even if appetite is reduced.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_33_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_33_3, @park_kilimanjaro, 'SLEEP_OVER', 1, '06:30', NULL, 'Trek via Lava Tower 4,630m acclimatization to Barranco Camp 3,950m', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_33, 4, 'Day 4', 'Barranco Wall to Karanga Camp (3,995m)',
    'Scale the legendary Barranco Wall — a 300-metre near-vertical scramble that is the most dramatic and exciting section of the Lemosho Route. Hands-on climbing through a natural rock chimney system brings you to the upper southern slopes of Kibo with expansive views across Tanzania''s highland plains.',
    'Early morning start to tackle the Barranco Wall before other teams. The scramble begins immediately from camp — a thrilling hands-and-feet ascent through rock gullies and ledges. Your guide leads the route while porters carry loads with remarkable ease. The exposure is dramatic but the holds are solid.',
    'Top out on the wall for sweeping views back over the Barranco Valley and across to the Moshi plains 3,000m below. Continue along the southern circuit traverse path, crossing several stream gullies through the senecio zone, to arrive at Karanga Camp (3,995m) in the early afternoon.',
    'Rest afternoon at Karanga — this is an important recovery day before the high camp. Hot drinks and snacks. Guide briefing on summit night procedure: departure time, clothing layers, what to expect on the final ascent. Early dinner and early sleep — tonight is the last full night before summit.',
    'White-necked raven nesting on the Barranco Wall, Alpine chat on the rocky outcrops, four-striped grass mouse scavenging at camp, sunbird occasionally seen at lower elevations of the wall',
    'The dramatic 300m Barranco Wall with its rock chimneys and ledges, view back across the Barranco Valley from the top of the wall, sweeping southern plains of Tanzania visible 3,000m below, Kibo''s glaciated summit growing closer',
    'The Barranco Wall is easier than it looks — every trekker makes it with the encouragement of their guide. Take it slowly, use your hands where needed, and enjoy the exhilaration. Karanga to Barafu tomorrow is a short but altitude-gaining day. Sleep as much as possible tonight.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_33_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_33_4, @park_kilimanjaro, 'SLEEP_OVER', 1, '06:00', NULL, 'Barranco Wall scramble to Karanga Camp 3,995m', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_33, 5, 'Day 5', 'Karanga to Barafu High Camp (4,673m)',
    'A short but significant day — climb from Karanga to Barafu High Camp at 4,673m, the last camp before the summit push. The alpine desert here is stark and otherworldly: grey volcanic scree, no vegetation, and the enormous south face of the Kibo cone filling the entire sky above.',
    'Breakfast and pack up camp. Trek uphill from Karanga through the increasingly barren upper mountain — vegetation disappears entirely above 4,200m, replaced by loose volcanic scree and rock. Pass the junction with the Mweka descent route.',
    'Arrive Barafu High Camp (4,673m) early afternoon. Settle into your tent, eat a substantial but easily digestible meal, and rest completely. Your body is preparing for a night at nearly 6,000m. Check all summit clothing and equipment with your guide.',
    'Lights out by 7:00 PM. Wake-up call between 11:00 PM and midnight for summit attempt. Sleep as much as possible — many climbers feel too anxious or altitude-affected to sleep deeply, but resting horizontally is beneficial even without sleep.',
    'White-necked raven and Alpine chat are the only wildlife at this altitude. The mountain becomes a world of rock, ice, and sky',
    'The stark volcanic scree landscape of the upper alpine desert, the immense south face of Kibo cone above camp, glacial ice cliffs visible on the crater rim, views south to the Tanzanian plains stretching 150 km to the horizon',
    'This is a short trekking day intentionally — rest is critical for summit success. Eat well, hydrate, and avoid strenuous exertion. Acetazolamide (Diamox) users should continue their dose. Barafu is cold and often windy — wear all your layers. Summit clothes should be checked and staged for the midnight wake-up.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_33_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_33_5, @park_kilimanjaro, 'SLEEP_OVER', 1, '06:30', NULL, 'Trek to Barafu High Camp 4,673m, rest for summit', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_33, 6, 'Day 6', 'Summit Night — Uhuru Peak (5,895m) & Descent to Millennium Camp',
    'The defining day of the expedition. Beginning at midnight, you climb through darkness and bitter cold to the roof of Africa. At Uhuru Peak — 5,895m above sea level — you stand on the highest point on the African continent. Then descend all the way to Millennium Camp at 3,820m — a total of over 2,000m of descent.',
    'Midnight wake-up. Hot drinks and a light snack. Dress in all summit layers — base layer, mid layer, down jacket, hardshell, balaclava, glacier gloves, insulated boots with gaiters. Headtorch on. Begin the summit ascent in the dark, zigzagging up the Rebmann Glacier route on loose scree toward Stella Point on the crater rim.',
    'Reach Stella Point (5,756m) at crater rim — a massive achievement. The final 45-minute traverse along the crater rim to Uhuru Peak, at 5,895m, is the highest ground in Africa. Receive your Uhuru Peak certificate and take photos with the iconic wooden sign. Begin the long descent to Barafu, then continue all the way down to Millennium Camp (3,820m).',
    'Arrive at Millennium Camp (High Camp) exhausted but triumphant. Hot dinner served by your crew. Celebrate your achievement around the camp with your mountain team — sing Kilimanjaro songs with your guides and porters. Deep, well-earned sleep at 3,820m.',
    'The mountain above 5,000m is an arctic environment with no wildlife. At lower elevations on the descent, white-necked ravens appear again. The summit experience is about stars, ice, and the curvature of the earth at horizon',
    'The Milky Way blazing above at midnight during the ascent, the glow of first light over the Kenyan plains as you near the crater rim, the ancient glaciers of Kilimanjaro at the summit, the extraordinary 360-degree view from Uhuru Peak across Tanzania, Kenya and beyond — the curvature of the earth is visible',
    'This is the hardest day of the expedition and also the greatest. Most climbers experience tears at Uhuru Peak — the combination of altitude, exhaustion, and achievement is overwhelming. The descent from the summit to Millennium Camp covers over 2,000m of vertical — your knees will be tired but your spirits will soar. Trekking poles are essential for the descent.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_33_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_33_6, @park_kilimanjaro, 'SLEEP_OVER', 1, '00:00', NULL, 'Midnight summit to Uhuru Peak 5,895m, descend to Millennium Camp 3,820m', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_33, 7, 'Day 7', 'Millennium Camp to Mweka Gate & Return to Arusha',
    'The final descent through the rainforest on the Mweka Route — historically used as the porters'' path down the mountain. After receiving your summit certificate at Mweka Gate, drive back to Arusha for a celebration dinner and a real bed at last.',
    'Breakfast at Millennium Camp. Final pack-up and tip your porters and guides — an important moment of gratitude for the crew who made your summit possible. Begin the descent through the upper heath zone, re-entering the rainforest below 2,800m.',
    'Descend through the lush Kilimanjaro rainforest on the Mweka Route, knee-saving trekking poles essential on the steep path. Hear colobus monkeys calling in the canopy above. Arrive at Mweka Gate (1,640m) where your KINAPA summit certificate is officially issued.',
    'Drive from Mweka Gate back to Arusha (approximately 2 hours). Celebration dinner at a good restaurant in Arusha — cold beer, fresh food, and a comfortable chair have never felt so magnificent. Overnight in Arusha.',
    'Black-and-white colobus monkey returning in the forest zone, blue monkey, Hartlaub''s turaco, silvery-cheeked hornbill, various forest sunbirds, bushbuck near the forest edge',
    'The beautiful Kilimanjaro rainforest on the descent, colobus monkey troops in the canopy, views of Kibo summit cone looking back up from the forest, the emotional moment of crossing through Mweka Gate',
    'Mweka Gate certificate collection is compulsory — allow 30 minutes. Tip envelopes for guides and porters are best prepared in advance (KPAP guidelines available). Your legs will be sore — take ibuprofen if needed. The drive back to Arusha is a joyful journey.',
    'Kilimanjaro', 'Arusha', 0, 0, 'B,L', @now, @now);
SET @day_33_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_33_7, @park_kilimanjaro, 'DAY_TRIP', 1, '07:00', '14:00', 'Descent Mweka Route to Mweka Gate 1,640m, certificate collection', @now);

-- ============================================================
-- ITINERARY 34: Great Migration Photography Expedition (7 days/6 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_34, 1, 'Day 1', 'Arusha to Tarangire — Elephants & Baobab Photography',
    'Drive from Arusha to Tarangire National Park for an afternoon of photography among the ancient baobab trees and the largest elephant herds in northern Tanzania. The Tarangire River at the dry-season low draws hundreds of elephants — the density and intimacy of encounters here is unrivalled anywhere in East Africa.',
    'Morning departure from Arusha after briefing on the week''s photography goals, camera settings for the Tanzanian light, and vehicle positioning protocols. Drive to Tarangire (2.5 hours). Brief stop at the park gate to plan the afternoon photography circuit.',
    'Enter the park for an extended afternoon photography session. Position along the Tarangire River bank for elephant family group portraits, behavioural shots of elephants drinking and bathing, and the iconic juxtaposition of elephants with ancient baobab trees. Seek high perches on kopje outcrops for wide-angle landscape compositions.',
    'Drive to your ultra-luxury tented camp — positioned inside the park for full night sounds and immersive atmosphere. Post-photography debrief and image review session. Gourmet dinner under a canvas sky, listening to the distant sounds of the African bush.',
    'African elephant (largest concentrations in northern Tanzania — 3,000+ individuals), lion, leopard, cheetah, Maasai giraffe, plains zebra, wildebeest, fringe-eared oryx, lesser kudu, over 550 bird species including spectacular lilac-breasted rollers',
    'Ancient baobab trees 1,000+ years old in golden afternoon light, Tarangire River valley from above and below, termite-mound dotted savanna plains, Masai Steppe panorama to the east, swamp areas with dense bird concentrations',
    'Tarangire afternoon light is exceptional — the golden hour starts early here as the sun drops toward the Rift Valley. Position at the river at least 90 minutes before sunset. Elephants at the river work best with a low camera angle — ask your driver to position creatively. Pack a 70-200mm and a wide-angle lens for the baobab landscapes.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_34_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_1, @park_tarangire, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon elephant and baobab photography, overnight luxury camp inside park', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_34, 2, 'Day 2', 'Full Day Tarangire — Bird & Wildlife Photography',
    'A full day dedicated to Tarangire''s extraordinary diversity. With over 550 recorded bird species and resident predators patrolling the river valley, today delivers photography opportunities from dawn to dusk. Tarangire''s combination of bird life richness, elephant intimacy, and baobab landscape is unlike anywhere else in Africa.',
    'Pre-dawn wake-up for the golden hour game drive. Position at the river at first light for backlit elephant silhouettes against the rising sun, lion prides returning from night hunts, and the extraordinary dawn chorus of Tarangire''s birds. Specific targets: grey-crowned crane, lilac-breasted roller, southern ground hornbill, and the superb starling.',
    'Extended midday photography in the swamp areas for waterbird photography — yellow-billed stork, saddle-billed stork, African spoonbill, and fish eagle. Afternoon return to the river for elephant behavioural photography — mud bathing, sparring, and calf interactions make for powerful imagery.',
    'Sundowner drinks at a scenic viewpoint overlooking the river as the baobabs glow amber in the last light. Image review and editing session at camp. Gourmet dinner at the camp with your photographic guide.',
    'Southern ground hornbill, lilac-breasted roller, grey-crowned crane, saddle-billed stork, yellow-billed stork, African fish eagle, superb starling, Von der Decken''s hornbill, elephant, lion, leopard, cheetah, lesser kudu, giraffe',
    'Tarangire River at low water level — maximum elephant concentration along the banks, ancient baobab tree silhouettes at dawn and dusk, Silale Swamp waterbird photography, sweeping savanna grasslands in afternoon haze',
    'Tarangire is one of Africa''s great birding destinations. A 600mm lens is ideal for bird photography; a 24-105mm captures the landscape and elephants. Ask your guide to position the vehicle with the sun behind you for bird portraits. The swamp can be muddy — check your footwear.',
    'Tarangire', 'Tarangire', 0, 1, 'B,L,D', @now, @now);
SET @day_34_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_2, @park_tarangire, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day dawn-to-dusk bird and wildlife photography, overnight luxury camp', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_34, 3, 'Day 3', 'Tarangire to Ngorongoro — Rim Photography & Maasai Portraits',
    'Drive from Tarangire through the Lake Manyara area and up to the Ngorongoro Crater rim. En route, photograph the Great Rift Valley escarpment and the iconic landscape of the Manyara region. Arriving at the rim in time for the late afternoon golden hour — the Ngorongoro Crater rim at sunset is one of the most photogenic scenes in Africa.',
    'Early morning final game drive in Tarangire before check-out. Photograph the park at dawn one last time before the long drive north. Depart Tarangire after breakfast and drive through Mto wa Mbu.',
    'Stop at Mto wa Mbu for optional Maasai portrait photography and village cultural interaction — with permission and appropriate etiquette from your guide. Continue through the Lake Manyara escarpment with views over the Rift Valley. Ascend the Ngorongoro highlands through montane cloud forest.',
    'Arrive at the Ngorongoro Crater rim in late afternoon. Photograph the sunset over the caldera — one of Africa''s most iconic views. The cloud formations above the 260 sq km crater bowl, combined with the fading light, create extraordinary photographic opportunities. Overnight at luxury rim lodge.',
    'Maasai people and livestock along the roadside, African fish eagle over Lake Manyara visible from the escarpment, lammergeyer and augur buzzard on the rim, Maasai giraffe along the route, colobus monkeys in the rim forest',
    'Tarangire River valley at dawn on departure, Great Rift Valley escarpment with Lake Manyara shimmering below, Ngorongoro Crater rim panorama in golden late-afternoon light, dramatic cloud formations above the caldera',
    'Maasai portrait photography requires respectful engagement and a small gratuity — your guide will facilitate this appropriately. The rim sunset is highly weather-dependent; clear dry-season evenings produce the most dramatic colours. The drive from Tarangire to the rim takes approximately 3.5 hours.',
    'Tarangire', 'Ngorongoro', 130, 1, 'B,L,D', @now, @now);
SET @day_34_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_3, @park_manyara, 'TRANSIT', 1, NULL, NULL, 'Transit through Manyara area with escarpment photography stop', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_3, @park_ngorongoro, 'SLEEP_OVER', 2, '16:00', NULL, 'Arrive rim for golden hour sunset photography, overnight luxury rim lodge', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_34, 4, 'Day 4', 'Ngorongoro Crater — Big Five & Rhino Photography',
    'Descend into the Ngorongoro Crater for a full day of Big Five photography. The crater is the single best location in Tanzania for black rhino photography — the open terrain and high resident population mean you will almost certainly encounter this critically endangered animal in superb photographic conditions.',
    'Pre-dawn descent into the crater at gate opening for golden hour photography. Begin with the Lerai Forest where elephant bulls present superb portrait opportunities in the dappled forest light. Position for the open grassland areas at first light where black rhino are most active in the cool morning hours.',
    'Extended photography session on Lake Magadi for flamingo behaviour shots, crowned crane displays, and wading bird activity. Photograph lion prides in the midday rest period — perfect for close portrait work with shallow depth of field. Look for cheetah hunting on the eastern grasslands.',
    'Ascend the crater rim for sunset. Image review and editing session at the lodge. Dinner with panoramic views over the caldera as the stars emerge above the ancient crater walls.',
    'Black rhinoceros (best photographic opportunity in Tanzania — crater residents are well habituated), lion pride, African elephant bulls, buffalo herd, spotted hyena, flamingo, crowned crane, Kori bustard, secretary bird, black-backed jackal',
    'Crater floor in golden morning light, Lerai yellow-fever tree forest with elephant silhouettes, Lake Magadi flamingo reflections at midday, sweeping grassland plains enclosed by the 600m caldera walls, Ngoitokitok Springs oasis',
    'The black rhino in the crater are significantly more approachable than elsewhere in Tanzania. A 500-600mm lens is ideal for rhino portraits from distance. The crater floor has strict vehicle limits — your guide will position strategically. Do not chase animals — patient positioning produces the best images.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_34_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_4, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '17:00', 'Full day crater floor Big Five photography including black rhino', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_34, 5, 'Day 5', 'Ngorongoro to Serengeti — Migration Photography Begins',
    'Drive from the Ngorongoro Crater rim westward into the Serengeti for the centrepiece of the expedition — the Great Wildebeest Migration. The scale of the migration must be seen to be believed: over 1.5 million wildebeest, 300,000 zebra, and 500,000 Thomson''s gazelle moving in an endless cycle across the Serengeti-Mara ecosystem.',
    'Morning departure from the crater rim after breakfast. Drive through the Ngorongoro Conservation Area highlands with stops for highland wildlife photography — eland, Maasai with cattle, and highland birdlife. Pass through Naabi Hill Gate into the Serengeti with panoramic views of the plains below.',
    'Enter the Serengeti and begin afternoon photography sessions following the migration herds. The exact location of the migration shifts with season — your expert guide will position you at the most rewarding section of the herd movement. Wildebeest columns stretching to the horizon are among photography''s most powerful subjects.',
    'Arrive at your ultra-luxury Serengeti camp — positioned close to the heart of the action. Sundowners on the plains with wildebeest calls echoing across the evening grasslands. Gourmet dinner and evening briefing on the next two days'' photography priorities.',
    'Wildebeest (1.5 million in the ecosystem), plains zebra (300,000), Thomson''s gazelle, lion following the migration herds, cheetah hunting on the migration flanks, spotted hyena clan territories, crocodile ambushes at river crossings',
    'Naabi Hill panoramic view of the endless Serengeti plains from the park entrance, wildebeest migration columns stretching beyond the horizon, dust clouds rising from a thousand hooves, the scale of the ecosystem from horizon to horizon',
    'Migration photography is about scale and behaviour. Wide-angle shots capturing the scale of the herds are as important as telephoto wildlife portraits. River crossing photography requires patience — crossings can take hours to materialise. Your guide knows the crossings well.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_34_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_5, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Arrive Serengeti, migration photography afternoon, overnight ultra-luxury camp', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_34, 6, 'Day 6', 'Full Day Serengeti — Dawn to Dusk Predator & Migration Photography',
    'A full dawn-to-dusk photography day in the Serengeti — the ultimate wildlife photography experience on earth. From predator hunts at first light through the heat of midday (when cats rest and light shapes dramatic portraits) to the golden hour when the plains ignite with warm colour, this day delivers the full spectrum of Serengeti photography.',
    'Pre-dawn departure for the golden hour game drive. Focus on predator activity: cheetah scanning for prey on termite mounds at first light, lion prides on the move before the heat, leopard still visible in acacia trees at dawn. Position for backlit silhouettes against the rising sun.',
    'Midday photography at the Seronera River: hippo pool behaviour shots, crocodile on sandbanks, and the extraordinary aerial display of vultures. Afternoon drive following the migration and positioning for late-afternoon action — the golden hour in the Serengeti is among the most photogenic light on earth.',
    'Return to camp at last light having covered the full photographic day. Evening image review and curation session with your guide. Celebratory dinner — tomorrow is the final morning. Hear hyenas, lions, and the distant rumble of the migration from your tent.',
    'Cheetah with cubs, lion coalition hunting, leopard in acacia tree, wildebeest river crossing (seasonal), hippo pod behaviour, Nile crocodile, martial eagle, bateleur eagle, secretary bird, tawny eagle, vulture gatherings',
    'Serengeti golden hour light bathing the grasslands, migration dust clouds in afternoon backlighting, Seronera River reflections at midday, kopje silhouettes against a burning sunset sky, the Milky Way above camp after dark',
    'The golden hour in the Serengeti lasts approximately 45 minutes each end of the day — plan your positioning before the light arrives. River crossing photography requires reaching the crossing site early and waiting quietly — crossings are explosive and last only minutes. Shoot in burst mode.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_34_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_6, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full dawn-to-dusk photography day, overnight ultra-luxury camp', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_34, 7, 'Day 7', 'Serengeti Sunrise Photography & Fly Back to Arusha',
    'One last magical sunrise on the Serengeti plains — shooting the golden light as it floods across the endless grasslands and illuminates the migration herds. Then fly back to Arusha carrying extraordinary images and memories of one of the world''s great wildlife photography expeditions.',
    'Pre-dawn departure for the final sunrise photography session. Position at a chosen viewpoint or with a known predator sighting for first light. The Serengeti sunrise is one of the most photographed scenes in nature — and with good reason. Shoot wide and telephoto, landscape and portrait, to close out the expedition collection.',
    'Return to camp for a leisurely breakfast and final image review. Transfer to the airstrip for morning bush flight back to Arusha (via Seronera Airstrip). Arrive Arusha early afternoon.',
    NULL,
    'Final sunrise encounters: lion, cheetah, wildebeest in golden morning light, birds calling in the dawn chorus, Thomson''s gazelle and topi lit from behind by the rising sun',
    'Serengeti sunrise painting the grasslands in amber and gold, silhouettes of acacia trees and giraffes against the brightening sky, migration herds moving in the morning mist, aerial views of the Serengeti and Ngorongoro during the return flight',
    'The final morning is precious — maximise your time in the field rather than packing early. Your camp crew will pack your belongings while you are on the game drive. The bush flight offers aerial photography opportunities — have a camera ready for views of the crater and Rift Valley.',
    'Serengeti', 'Arusha', 0, 0, 'B', @now, @now);
SET @day_34_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_34_7, @park_serengeti, 'DAY_TRIP', 1, '05:30', '10:00', 'Final sunrise photography session before airstrip transfer', @now);

-- ============================================================
-- ITINERARY 35: Tanzania Backpacker Grand Tour (7 days/6 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_35, 1, 'Day 1', 'Arusha to Lake Manyara — Introduction to the Northern Circuit',
    'Depart Arusha for the first game drive of the grand tour at Lake Manyara National Park. Compact but incredibly diverse, Manyara packs groundwater forest, lake shore flamingos, and the famous tree-climbing lions into a single afternoon of game viewing — a spectacular introduction to northern Tanzania''s wildlife.',
    'Morning departure from Arusha after kit check and tour briefing. Drive along the Great Rift Valley escarpment road to Lake Manyara (130km, approximately 3 hours). Stop at the Manyara escarpment viewpoint for a first panoramic view over the alkaline lake and the Rift Valley floor.',
    'Enter Lake Manyara National Park for afternoon game drive. Trek through the dense groundwater forest spotting blue monkeys and baboon troops in the canopy. Drive along the lake shore scanning for tree-climbing lions, flamingo flocks, and hippo pods in the shallows.',
    'Exit the park and drive to your budget camp near the park boundary. Community campfire, self-catering dinner, and introductions among the group. Night sounds of the African bush from your tent.',
    'Tree-climbing lions, flamingo, hippo, baboon, blue monkey, elephant, giraffe, buffalo, bushbuck, fish eagle, pelican, stork species, vervet monkey',
    'Great Rift Valley escarpment panorama from the viewpoint, Lake Manyara stretching south under pink flamingo clouds, dense groundwater forest canopy alive with primates, Rift Valley floor views from inside the park',
    'Lake Manyara is small and best explored over 3-4 hours rather than a full day. This makes it a perfect first-afternoon park for the tour. Budget camp near the park entrance offers basic facilities in a great location. Bring your own sleeping bag.',
    'Arusha', 'Manyara', 130, 1, 'L,D', @now, @now);
SET @day_35_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_1, @park_manyara, 'DAY_TRIP', 1, '13:00', '17:30', 'Afternoon game drive, tree-climbing lions and flamingos', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_35, 2, 'Day 2', 'Manyara to Ngorongoro — Crater Floor on a Budget',
    'Drive from Lake Manyara up the highlands to the Ngorongoro Crater rim and descend to the crater floor for a half-to-full day of game viewing. Even on a budget this experience is life-changing — the Ngorongoro Crater is one of the wonders of the natural world and does not discriminate between backpackers and luxury travellers.',
    'Early morning departure from camp. Drive the 60km from Manyara to the Ngorongoro Conservation Area, ascending through highland montane forest to the crater rim. Register at the gate and prepare for the crater descent.',
    'Descend to the crater floor and spend the day on a classic game drive circuit: Lerai Forest for elephants, the hippo pool at Ngoitokitok Springs, Lake Magadi for flamingos, and the open grasslands for black rhino and lion. Packed lunch inside the crater.',
    'Ascend the crater rim in late afternoon. Drive to the rim campsite — basic facilities but the most dramatic campsite in Tanzania, perched on the edge of an ancient caldera. Prepare your own dinner over the camp stove with views of the crater below.',
    'Black rhino, lion, elephant, hippo, buffalo, spotted hyena, flamingo, crowned crane, wildebeest, zebra, jackal, serval, Grant''s gazelle, Thomson''s gazelle',
    'The Ngorongoro Crater — 260 sq km of pristine wilderness enclosed in the walls of an extinct volcano, Lake Magadi flamingo shores, Lerai Forest, crater rim sunset from the campsite',
    'The crater floor game drive takes 4-6 hours depending on sightings. Budget travellers access the same crater as luxury lodges — the wildlife is identical. Crater floor campfires are not permitted. The rim campsite has cold showers and basic pit latrines. It is cold at 2,300m — bring warm clothes.',
    'Manyara', 'Ngorongoro', 60, 1, 'B,L,D', @now, @now);
SET @day_35_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_2, @park_ngorongoro, 'DAY_TRIP', 1, '09:00', '17:00', 'Crater floor game drive, overnight rim campsite', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_35, 3, 'Day 3', 'Ngorongoro to Serengeti — Entering the Endless Plains',
    'Drive west from the Ngorongoro highlands into the Serengeti National Park. This is the moment the Serengeti reveals itself — the descent from the highlands opens into an ocean of golden grassland stretching beyond sight in every direction. Afternoon game drive in the central Serengeti begins the Serengeti chapter of the grand tour.',
    'Pack up camp at the crater rim. Morning descent through the Ngorongoro Conservation Area past the Olduvai Gorge turnoff — optional quick stop at the gorge viewpoint where 1.8-million-year-old human fossils were discovered. Continue west to Naabi Hill Gate, the Serengeti''s main entrance.',
    'Enter the Serengeti through Naabi Hill Gate (145km from the rim). Begin afternoon game drive across the short-grass southern plains, scanning termite mounds for cheetah, following wildebeest and zebra herds, and watching for lion on the kopje outcrops.',
    'Arrive at the public campsite inside the Serengeti. Set up tents as the sun goes down over the plains. Communal campfire dinner. First night sleeping inside the Serengeti — hyenas and lions are real neighbours here. Tents must be properly secured.',
    'Cheetah, lion, wildebeest, plains zebra, topi, Thomson''s gazelle, eland, giraffe, secretary bird, lilac-breasted roller, kori bustard, bateleur eagle',
    'Naabi Hill panoramic entry into the Serengeti plains, the dramatic transition from highland to savanna, endless grass plains with scattered kopje granite outcrops, the first sunset inside the Serengeti',
    'The Serengeti public campsites are unfenced — wild animals move through camp at night. Food must be locked in the vehicle. Follow your guide''s instructions about night-time safety. The stars inside the Serengeti are among the best in the world.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_35_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_3, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Arrive Serengeti, afternoon game drive, overnight public campsite', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_35, 4, 'Day 4', 'Full Day Serengeti — Big Cats & Kopje Rock Formations',
    'A full day exploring the heart of the Serengeti with morning and afternoon game drives covering the Seronera Valley — the most wildlife-rich area of the park and the predator capital of Africa. The kopje rock formations are the key to finding leopards and lions resting during the heat of the day.',
    'Dawn game drive from camp — the best time in the Serengeti. Drive to the Seronera Valley to track lion prides returning from night hunts, scan the kopje outcrops for resting leopards, and position for cheetah activity on the open plains. The morning chorus of birds fills the Serengeti with sound.',
    'Packed lunch at a scenic kopje. Afternoon game drive through the central Serengeti following the wildebeest and zebra herds, visiting the Seronera River hippo pools and crocodile sandbanks, and searching for the Serengeti''s famous large lion coalitions.',
    'Second night in the Serengeti public campsite. The sounds of the bush at night — lion roars, hyena calls, the chorus of nightjars — make for an unforgettable experience. Group dinner over the campfire.',
    'Lion coalition, leopard on kopje, cheetah family, spotted hyena, wildebeest, plains zebra, hippopotamus, Nile crocodile, topi, eland, Thomson''s and Grant''s gazelle, elephant, giraffe',
    'Seronera Valley wildlife-rich grasslands and riverine forest, granite kopje formations rising dramatically from the flat plains, Seronera River hippo pools and sandbanks, the endless Serengeti horizon in every direction',
    'The full day in Seronera is the highlight of the Serengeti experience. The key is patience — stop and stay with animals rather than moving on. Predator sightings build over time. The afternoon hippo pool at the Seronera bridge is a reliable and spectacular stop.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_35_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_4, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day Seronera Valley game drives, overnight public campsite', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_35, 5, 'Day 5', 'Serengeti to Tarangire Transit — Across Northern Tanzania',
    'A long drive day crossing from the Serengeti through the Ngorongoro Conservation Area and south to the Tarangire region. En route, wildlife is visible in both the Serengeti exit and the Ngorongoro Conservation Area highlands — this transit day still delivers sightings while covering ground for the final days of the tour.',
    'Early morning departure from the Serengeti campsite after packing down. Exit the Serengeti through Naabi Gate with wildlife spotting on the way out. Re-enter the Ngorongoro Conservation Area and drive east through the highlands, watching for eland, Maasai cattle, and highland raptors.',
    'Continue the drive east and south through Karatu and the coffee-growing highlands, passing through Makuyuni and down to the Tarangire corridor. Game viewing en route as the landscape transitions from highland back to acacia savanna. Arrive at budget camp near Tarangire in the late afternoon.',
    'Set up tents and prepare a communal dinner. The area around Tarangire supports elephant populations that roam outside the park boundary — listen for the low rumbles of elephant herds in the bush surrounding camp.',
    'Elephant outside park boundary in the evening, eland and Maasai cattle on the Ngorongoro highlands, augur buzzard, Montagu''s harrier on the highlands, giraffe and zebra on the approach to Tarangire',
    'Serengeti plains on early morning departure, Ngorongoro highland moorland, Karatu coffee plantation hillsides, the broad Tarangire ecosystem acacia savanna on arrival',
    'This is the longest driving day of the tour at approximately 300km. It is best broken with a picnic lunch stop in the highlands. The transit through the Ngorongoro Conservation Area still requires paying conservation fees. Arrive camp before dark to set up tents.',
    'Serengeti', 'Tarangire', 300, 1, 'B,L,D', @now, @now);
SET @day_35_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_5, @park_serengeti, 'TRANSIT', 1, NULL, NULL, 'Exit Serengeti via Naabi Gate on transit south', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_5, @park_ngorongoro, 'TRANSIT', 2, NULL, NULL, 'Transit through Ngorongoro Conservation Area highlands en route to Tarangire', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_35, 6, 'Day 6', 'Tarangire Game Drive & Transit to Mkomazi',
    'A morning game drive in Tarangire National Park among the legendary elephant herds and ancient baobab landscapes, then drive east to the remote and wild Mkomazi National Park on the slopes of the Pare Mountains. Two very different parks in one day — the abundant and accessible Tarangire giving way to the raw wilderness of Mkomazi.',
    'Early morning entry into Tarangire National Park for a morning game drive along the Tarangire River. Watch elephant herds interact at the river bank, spot giraffes browsing between the baobab trees, search for lion and leopard in the riverine woodland. This is Tarangire at its most active.',
    'Exit Tarangire after lunch and drive east toward Same and the Pare Mountain foothills. The landscape transitions from the broad Tarangire plains to the drier Acacia-Commiphora scrub of eastern Tanzania. Enter Mkomazi National Park as the afternoon light softens over the Pare Mountains.',
    'Set up camp inside Mkomazi or at the park boundary. Listen to the sounds of a true wilderness park — African wild dog may howl in the distance and the calls of night birds fill the dry-bush darkness. Simple but memorable camp dinner under eastern Tanzania''s spectacular night sky.',
    'Morning: elephant herds, lion, giraffe, zebra, fringe-eared oryx, lesser kudu in Tarangire. Afternoon/evening: lesser kudu, gerenuk, dik-dik, African wild dog, eland, hartebeest, Friedmann''s lark in Mkomazi',
    'Tarangire River morning light with elephants and baobabs, Masai Steppe panorama, Pare Mountains dramatic backdrop to Mkomazi, remote Acacia-Commiphora savanna of eastern Tanzania',
    'Mkomazi is one of Tanzania''s least-visited national parks — you may have the entire park to yourself. The African wild dog population is one of the reasons Mkomazi was established as a national park. The drive from Tarangire to Mkomazi takes approximately 2.5-3 hours.',
    'Tarangire', 'Mkomazi', 110, 1, 'B,L,D', @now, @now);
SET @day_35_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_6, @park_tarangire, 'DAY_TRIP', 1, '06:00', '12:00', 'Morning game drive Tarangire River and elephant herds', @now);
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_6, @park_mkomazi, 'SLEEP_OVER', 2, '15:00', NULL, 'Arrive Mkomazi afternoon, overnight camp', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_35, 7, 'Day 7', 'Mkomazi Rhino Sanctuary Morning & Return to Arusha',
    'A final morning in the remote wilderness of Mkomazi National Park, including a visit to the renowned Mkomazi Rhino Sanctuary where black rhinos are being bred for reintroduction. Then drive back to Arusha, completing a grand circuit of northern Tanzania''s most remarkable wild areas.',
    'Early morning game drive in Mkomazi through the Acacia-Commiphora bushland, searching for lesser kudu, gerenuk, and the elusive African wild dog. Visit the Mkomazi Rhino Sanctuary for a guided tour of the black rhino conservation and breeding programme — a deeply moving experience at the frontline of African conservation.',
    'Depart Mkomazi and drive back to Arusha (approximately 110km, 2 hours). The road passes through the foothills of the Pare and Usambara Mountains with views of traditional Pare villages and small-scale farming on the mountain terraces.',
    NULL,
    'Black rhinoceros in the sanctuary, African wild dog, lesser kudu, gerenuk, fringe-eared oryx, eland, hartebeest, dik-dik, Friedmann''s lark, martial eagle, secretary bird',
    'Mkomazi Acacia-Commiphora wilderness, Pare Mountains framing the park landscape, Usambara Mountain range on the horizon, remote and wild eastern Tanzania landscape',
    'The rhino sanctuary visit requires advance booking and adds approximately 2 hours to the morning. Tipping your driver-guide appropriately at the end of the tour is important — this has been a week of long days and expert guidance. Arrive back in Arusha by early afternoon.',
    'Mkomazi', 'Arusha', 110, 0, 'B,L', @now, @now);
SET @day_35_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_35_7, @park_mkomazi, 'DAY_TRIP', 1, '06:00', '11:00', 'Morning game drive and rhino sanctuary visit before departure', @now);

-- ============================================================
-- ITINERARY 36: Safari & Zanzibar Beach Honeymoon (8 days, 7 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 1, 'Day 1', 'Arusha → Tarangire — A Romantic Arrival in the Wild',
    'Begin your honeymoon safari with a scenic drive from Arusha into the heart of Tarangire National Park. Ancient baobab trees tower above the savanna as elephant herds drift to the river — the perfect opening chapter to your romantic Tanzania adventure.',
    'Depart Arusha mid-morning after hotel breakfast. Drive southwest to Tarangire National Park (approximately 2.5 hours). Enter the park at Kuro Gate and begin your first game drive along the Tarangire River, watching elephant herds bathe and play in the shallow waters.',
    'Romantic bush picnic lunch beneath a giant baobab tree. Continue afternoon game drive through the central circuits — spot lion, giraffe, zebra, and buffalo against the backdrop of rolling savanna. Your luxury lodge is perfectly positioned for sundowner views.',
    'Arrive at your luxury lodge for a welcome champagne sundowner on the private deck. Private candlelit dinner under the stars — your first night in the African bush.',
    'African elephant (large herds), lion, leopard, giraffe, zebra, wildebeest, buffalo, fringe-eared oryx, lesser kudu, impala, 550+ bird species',
    'Ancient baobab tree silhouettes at golden hour, Tarangire River valley teeming with wildlife, termite mound-dotted savanna, sweeping Masai Steppe views',
    'Tarangire is famous for the largest elephant herds in northern Tanzania. Dry season visits (June–October) offer the most dramatic concentrations of wildlife along the river.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_36_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_36_1, @park_tarangire, 'SLEEP_OVER', 1, '10:00', NULL, 'Romantic arrival game drive along Tarangire River, overnight in park', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 2, 'Day 2', 'Tarangire → Ngorongoro Crater Rim — Sundowners at the Edge of the World',
    'Bid farewell to Tarangire and journey northward to the Ngorongoro Conservation Area. The dramatic rim of the world''s largest intact volcanic caldera awaits, offering a breathtaking sunset and an intimate dinner with views stretching into Africa''s greatest wildlife sanctuary.',
    'Early morning game drive from camp at sunrise — the golden light illuminates baobabs and elephants heading to water. Enjoy a leisurely breakfast back at lodge before checking out and driving north through Mto wa Mbu.',
    'En route, stop briefly at Lake Manyara viewpoint for sweeping Rift Valley panoramas. Continue driving through the lush highlands to the Ngorongoro Conservation Area gate and up to the crater rim.',
    'Arrive at rim lodge in time for a private sundowner at the crater viewpoint — champagne glasses in hand as the caldera glows in the fading light. Gourmet dinner at the lodge with crater views.',
    'Elephant, lion, giraffe, baboon and vervet monkey along the Manyara corridor, Maasai cattle herds in the conservation area, crater rim wildlife including spotted hyena and buffalo',
    'Ngorongoro Crater rim panorama at sunset — one of Africa''s most romantic viewpoints, Great Rift Valley escarpment, highland forest along the rim road',
    'The Ngorongoro Crater rim sits at 2,300m — pack a warm layer for chilly evenings and the most magical sundowner experience in Tanzania.',
    'Tarangire', 'Ngorongoro', 130, 1, 'B,L,D', @now, @now);
SET @day_36_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_36_2, @park_ngorongoro, 'SLEEP_OVER', 1, '15:00', NULL, 'Scenic rim drive arrival, sundowner at crater viewpoint, overnight rim lodge', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 3, 'Day 3', 'Ngorongoro Crater Floor — The Big Five Together',
    'Descend 600 metres into the Ngorongoro Crater for a full day among the densest concentration of wildlife on earth. The world''s largest intact volcanic caldera is your private paradise — search for all of the Big Five in this ancient bowl of life.',
    'Pre-dawn champagne flask and blanket provided as you descend the crater wall at first light. Golden hour on the crater floor — lion prides wake, hyenas return from the night, and black rhino graze in the open grassland. Morning game drive focuses on the Lerai Forest for elephants and the open plains for predators.',
    'Luxury bush picnic lunch beside the Ngoitokitok Springs hippo pool. Afternoon game drive along the shores of Lake Magadi for thousands of flamingos and wading birds. Search for the crater''s famous black rhino and observe buffalo herds moving across the caldera floor.',
    'Ascend the crater rim in late afternoon. Romantic dinner at the lodge with crater views by candlelight.',
    'Black rhino, lion, African elephant, Cape buffalo, spotted hyena, cheetah, wildebeest, zebra, flamingo, crowned crane, jackal, serval cat, Grant''s and Thomson''s gazelle',
    'The vast caldera floor stretching 20km across, Lake Magadi''s shimmering pink flamingo carpet, Lerai Forest ancient yellow-bark acacia trees, dramatic 600m crater walls enclosing the world below',
    'The Ngorongoro Crater has one of the highest densities of predators in Africa. Early descent maximises Big Five sightings. Crater floor temperatures are warmer than the rim.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_36_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_36_3, @park_ngorongoro, 'DAY_TRIP', 1, '06:30', '17:00', 'Full day crater floor Big Five game drive, overnight rim lodge', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 4, 'Day 4', 'Ngorongoro → Serengeti — Into the Endless Plains',
    'Leave the crater behind and cross into the Serengeti, Africa''s most iconic wilderness. The drive through the Ngorongoro Conservation Area reveals Maasai herders and wildlife sharing the same land before the boundless Serengeti plains open before you.',
    'Morning departure from the rim lodge. Drive through the Ngorongoro highlands and Olduvai Gorge area — optional brief stop at the Olduvai Gorge Museum, cradle of humankind. Enter the Serengeti through the Naabi Hill Gate.',
    'Afternoon game drive across the Serengeti''s central Seronera Valley — the predator capital of Africa. Spot lion prides lazing under acacia trees, cheetahs scanning the plains, and leopards draped over branches. Arrive at your luxury tented camp.',
    'Exclusive private bush dinner under a canopy of stars — lanterns, fine dining, and the sounds of the African night. Your most romantic evening of the safari.',
    'Lion, cheetah, leopard, elephant, giraffe, buffalo, wildebeest, zebra, Thomson''s gazelle, topi, eland, wild dog, serval, caracal',
    'The Serengeti''s vast golden plains stretching to the horizon, acacia-dotted Seronera Valley, kopje rock formations silhouetted at sunset, the Milky Way over the endless savanna',
    'The Serengeti ecosystem hosts the Great Migration — over two million wildebeest, zebra, and gazelle moving in a continuous cycle. Witnessing this spectacle depends on the season.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_36_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_36_4, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon arrival game drive, bush dinner, overnight luxury camp', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 5, 'Day 5', 'Full Day Serengeti — Balloon Dawn & Predator Drama',
    'An entire day in the heart of the Serengeti with the option to float above the plains at sunrise in a hot air balloon. This is the ultimate honeymoon day — romance, adventure, and wildlife in perfect measure.',
    'Optional pre-dawn hot air balloon launch — float silently over the Serengeti at sunrise, watching herds of wildebeest and zebra move far below. A Champagne breakfast in the bush follows the landing. For non-balloon guests, an early dawn game drive chases the action before the heat of the day.',
    'Morning and afternoon game drives with your private guide through the central plains and kopje areas. Search for cheetah mothers with cubs, leopards resting on rocky outcrops, and the endless herds of the Serengeti. Return to camp for afternoon rest.',
    'Romantic sundowner at a scenic kopje with panoramic views. Bush dinner under the stars at your luxury camp — local Maasai cultural evening entertainment available.',
    'Lion (prides and males), cheetah with cubs, leopard, spotted hyena, African wild dog, elephant, giraffe, Cape buffalo, wildebeest, zebra, eland, topi, impala, bat-eared fox',
    'Hot air balloon views of the endless Serengeti plains at sunrise, kopje rock formations hosting diverse wildlife, the central plains teeming with the Great Migration herds',
    'Hot air balloon flights must be booked and confirmed in advance. The balloon experience is highly weather-dependent but is rated among the most romantic experiences in Africa.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_36_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_36_5, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives, optional balloon at dawn, overnight luxury camp', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 6, 'Day 6', 'Serengeti Morning → Fly to Zanzibar — From Plains to Paradise',
    'Enjoy a final morning game drive on the Serengeti plains before flying to Zanzibar — the spice island jewel of the Indian Ocean. By afternoon you are walking on white sand with turquoise water lapping at your feet.',
    'Early morning game drive at dawn — one last encounter with lions, elephants, and the endless plains. Return to camp for breakfast and check-out. Transfer to Seronera Airstrip for your charter flight to Zanzibar.',
    'Arrive Zanzibar airport early afternoon. Transfer to your beachfront resort. Afternoon at leisure on the private beach — swim in the warm Indian Ocean, enjoy a couples'' beach massage, or simply relax with cocktails as the azure waters sparkle before you.',
    'Sunset cocktails on the beach. Candlelit seafood dinner at the resort restaurant — the freshest catch grilled to perfection.',
    'Final Serengeti game drive: lion, cheetah, elephant, zebra, wildebeest, topi; Zanzibar: sea turtles (seasonal), dolphins (optional), shore birds',
    'Dawn light on the Serengeti''s golden plains, aerial views of the Great Rift Valley on the flight to Zanzibar, Zanzibar''s turquoise Indian Ocean and powder-white beaches',
    'The flight from Serengeti to Zanzibar takes approximately 2–3 hours with a stop. Pack beach essentials in carry-on luggage as checked bags travel separately.',
    'Serengeti', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_36_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_36_6, @park_serengeti, 'DAY_TRIP', 1, '06:00', '10:00', 'Final morning game drive before fly-out to Zanzibar', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 7, 'Day 7', 'Zanzibar — Jozani Forest, Spice Tour & Stone Town Sunset',
    'Explore the enchanting soul of Zanzibar — ancient forest inhabited by rare red colobus monkeys, fragrant spice plantations, and the romantic labyrinth of Stone Town at dusk. A day that blends nature, culture, and timeless romance.',
    'Morning drive to Jozani Chwaka Bay National Park. Guided forest walk through the indigenous groundwater forest to meet the endemic Zanzibar red colobus monkey. Walk the mangrove boardwalk over Chwaka Bay before returning for a fresh coconut on the roadside.',
    'Visit a traditional spice plantation — taste and smell cloves, vanilla, cinnamon, nutmeg, and cardamom fresh from the plant. Your guide crafts fresh fruit hats from palm leaves. Lunch at a local restaurant.',
    'Late afternoon drive to Stone Town. Stroll hand-in-hand through the UNESCO-listed labyrinthine alleys, past carved wooden doors and ancient mosques. Watch the sunset from the waterfront Forodhani Gardens before a romantic seafood dinner at a cliffside restaurant.',
    'Zanzibar red colobus monkey (endemic, endangered), Sykes'' monkey, Ader''s duiker, bush pig, giant land crab, mangrove kingfisher, over 50 butterfly species',
    'Ancient indigenous forest canopy alive with monkeys, mangrove boardwalk reflecting in tidal Chwaka Bay, Stone Town''s Arabic-Swahili architectural labyrinth, dramatic Indian Ocean sunset from the seafront',
    'The Zanzibar red colobus is found only on Zanzibar — one of Africa''s rarest primates. Stone Town is a UNESCO World Heritage Site. Book the seafood dinner restaurant in advance during high season.',
    'Zanzibar', 'Zanzibar', 80, 1, 'B,L,D', @now, @now);
SET @day_36_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_36_7, @park_jozani, 'DAY_TRIP', 1, '08:30', '12:00', 'Jozani Forest red colobus walk and mangrove boardwalk', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_36, 8, 'Day 8', 'Zanzibar Beach Morning & Departure',
    'Savour your final morning on Zanzibar''s white sand beaches before transferring to the airport. A final swim in the turquoise Indian Ocean, one last beach breakfast, and wonderful memories of a honeymoon that blended the wild savanna with island paradise.',
    'Final leisurely breakfast at the resort. Last swim and beach walk. Collect any purchased spices or souvenirs. Check out and transfer to Zanzibar''s Abeid Amani Karume International Airport for your onward flight.',
    NULL,
    NULL,
    'Shore birds, possibility of dolphin sighting on transfer, tropical reef fish visible from beach',
    'Last lingering views of Zanzibar''s powder-white beaches and impossibly turquoise Indian Ocean',
    'Airport transfer time depends on flight schedule. Allow at least 2.5 hours before departure for international flights from Zanzibar.',
    'Zanzibar', 'Zanzibar Airport', 35, 0, 'B', @now, @now);
SET @day_36_8 = LAST_INSERT_ID();

-- ============================================================
-- ITINERARY 37: Kilimanjaro Summit & Crater Safari Combo (9 days, 8 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 1, 'Day 1', 'Machame Gate — Into the Kilimanjaro Rainforest',
    'Begin your Kilimanjaro Machame Route ascent with a drive to the park gate and a magical first trek through the lush equatorial rainforest. The sounds and scents of the mountain envelope you as you climb to your first camp at 3,000 metres.',
    'Depart Arusha after an early breakfast. Drive to Machame Gate (95km, approximately 2.5 hours). Complete registration, meet your mountain crew, and check equipment. Begin trekking at approximately 10:00 AM through dense rainforest — giant heathers, hagenia trees draped in old-man''s beard lichen, and the calls of endemic birds.',
    'Steady climb through the montane rainforest zone. The trail gains elevation gently at first then steepens through the heath zone. Packed lunch eaten on the trail. Continue climbing through the heathland transition zone.',
    'Arrive at Machame Camp (3,000m) in the early evening. Hot dinner prepared by your mountain chef. Briefing by your lead guide on the days ahead. Early to bed to begin acclimatisation.',
    'Black-and-white colobus monkey, blue monkey, white-necked raven, Hartlaub''s turaco, silvery-cheeked hornbill, various sunbirds, bushbuck, duiker',
    'Rainforest canopy dripping with moss and epiphytes, giant tree ferns and heather towering overhead, first glimpses of Kibo''s snowcapped peak through breaks in the forest',
    'The Machame Route is known as the ''Whiskey Route'' for its steeper, more challenging profile — but the scenery is outstanding. Proper broken-in boots and trekking poles are essential from day one.',
    'Arusha', 'Machame Camp', 95, 1, 'L,D', @now, @now);
SET @day_37_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_1, @park_kilimanjaro, 'SLEEP_OVER', 1, '10:00', NULL, 'Machame Gate to Machame Camp trek through rainforest, overnight at 3,000m', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 2, 'Day 2', 'Machame Camp → Shira Plateau — Above the Clouds',
    'Trek out of the heath zone and onto the vast Shira Plateau — a high-altitude moorland with extraordinary views of Kibo and Mawenzi peaks. You are now above the clouds and entering the alpine desert.',
    'Wake at Machame Camp (3,000m) to breakfast with mountain views. Depart camp and climb steeply through the heather-moorland transition zone. The vegetation changes dramatically from giant heathers to groundsels and lobelias as the trail ascends.',
    'Cross the open moorland of the Shira Ridge with stunning panoramic views. Packed lunch at Shira Cave or a scenic ridge viewpoint. Continue across the Shira Plateau to Shira Camp.',
    'Arrive Shira Camp (3,840m) in the afternoon. Rest and hydration essential for acclimatisation. Hot dinner served in the mess tent. Guide discusses the route ahead over Lava Tower.',
    'Alpine swifts wheeling over the plateau, common kestrel, moorland francolin, scarce signs of eland tracks, rock hyrax near camp',
    'Sweeping views of Kibo''s ice-capped summit from the Shira Plateau, the vast moorland stretching in all directions above the clouds, giant groundsels and lobelia plants unique to African high-altitude moorland',
    'Altitude gain is significant today — drink at least three litres of water. Walk at a slow, steady pace (''pole pole'') and report any headaches or nausea to your guide immediately.',
    'Machame Camp', 'Shira Camp', 0, 1, 'B,L,D', @now, @now);
SET @day_37_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_2, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Machame Camp to Shira Plateau trek, overnight at 3,840m', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 3, 'Day 3', 'Shira → Lava Tower → Barranco Valley — Acclimatise High, Sleep Low',
    'Trek to the Lava Tower at 4,600m for crucial acclimatisation before descending to the lush Barranco Valley. This ''climb high, sleep low'' profile dramatically improves your summit success rate. The Lava Tower is a dramatic volcanic plug rising from the alpine desert.',
    'Early breakfast at Shira Camp. Begin trekking across the Shira Plateau toward the Southern Circuit. The landscape becomes increasingly arid and otherworldly as you ascend toward the Lava Tower through high-altitude desert.',
    'Reach Lava Tower (4,600m) for lunch — this is a critical acclimatisation point. Rest here for at least one hour. Then descend 650 metres through dramatic canyon scenery to the lush Barranco Valley, filled with giant groundsels and Dendrosenecio plants.',
    'Arrive Barranco Camp (3,950m) in the late afternoon. The dramatic Breach Wall towers above camp. Hot dinner and early rest.',
    'Alpine chats, streaky seed-eaters, augur buzzard soaring on thermals, resident rock hyrax around camp, occasional leopard spoor in the valley',
    'The iconic Lava Tower volcanic plug, dramatic alpine desert landscape at 4,600m, descent into the lush Barranco Valley with giant groundsels, the vertical Breach Wall looming above camp at sunset',
    'The Lava Tower acclimatisation stop is vital — do not rush this section. The descent to Barranco feels dramatic after the barren upper slopes. Layers are essential as temperatures fluctuate wildly.',
    'Shira Camp', 'Barranco Camp', 0, 1, 'B,L,D', @now, @now);
SET @day_37_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_3, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Shira to Lava Tower acclimatisation then descent to Barranco Camp, overnight at 3,950m', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 4, 'Day 4', 'Barranco Wall → Karanga Valley — The Great Scramble',
    'Tackle the famous Barranco Wall — a thrilling hands-and-feet scramble up a near-vertical 300m volcanic cliff face. This is the most exciting and memorable trekking challenge on the Machame Route, rewarded with extraordinary views from the top.',
    'Early morning departure from Barranco Camp. The Barranco Wall begins immediately and requires a careful hands-on scramble in sections — your guide leads through the safest route. The wall is not technically difficult but is exhilarating. Reach the top for expansive views of Kibo and the southern glaciers.',
    'Continue along the Southern Circuit with stunning views across the ice-capped summit. Trek through the Karanga Valley and up the far side. Packed lunch at a rocky viewpoint. Arrive Karanga Camp (3,995m) by mid-afternoon.',
    'Rest and acclimatisation at Karanga Camp. Hot dinner and early night — summit night is approaching.',
    'Augur buzzard, alpine swift, scarce flora adapted to extreme altitude — Helichrysum everlastings, lobelia in the valley',
    'The dramatic Barranco Wall scramble with Kibo''s ice fields directly above, panoramic southern glacier views from the top of the wall, Karanga Valley''s rugged gorge landscape',
    'The Barranco Wall scramble is safe with a guide but can feel intimidating — take your time and follow instructions. Gloves are helpful. The sense of achievement at the top is immense.',
    'Barranco Camp', 'Karanga Camp', 0, 1, 'B,L,D', @now, @now);
SET @day_37_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_4, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Barranco Wall scramble to Karanga Camp, overnight at 3,995m', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 5, 'Day 5', 'Karanga → Barafu Base Camp — The Final Countdown',
    'A shorter but important trekking day, ascending from Karanga to Barafu Base Camp — the high camp from which tonight''s summit attempt launches. The landscape is now pure volcanic rock and scree, stripped of all vegetation.',
    'Late morning departure from Karanga Camp after a good breakfast and extended rest. Short trek (3–4 hours) across the high alpine desert to Barafu Camp. The terrain is increasingly rocky with loose scree requiring careful footing.',
    'Arrive Barafu Camp (4,673m) by early afternoon. Rest is essential — the summit attempt begins at midnight. Hot lunch served at camp. Thorough kit check with your guide and briefing on summit night procedure.',
    'Early dinner at 17:00. In bed by 18:00. Wake-up call at midnight for summit departure. Sleep is difficult at this altitude but rest is critical.',
    'Almost no wildlife at this extreme altitude — white-necked raven occasionally visits camp hoping for food scraps',
    'First views of the Rebmann and Ratzel glaciers from Barafu, the volcanic scree and lava rock moonscape of the high alpine desert, Mawenzi peak framed against the sky, vast plains of Tanzania stretching below',
    'Hydrate aggressively throughout the day. Eat even if appetite is suppressed. Prepare all summit clothing before sleeping — temperatures at the summit will be -15°C to -25°C. Summit night success rate improves dramatically with good rest here.',
    'Karanga Camp', 'Barafu Camp', 0, 1, 'B,L,D', @now, @now);
SET @day_37_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_5, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Karanga to Barafu Base Camp, rest for midnight summit departure, overnight at 4,673m', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 6, 'Day 6', 'Summit Uhuru Peak (5,895m) → Descent to Mweka Gate → Arusha',
    'The crown of Africa awaits. Depart at midnight, climb through bitter cold and thin air to Stella Point on the crater rim, and then make the final push to Uhuru Peak — the highest point on the African continent at 5,895m. Descend to the lower slopes and drive to Arusha.',
    'Midnight departure from Barafu in head-torch light. Slow, steady ascent up the steep scree and snow slopes of Kibo. Reach Stella Point (5,756m) at crater rim for a spectacular sunrise above the clouds. Push on to Uhuru Peak (5,895m) — the roof of Africa. Celebrate with your crew.',
    'Begin descent via the Mweka Route — steep and loose but fast. Descend through the heath zone to Mweka Camp for a celebratory lunch and certificate presentation. Continue descent to Mweka Gate and meet your vehicle.',
    'Drive back to Arusha. Overnight in a comfortable Arusha hotel — hot shower, real bed, and a celebratory dinner.',
    'High altitude birds on descent: augur buzzard, crowned eagle, alpine swift; rainforest wildlife on the lower Mweka Route: blue monkey, black-and-white colobus',
    'The Uhuru Peak crater rim and glaciers at sunrise, the vast caldera of Kibo at the summit, views stretching across Tanzania, Kenya, and on clear days to the Indian Ocean, the green savanna plains far below',
    'Summit night is the hardest night of the climb — altitude sickness, cold, and fatigue are all factors. Your guide will assess fitness throughout. Descent to lower altitude rapidly reverses most altitude symptoms.',
    'Barafu Camp', 'Arusha', 0, 1, 'B,L,D', @now, @now);
SET @day_37_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_6, @park_kilimanjaro, 'DAY_TRIP', 1, '00:00', '16:00', 'Summit Uhuru Peak via Machame Route, descend Mweka Route to gate, drive to Arusha', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 7, 'Day 7', 'Rest Day Arusha → Drive to Ngorongoro Crater Rim',
    'A welcome morning of rest in Arusha after the Kilimanjaro summit, before driving to the Ngorongoro Conservation Area in the afternoon. Your body recovers as you transition from the world''s highest freestanding mountain to the world''s largest intact volcanic caldera.',
    'Morning of complete rest at the Arusha hotel. Sleep in, enjoy a leisurely breakfast, use the swimming pool, and arrange a post-climb massage. Optional visit to the Cultural Heritage Centre or a local coffee plantation.',
    'Early afternoon departure from Arusha. Drive west through the Rift Valley to the Ngorongoro Conservation Area (190km, approximately 3.5 hours). Ascend the crater rim road through highland forest.',
    'Arrive at crater rim lodge as the sun drops. Sunset drinks overlooking the Ngorongoro Crater far below. Dinner at the lodge.',
    'Buffalo, baboon, and hyena sometimes seen near the crater rim road; Maasai cattle and donkeys in the conservation area buffer zone',
    'The dramatic winding rim road ascending through highland forest, first glimpse of the Ngorongoro Crater in the late afternoon light, sweeping conservancy views as the sun sets',
    'The rest day in Arusha is essential after the physical demands of the summit. Even fit trekkers will feel the cumulative altitude fatigue. Hydration and nutrition remain important.',
    'Arusha', 'Ngorongoro', 190, 1, 'B,L,D', @now, @now);
SET @day_37_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_7, @park_ngorongoro, 'SLEEP_OVER', 1, '16:00', NULL, 'Afternoon arrival at crater rim, overnight rim lodge', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 8, 'Day 8', 'Ngorongoro Crater Descent — Big Five After the Big Summit',
    'Having stood on the roof of Africa, now descend into one of the world''s greatest wildlife arenas. The Ngorongoro Crater offers a dramatically different adventure — sitting in a vehicle watching lions and rhinos in the world''s most perfect natural amphitheatre.',
    'Early morning descent into the Ngorongoro Crater at first light. The transition from the cold rim to the warmer crater floor is invigorating. Begin game drive in the Lerai Forest area — elephant families move between the trees as hippos wallow in the dawn light.',
    'Game drive continues across the crater floor. Extended search for the rare black rhino in the open grassland. Picnic lunch at the Ngoitokitok hippo pool — dozens of hippos crowd the waterhole. Afternoon drive along Lake Magadi for flamingos and wading birds.',
    'Ascend the crater rim in late afternoon. Dinner at the rim lodge with a sense of two great African adventures now accomplished.',
    'Black rhino, lion, spotted hyena, African elephant, Cape buffalo, hippo, flamingo, cheetah, wildebeest, zebra, jackal, serval, crowned crane',
    'Dawn on the crater floor with mist rising from the valley, the Lerai Forest in morning light, Lake Magadi''s flamingo carpet, the dramatic caldera walls enclosing this extraordinary wildlife concentration',
    'After the exertion of Kilimanjaro, today is a gentle seated safari — let the wildlife come to you. The crater is a fitting reward for reaching Africa''s highest summit.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_37_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_37_8, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '16:30', 'Full day crater floor game drive after Kilimanjaro summit', @now);

-- Day 9
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_37, 9, 'Day 9', 'Ngorongoro → Arusha — Farewell to the Crater Highlands',
    'A final breakfast on the Ngorongoro Crater rim, then a scenic drive back to Arusha. Reflect on an extraordinary nine days combining the summit of Africa''s highest mountain with the wildlife splendour of its greatest crater.',
    'Leisurely breakfast at the rim lodge with one final view of the Ngorongoro Crater stretching below. Check out and begin the drive back to Arusha through the Ngorongoro Conservation Area and down into the Great Rift Valley.',
    NULL,
    NULL,
    'Maasai cattle and wildlife shared landscapes on the conservation area drive, possible roadside baboon and vervet monkey sightings, Rift Valley birdlife',
    'Final panoramic views of the Ngorongoro Crater rim from the lodge, the Rift Valley escarpment descent, Arusha city with Meru and Kilimanjaro visible on clear days',
    'Allow 3.5–4 hours driving time to Arusha. International flight departures from Kilimanjaro International Airport should be scheduled for late afternoon or evening on Day 9 at the earliest.',
    'Ngorongoro', 'Arusha', 190, 0, 'B', @now, @now);
SET @day_37_9 = LAST_INSERT_ID();

-- ============================================================
-- ITINERARY 38: Ultimate Tanzania — Safari & Spice Island (10 days, 9 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 1, 'Day 1', 'Arusha → Tarangire — Elephant Kingdom Arrival',
    'Your ultimate Tanzania adventure opens in the land of ancient baobabs and the largest elephant herds in northern Tanzania. Tarangire National Park sets the standard high from the very first afternoon — a landscape so rich it will leave you breathless.',
    'Depart Arusha mid-morning and drive southwest to Tarangire National Park (120km, approximately 2.5 hours). Pick up your park permits at Kuro Gate and begin your first game drive along the Tarangire River.',
    'Afternoon game drive through the iconic baobab-studded savanna. Watch elephant herds of 50 or more descend to the river to drink. Scan the baobab canopies for leopard and large pythons. Visit the swamp areas teeming with herons, storks, and ibis.',
    'Arrive at your ultra-luxury lodge for a welcome reception with sundowner cocktails. Gourmet dinner with table service and fine wines. Briefing on the days ahead.',
    'African elephant (massive herds), lion, leopard, giraffe, zebra, wildebeest, buffalo, fringe-eared oryx, lesser kudu, gerenuk, ostrich, 550+ bird species including yellow-collared lovebird',
    'Ancient baobab silhouettes against the burning sunset, Tarangire River flanked by dense vegetation alive with wildlife, the vast Masai Steppe stretching to the horizon',
    'Tarangire is at its wildlife peak during the dry season when all animals concentrate along the river. This ultra-luxury itinerary uses private conservancy camps for exclusive access.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_38_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_1, @park_tarangire, 'SLEEP_OVER', 1, '11:00', NULL, 'Afternoon arrival game drive, overnight ultra-luxury lodge in park', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 2, 'Day 2', 'Full Day Tarangire — Deep Dive into Baobab Country',
    'Devote an entire day to Tarangire — exploring circuits rarely visited and spending unhurried time with the park''s extraordinary wildlife. A walking safari with an armed ranger reveals the bush at ground level in an intimate way no vehicle can match.',
    'Pre-dawn wake-up for a bush walking safari with an armed Maasai ranger guide. Experience the sounds, smells, and tracks of the African bush on foot. Watch elephant families at close range and track lion spoor through the riverine thickets.',
    'Return to camp for brunch. Afternoon game drive into the southern circuits of the park — the Silale Swamp and Gursi areas offer exceptional bird watching and large concentrations of buffalo. Tree-climbing pythons coil in the baobab branches overhead.',
    'Sundowner in the bush beside the Tarangire River. Dinner at camp with a Maasai cultural performance around the campfire.',
    'African elephant, lion, leopard, cheetah (open areas), African wild dog (occasional), buffalo (large herds), fringe-eared oryx, lesser kudu, giraffe, zebra, over 550 birds',
    'The Silale Swamp panorama alive with waterbirds, ancient baobab groves that have witnessed centuries of wildlife, the Tarangire River snaking through the valley, termite castle formations across the plains',
    'The walking safari must be booked in advance and requires a minimum age of 16 years. Closed-toe shoes mandatory. This ultra-luxury camp has full private guide and vehicle exclusivity.',
    'Tarangire', 'Tarangire', 0, 1, 'B,L,D', @now, @now);
SET @day_38_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_2, @park_tarangire, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day in park including walking safari and game drives, overnight ultra-luxury lodge', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 3, 'Day 3', 'Tarangire → Lake Manyara — Tree-Climbing Lions & Rift Valley Views',
    'Move northward from Tarangire to the jewel-like Lake Manyara National Park, set at the foot of the Great Rift Valley escarpment. Compact yet extraordinarily diverse, Manyara offers the remarkable spectacle of lions reclining in acacia trees above your vehicle.',
    'Morning game drive exit from Tarangire. Drive north to Lake Manyara National Park (70km). Enter through the Manyara groundwater forest — a lush cathedral of fig trees and mahogany alive with troops of baboon and blue monkey.',
    'Full afternoon game drive through the park''s diverse habitats: dense forest, open floodplain, and the shallow alkaline lake shore. Search for the park''s famous tree-climbing lions in the yellow fever acacias. Visit the hot springs and hippo pools.',
    'Drive to your luxury lodge on the Manyara escarpment rim for sunset cocktails with sweeping Rift Valley and lake views. Fine dining dinner.',
    'Tree-climbing lions (unique behaviour), flamingo (seasonal thousands), elephant, baboon, blue monkey, hippo, buffalo, giraffe, impala, over 400 bird species including stork, pelican, and fish eagle',
    'The Great Rift Valley escarpment towering above the park, alkaline Lake Manyara reflecting the sky, the lush groundwater forest contrasting with the open floodplain, hot springs steaming near the lakeshore',
    'Lake Manyara is one of Africa''s premier bird watching destinations. The tree-climbing lion is a behavioural phenomenon unique to this park — sightings are frequent but not guaranteed.',
    'Tarangire', 'Manyara', 70, 1, 'B,L,D', @now, @now);
SET @day_38_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_3, @park_manyara, 'DAY_TRIP', 1, '10:00', '17:00', 'Full day game drive, overnight at escarpment lodge', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 4, 'Day 4', 'Lake Manyara → Ngorongoro Crater — The Eighth Wonder',
    'Climb from the Rift Valley floor to the rim of the Ngorongoro Crater — the world''s largest intact volcanic caldera and one of the most extraordinary wildlife concentrations on earth. Descend into the crater for an unforgettable day of Big Five encounters.',
    'Depart the Manyara escarpment lodge and drive through the highlands to Ngorongoro Conservation Area (60km). Ascend the crater rim road through highland forest alive with buffalo and elephant. Check in to your luxury rim lodge.',
    'After a brief rest, descend into the Ngorongoro Crater for an afternoon game drive. The crater floor reveals lions resting on open grasslands, black rhino grazing near the Lerai Forest, and thousands of flamingos wading along Lake Magadi''s shores.',
    'Ascend the crater rim before sunset. Dinner at the luxury rim lodge with panoramic views over the caldera as stars emerge above the ancient volcano.',
    'Black rhino, lion, spotted hyena, African elephant, Cape buffalo, hippo, flamingo, cheetah, wildebeest, zebra, crowned crane, jackal, serval, golden jackal',
    'The 600m descent into the caldera with sweeping views of the 20km-wide crater floor, Lake Magadi''s flamingo carpet turning the shoreline pink, the Lerai Forest framed by the towering caldera walls',
    'The Ngorongoro Crater is a UNESCO World Heritage Site. Vehicle numbers on the crater floor are controlled — arriving early on Day 5 avoids the midday crowds.',
    'Manyara', 'Ngorongoro', 60, 1, 'B,L,D', @now, @now);
SET @day_38_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_4, @park_ngorongoro, 'DAY_TRIP', 1, '13:00', '17:30', 'Afternoon crater descent and game drive, overnight rim lodge', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 5, 'Day 5', 'Ngorongoro → Serengeti — Crossing into Endless Plains',
    'Leave the Ngorongoro highlands and cross into the Serengeti — Africa''s most iconic wilderness. The drive through the Ngorongoro Conservation Area, past Olduvai Gorge, and through the Naabi Hill Gate opens onto the spectacular Serengeti plains.',
    'Morning departure from the rim lodge. Optional brief stop at the Olduvai Gorge Museum (Cradle of Humankind — earliest human fossils discovered here). Drive through the Ngorongoro Conservation Area with Maasai herders sharing land with wildlife.',
    'Enter the Serengeti National Park through Naabi Hill Gate. Immediate afternoon game drive across the Serengeti''s southern and central plains — vast open grasslands with acacia kopjes. Search for cheetah using termite mounds as vantage points, and lion prides in the shade.',
    'Arrive at ultra-luxury tented camp. Private outdoor shower under the stars, butler service, gourmet dinner delivered to your private deck.',
    'Cheetah, lion, leopard, elephant, giraffe, buffalo, wildebeest, zebra, topi, eland, Thomson''s and Grant''s gazelle, bat-eared fox, African wild dog',
    'The Serengeti''s legendary endless golden plains, acacia woodland kopjes teeming with predators, the dramatic open sky above the Serengeti, the sheer scale of the ecosystem that hosts two million wildebeest',
    'The ultra-luxury camps in the Serengeti offer private conservancy access outside the national park — meaning night drives and walking safaris are available, which are prohibited inside the national park.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_38_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_5, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon arrival game drive, overnight ultra-luxury camp', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 6, 'Day 6', 'Full Day Serengeti — Balloon Safari & Big Cat Encounters',
    'An extraordinary day on the Serengeti with the option of a pre-dawn hot air balloon flight over the plains. From above, the scale of this ecosystem becomes fully apparent. On the ground, your private guide finds the drama of predator-prey interactions.',
    'Optional hot air balloon launch before dawn. Float silently over the Serengeti as the sun rises, spotting herds of wildebeest and zebra from above. Champagne breakfast in the bush upon landing. For non-balloon guests, a dawn game drive chases the early morning predator activity.',
    'Full morning and afternoon game drive circuits through the central Seronera Valley — the Serengeti''s predator heartland. Extended time watching cheetah hunts, lion cub play, and elephant families crossing the plains.',
    'Night drive (available from private conservancy) in search of leopard, aardvark, and nocturnal species. Gourmet dinner under the stars at camp.',
    'Lion, cheetah, leopard, spotted hyena, African wild dog, elephant, giraffe, buffalo, wildebeest (Great Migration herds seasonal), zebra, topi, impala, bat-eared fox, aardvark (night drive)',
    'The Serengeti from above in a hot air balloon at sunrise — one of the greatest wildlife spectacles on earth, Seronera Valley kopje formations hosting multiple predator species',
    'Hot air balloon flights require advance booking and are weather dependent. Night drives must be conducted from private conservancy land adjacent to the park.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_38_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_6, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day game drives including optional balloon safari, overnight ultra-luxury camp', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 7, 'Day 7', 'Serengeti Morning → Fly to Zanzibar — Savanna to Spice Island',
    'A final morning game drive on the endless Serengeti plains before a charter flight takes you to Zanzibar — the Spice Island jewel of the Indian Ocean. In a single day you move from the world''s greatest wildlife spectacle to paradise beaches.',
    'Dawn game drive from camp for a final encounter with the Serengeti''s wildlife. Return to camp for breakfast and check-out. Transfer to Seronera Airstrip for your private charter flight to Zanzibar. Enjoy aerial views of the Serengeti, Ngorongoro, and Kilimanjaro from above.',
    'Arrive Zanzibar airport in early afternoon. VIP transfer to your ultra-luxury beachfront resort. Private beach villa with direct Indian Ocean access. Afternoon at leisure — swim in the warm turquoise waters, book a couples'' beach massage, or explore the resort grounds.',
    'Private sunset cocktails on the beach. Tasting menu seafood dinner at the resort''s beachfront restaurant.',
    'Final Serengeti wildlife on morning drive; Zanzibar: sea turtles nesting seasonally, Indo-Pacific dolphins offshore, tropical reef fish in the crystal shallows, kingfishers and herons on the shore',
    'Aerial views of the Serengeti ecosystem stretching to the horizon, the Indian Ocean coastline approaching from the air, Zanzibar''s powder-white beaches and turquoise lagoons',
    'The charter flight routing may transit through Arusha or Dar es Salaam depending on scheduling. Checked luggage is weight-restricted on bush flights — soft bags only. Beach resort check-in is usually from 14:00.',
    'Serengeti', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_38_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_7, @park_serengeti, 'DAY_TRIP', 1, '06:00', '10:00', 'Final morning game drive before fly-out to Zanzibar', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 8, 'Day 8', 'Zanzibar — Jozani Forest & Prison Island Excursion',
    'Explore the wild and cultural heart of Zanzibar with visits to the Jozani Forest — home of the endemic red colobus monkey — and historic Prison Island with its famous Aldabra giant tortoises. The Indian Ocean provides a stunning backdrop throughout.',
    'Morning drive to Jozani Chwaka Bay National Park. Guided forest walk through the indigenous groundwater forest to encounter the endangered Zanzibar red colobus monkey at close range. Walk the raised mangrove boardwalk over tidal Chwaka Bay before returning to Stone Town.',
    'Board a traditional dhow for the short crossing to Prison Island (Changuu Island). Walk among the giant Aldabra tortoises — some over 100 years old. Snorkel the surrounding coral reef in the crystal-clear warm water. Return to your resort by late afternoon.',
    'Dinner at the resort''s finest restaurant — fresh Zanzibar lobster and prawns with local spice sauces.',
    'Zanzibar red colobus monkey (endemic, critically endangered), Aldabra giant tortoise (Prison Island), Sykes'' monkey, tropical reef fish including parrotfish, butterflyfish, and clownfish, hawksbill sea turtle (seasonal)',
    'Jozani''s ancient forest canopy, mangrove creek tidal reflections, the turquoise Indian Ocean surrounding Prison Island, coral reef gardens alive with colour',
    'Prison Island has historical significance as a former quarantine station and slave holding point. The tortoise population is one of the largest outside the Aldabra Atoll. Snorkel gear provided by resort.',
    'Zanzibar', 'Zanzibar', 80, 1, 'B,L,D', @now, @now);
SET @day_38_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_38_8, @park_jozani, 'DAY_TRIP', 1, '08:30', '12:00', 'Jozani Forest red colobus walk, then Prison Island dhow trip', @now);

-- Day 9
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 9, 'Day 9', 'Zanzibar — Stone Town, Spice Farms & Sunset Dhow Cruise',
    'Immerse yourself in Zanzibar''s extraordinary cultural tapestry — a UNESCO World Heritage city shaped by Arab, Persian, Indian, and African influences. Wander ancient alleys, visit fragrant spice plantations, and sail into the sunset on a traditional wooden dhow.',
    'Morning guided walking tour of Stone Town — the UNESCO-listed historic centre of Zanzibar City. Explore the House of Wonders, the Arab Fort, the Old Slave Market, and the labyrinthine alleys lined with intricately carved wooden doors. Visit the colourful Darajani Market.',
    'Drive to a traditional spice plantation in the central highlands. A knowledgeable guide reveals cloves, nutmeg, cinnamon, black pepper, cardamom, turmeric, and vanilla growing in their natural environment. Taste fresh tropical fruits and enjoy a traditional Zanzibari lunch at the farm.',
    'Return to Stone Town waterfront for the sunset dhow cruise. Sail on a traditional wooden ngalawa or dhow as the sun dips below the horizon, painting the Indian Ocean gold. Onboard snacks and drinks provided.',
    'Zanzibar is a cultural rather than wildlife destination today. Observe the vibrant urban bird life of Stone Town including pied crow, Indian myna, and sunbirds in the spice gardens',
    'Stone Town''s ancient carved-door architecture and Swahili-Arabic streetscapes, the fragrant spice plantation landscape, the Indian Ocean at golden hour from the wooden dhow deck',
    'Stone Town is best explored on foot with a knowledgeable local guide. The spice tour is interactive and suitable for all ages. Book the sunset dhow cruise through your resort concierge.',
    'Zanzibar', 'Zanzibar', 60, 1, 'B,L,D', @now, @now);
SET @day_38_9 = LAST_INSERT_ID();

-- Day 10
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_38, 10, 'Day 10', 'Zanzibar Departure — Last Taste of Paradise',
    'Savour your final morning in Zanzibar before transferring to the airport. Whether you spend the last hours on the beach, browsing Stone Town''s boutique spice shops, or simply watching the dhows sail past from the resort terrace — Tanzania will stay with you forever.',
    'Final breakfast at the resort. Last swim or beach walk. Optional visit to the Stone Town spice market for last-minute souvenirs — loose cloves, vanilla pods, and saffron make wonderful gifts. Check out and transfer to Zanzibar''s Abeid Amani Karume International Airport.',
    NULL,
    NULL,
    'Shore birds on the beach at low tide, Indian myna and sunbirds in resort gardens, possible dolphin sighting on the airport transfer road (dolphins frequent Kizimkazi Bay)',
    'The last lingering views of Zanzibar''s Indian Ocean coastline and its iconic wooden dhow silhouettes on the water',
    'Allow at least 3 hours before your international departure. Zanzibar airport can be congested during peak season. Confirm all excess baggage arrangements with your resort in advance.',
    'Zanzibar', 'Zanzibar Airport', 35, 0, 'B', @now, @now);
SET @day_38_10 = LAST_INSERT_ID();

-- ============================================================
-- ITINERARY 39: Northern Tanzania & Zanzibar Grand Tour (10 days, 9 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 1, 'Day 1', 'Arusha National Park — Canoeing, Colobus & Crater Views',
    'Begin your grand northern Tanzania tour with a short drive to Arusha National Park — a wonderfully diverse park in the shadow of Mount Meru. Canoe the Momella Lakes, walk among colobus monkeys, and gaze into the Ngurdoto Crater before your overnight in Arusha.',
    'Morning drive from Arusha to Arusha National Park (35km, 45 minutes). Morning game drive through the fig tree forest spotting black-and-white colobus monkeys, giraffes, and zebra. Visit the Ngurdoto Crater viewpoint — a miniature Ngorongoro filled with buffalo.',
    'Canoe excursion on the Momella Lakes — paddle among flamingos, pelicans, and waterfowl with Mount Meru reflected in the still water. Guided lakeside walk spotting waterbirds and hippo tracks. Picnic lunch at the lake shore.',
    'Return to Arusha by late afternoon. Check into your lodge. Welcome dinner and briefing on the nine days ahead.',
    'Black-and-white colobus monkey, giraffe, zebra, buffalo, bushbuck, waterbuck, dik-dik, hippo, flamingo, pelican, African fish eagle, augur buzzard, silvery-cheeked hornbill',
    'Mount Meru''s dramatic volcanic cone reflected in the Momella Lakes, the lush fig tree forest canopy, Ngurdoto Crater panorama with wildlife far below, the mountain moorland above the tree line',
    'Arusha National Park offers a fantastic introductory safari — easy terrain, diverse habitats, and great wildlife in a compact area. The canoeing experience on the Momella Lakes is unique in northern Tanzania.',
    'Arusha', 'Arusha', 35, 1, 'L,D', @now, @now);
SET @day_39_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_1, @park_arusha, 'DAY_TRIP', 1, '08:30', '16:30', 'Morning game drive, canoe excursion on Momella Lakes, return to Arusha for overnight', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 2, 'Day 2', 'Arusha → Tarangire — Baobab Giants & Elephant Herds',
    'Drive south-west from Arusha to Tarangire National Park for a full day in the land of ancient baobabs and the greatest elephant concentrations in northern Tanzania. The park''s Tarangire River is a lifeline that draws wildlife from across the Masai Steppe.',
    'Early morning departure from Arusha. Drive to Tarangire National Park (120km, approximately 2.5 hours). Enter Kuro Gate and begin morning game drive along the Tarangire River — elephant herds of 50 or more move to water as lions rest in the shade.',
    'Picnic lunch under a monumental baobab tree. Afternoon game drive through the park''s central circuits — the Boundary Hill area for excellent lion sightings and the Silale Swamp for massed waterbirds. Scan baobab branches for leopard and python.',
    'Arrive at your lodge inside or adjacent to the park. Dinner and overnight.',
    'African elephant (500+ animals in dry season), lion, leopard, giraffe, zebra, wildebeest, buffalo, fringe-eared oryx, lesser kudu, python, ground hornbill, 550+ bird species',
    'Ancient baobab tree landscapes that dwarf the safari vehicle, Tarangire River valley framed by golden grassland, termite mound castellations rising above the savanna floor',
    'Tarangire''s elephant herds are the most accessible in northern Tanzania. The park''s 2,850 km² size means you can find a quiet circuit even when visitor numbers are higher.',
    'Arusha', 'Tarangire', 120, 1, 'B,L,D', @now, @now);
SET @day_39_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_2, @park_tarangire, 'SLEEP_OVER', 1, '10:00', NULL, 'Full day game drive along Tarangire River, overnight in park', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 3, 'Day 3', 'Tarangire → Lake Manyara — Flamingo Shores & Tree Lions',
    'Move north to Lake Manyara National Park — a compact jewel of biodiversity set between the towering Great Rift Valley escarpment and the shimmering alkaline lake. The park packs extraordinary wildlife diversity into a surprisingly small area.',
    'Morning game drive exit from Tarangire. Drive north to Lake Manyara (70km). Enter through the dense groundwater forest — one of Tanzania''s best primates hotspots with baboons, blue monkeys, and bushbuck in the undergrowth.',
    'Afternoon game drive through the open floodplain and along the lake shore. Search for the famous tree-climbing lions resting in the yellow fever acacias above your vehicle. Visit the hot springs area and the hippo pool. Walk the lake shore for flamingo viewing.',
    'Drive to overnight lodge on the Manyara escarpment or in the adjacent Karatu area. Dinner and overnight.',
    'Tree-climbing lions, flamingo (thousands, seasonal), elephant, baboon, blue monkey, hippo, buffalo, giraffe, impala, pelican, marabou stork, African fish eagle, 400+ bird species',
    'The alkaline Lake Manyara stretching to the horizon, the Great Rift Valley escarpment rising dramatically above the park, lush groundwater forest contrasting with the open lake shore grasslands',
    'Lake Manyara is one of Tanzania''s most important birding sites with over 400 species recorded. The tree-climbing lion behaviour is thought to be a learned adaptation to escaping buffalo harassment.',
    'Tarangire', 'Manyara', 70, 1, 'B,L,D', @now, @now);
SET @day_39_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_3, @park_manyara, 'DAY_TRIP', 1, '10:00', '17:00', 'Full day game drive including tree-climbing lion search, overnight at escarpment lodge', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 4, 'Day 4', 'Manyara → Ngorongoro Crater — Africa''s Greatest Wildlife Arena',
    'Ascend to the Ngorongoro Crater rim and descend into the world''s largest intact volcanic caldera for an afternoon of Big Five game viewing. The sheer density of wildlife on the crater floor is unlike anywhere else in Africa.',
    'Morning departure from the Manyara area. Drive to the Ngorongoro Conservation Area (60km). Ascend the rim road through highland forest — elephant and buffalo often seen near the gate. Check in to your crater rim lodge with panoramic caldera views.',
    'Early afternoon descent into the Ngorongoro Crater. Afternoon game drive across the crater floor — search for the rare black rhino in the open grassland, observe lion prides resting under acacia trees, and watch flamingos wade along Lake Magadi.',
    'Ascend the crater in late afternoon. Dinner at the rim lodge overlooking the vast caldera.',
    'Black rhino, lion, spotted hyena, cheetah, African elephant, Cape buffalo, hippo, flamingo, wildebeest, zebra, crowned crane, jackal, serval',
    'The volcanic caldera walls rising 600m above the crater floor, Lake Magadi''s pink flamingo carpet, the crater floor grasslands teeming with wildlife, sunset light on the ancient caldera rim',
    'The descent road is steep and can be muddy in wet season — 4WD is essential. The crater floor covers 260 km² and is accessible only by 4WD vehicles with a licensed guide.',
    'Manyara', 'Ngorongoro', 60, 1, 'B,L,D', @now, @now);
SET @day_39_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_4, @park_ngorongoro, 'DAY_TRIP', 1, '12:30', '17:00', 'Afternoon crater descent game drive, overnight rim lodge', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 5, 'Day 5', 'Ngorongoro → Serengeti — Plains of Endless Life',
    'Cross from the Ngorongoro highlands into the Serengeti National Park — Africa''s most celebrated wilderness. The transition from highland crater to open savanna plains is one of the most dramatic landscape changes in all of Africa.',
    'Morning departure from the rim lodge. Drive west through the Ngorongoro Conservation Area. Optional stop at Olduvai Gorge Museum — where fossils of early hominids established East Africa as the cradle of humankind. Cross the Naabi Hill boundary into the Serengeti.',
    'Afternoon game drive through the Serengeti''s southern short-grass plains and into the central Seronera Valley. The predator density here is extraordinary — lion, cheetah, and leopard sharing the same stretch of acacia-dotted savanna. Arrive at your lodge or tented camp.',
    'Dinner at camp as the sounds of the Serengeti night fill the air — hyena whooping, lion rumbling, and the chorus of thousands of insects.',
    'Cheetah, lion, leopard, spotted hyena, African wild dog, elephant, giraffe, buffalo, wildebeest, zebra, topi, eland, Thomson''s gazelle, bat-eared fox',
    'The Serengeti''s legendary boundless plains stretching in every direction, kopje rock outcrops housing predators, vast herds of wildebeest and zebra on the migration circuit',
    'The Serengeti ecosystem covers 30,000 km² including the Maasai Mara across the Kenyan border. Your guide will advise on the best circuits depending on the season and migration position.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_39_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_5, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon arrival game drive, overnight in park', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 6, 'Day 6', 'Full Day Serengeti — The Predator Capital of Africa',
    'Spend an entire day in the Serengeti''s wildlife heartland. From the Seronera Valley to the vast plains, today is dedicated to following the action — from dawn predator activity to midday elephant families to the golden hour big cat encounters.',
    'Pre-dawn game drive at first light — the best time for predator activity. Lions return from night hunts, leopards are still visible in acacia trees, and cheetahs ascend termite mounds to survey the plains. Dawn light on the Serengeti plains is extraordinary.',
    'Morning and afternoon game drives with a packed lunch eaten beside a seasonal river. Explore the kopje rock formations — each one a microhabitat hosting mongoose, rock hyrax, agama lizards, and often a lion or leopard. Visit the Seronera River for crocodile and hippo.',
    'Sundowner at a scenic kopje hilltop. Night at your Serengeti camp or lodge.',
    'Lion, cheetah, leopard, spotted hyena, African wild dog, elephant, giraffe, Nile crocodile, hippo, buffalo, topi, wildebeest, zebra, eland, Thomson''s gazelle, bat-eared fox, mongoose',
    'The Seronera Valley''s acacia-lined river courses teeming with predators, the vast open plains with herds stretching to the horizon, kopje formations alive with wildlife against the golden savanna',
    'The Seronera Valley has the highest density of resident predators in the Serengeti ecosystem. Leopards are frequently seen in the sycamore figs along the Seronera River.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_39_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_6, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day game drives, dawn to dusk in the park, overnight', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 7, 'Day 7', 'Serengeti → Fly to Zanzibar — Plains to Ocean',
    'A final morning game drive on the Serengeti''s golden plains before a charter flight carries you to Zanzibar — the jewel of the Indian Ocean. By afternoon you are walking barefoot on white sand with the warm ocean lapping at your feet.',
    'Early morning game drive at dawn — a final immersion in the Serengeti''s wildlife drama. Return to camp for breakfast and check-out. Transfer by safari vehicle to Seronera Airstrip for your charter flight. Enjoy aerial views of the Serengeti and Great Rift Valley.',
    'Arrive Zanzibar airport in early afternoon. Transfer to your beachfront hotel or boutique resort. Afternoon at leisure on the beach — swim in the warm turquoise Indian Ocean, enjoy fresh coconut water under a palm tree, or book a sunset snorkelling excursion.',
    'Sunset cocktails on the beach. Fresh seafood dinner at the resort — Zanzibar lobster, tiger prawns, and octopus cooked in local spices.',
    'Final Serengeti wildlife on morning drive; Zanzibar beach and reef: colourful reef fish, sea turtles (seasonal), Indo-Pacific dolphins (Kizimkazi Bay), shore birds',
    'Last panoramic views of the Serengeti plains from the air, the Indian Ocean coastline and coral reefs visible from the flight, Zanzibar''s white beaches and turquoise lagoons',
    'Charter flight schedules vary — your operator will confirm timing. Soft bags only on bush flights due to weight and size limits. Beach resort essentials should be in hand luggage.',
    'Serengeti', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_39_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_7, @park_serengeti, 'DAY_TRIP', 1, '06:00', '10:00', 'Final morning game drive before charter flight to Zanzibar', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 8, 'Day 8', 'Zanzibar — Jozani Forest & Pristine Beaches',
    'Discover the natural heart of Zanzibar — the ancient Jozani Forest with its endemic red colobus monkeys and rich mangrove ecosystem. Then spend the afternoon at some of the Indian Ocean''s most beautiful beaches.',
    'Morning drive to Jozani Chwaka Bay National Park (45 minutes from Stone Town). Guided forest walk through the indigenous groundwater forest — encounter the critically endangered Zanzibar red colobus monkey at close range. Walk the mangrove boardwalk over Chwaka Bay tidal flats.',
    'Drive to the east coast beaches — Paje or Bwejuu for kite surfing (seasonal), or Matemwe for snorkelling. Afternoon on the beach — swim, snorkel on the fringing reef, or simply relax under the palm trees with a cold Zanzibar Gin and Tonic.',
    'Dinner at a beach restaurant — fresh-caught fish cooked over charcoal, Zanzibari-style. Overnight at your beach resort.',
    'Zanzibar red colobus monkey (endemic, one of Africa''s rarest primates), Sykes'' monkey, Ader''s duiker, mangrove kingfisher, various reef fish while snorkelling, hawksbill sea turtle (seasonal in nesting areas)',
    'Jozani''s dense forest canopy, mangrove boardwalk over tidal waters, the east coast''s powder-white beaches and turquoise Indian Ocean at low tide, coral reef gardens visible through the clear water',
    'The east coast beaches are best at low tide for beach walking and at high tide for swimming and snorkelling. The marine park fee is required for snorkelling near the reef. Book through your resort.',
    'Zanzibar', 'Zanzibar', 90, 1, 'B,L,D', @now, @now);
SET @day_39_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_39_8, @park_jozani, 'DAY_TRIP', 1, '08:30', '12:00', 'Jozani Forest walk and mangrove boardwalk, afternoon beach', @now);

-- Day 9
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 9, 'Day 9', 'Zanzibar — Stone Town, Spice Tour & Sunset Dhow Cruise',
    'Explore the soul of Zanzibar — the UNESCO World Heritage-listed Stone Town with its Arabic-Swahili architecture, the fragrant spice farms of the central highlands, and a romantic sunset dhow sailing on the Indian Ocean.',
    'Morning guided walking tour of Stone Town. Navigate the labyrinthine alleys past intricately carved wooden doorways, the 19th-century House of Wonders, the Arab Fort, the Old Slave Market memorial, and the colourful Darajani morning market teeming with fresh produce, spices, and fish.',
    'Drive to a traditional spice farm in Zanzibar''s interior highlands. Guided plantation walk — identify and taste cloves, nutmeg, cinnamon, black pepper, lemongrass, turmeric, and vanilla in their natural form. Traditional Zanzibari lunch served at the farm.',
    'Return to Stone Town for the sunset dhow cruise on the Indian Ocean. Board a traditional wooden dhow at the Zanzibar Serena waterfront for a 90-minute sail. Snacks and sundowner drinks provided as the sky turns orange over the sea.',
    'Tropical birds in Stone Town gardens: Indian myna, pied crow, sunbirds; spice garden butterflies; shore birds at low tide on the waterfront',
    'Stone Town''s ancient carved-door streetscapes and Swahili-Arabic rooftop silhouettes against the sky, the lush spice plantation canopy, the Indian Ocean at golden hour from the dhow deck',
    'Stone Town''s narrow alleys are best explored on foot with a local guide. The spice tour is highly interactive and especially popular with families. The sunset dhow cruise should be booked the day before.',
    'Zanzibar', 'Zanzibar', 60, 1, 'B,L,D', @now, @now);
SET @day_39_9 = LAST_INSERT_ID();

-- Day 10
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_39, 10, 'Day 10', 'Zanzibar Departure — Final Moments in Paradise',
    'Your grand northern Tanzania tour draws to a close with a final morning in Zanzibar. Collect your spice souvenirs, take one last swim, and depart with the taste of cloves and the sound of the Indian Ocean in your memory.',
    'Leisurely final breakfast at the resort. Last beach walk or swim in the Indian Ocean. Visit the Stone Town spice market for souvenirs — vanilla pods, clove oil, and Zanzibar saffron are excellent gifts. Check out and transfer to the airport.',
    NULL,
    NULL,
    'Shore birds on the beach at low tide, Indian Ocean reef fish visible from the beach at high tide, possibility of dolphins near Kizimkazi Bay on the airport road',
    'Final Indian Ocean views, dhow silhouettes on the horizon, Zanzibar''s iconic palm-fringed shoreline from the airport departure area',
    'Allow at least 3 hours before international departure. Zanzibar International Airport is compact but can be busy. Pre-book excess baggage allowance for any spice or souvenir purchases.',
    'Zanzibar', 'Zanzibar Airport', 35, 0, 'B', @now, @now);
SET @day_39_10 = LAST_INSERT_ID();

-- ============================================================
-- ITINERARY 40: Family Safari & Zanzibar Beach Holiday (9 days, 8 nights)
-- ============================================================
-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 1, 'Day 1', 'Arusha → Tarangire — Family Safari Begins',
    'Your family safari adventure kicks off with a drive to Tarangire National Park — one of northern Tanzania''s most family-friendly safari destinations. Massive elephant herds, towering baobab trees, and a wide variety of wildlife make for an unforgettable first afternoon in the bush.',
    'Mid-morning departure from Arusha after family breakfast. Drive to Tarangire National Park (120km, approximately 2.5 hours). Enter Kuro Gate and immediately begin your family game drive — children delight in spotting elephant herds up close as they bathe and play in the Tarangire River.',
    'Picnic lunch under a giant baobab tree — a natural family photo backdrop. Afternoon game drive through the central circuits — spot giraffe, zebra, wildebeest, and buffalo together on the open plains. Watch ground hornbills stalking through the grass.',
    'Arrive at your family-friendly lodge. Children''s welcome activities and early family dinner. Evening bush walk briefing for tomorrow.',
    'African elephant (large family herds ideal for children), giraffe, zebra, wildebeest, buffalo, lion, impala, ground hornbill, warthog, mongoose, 550+ bird species',
    'Ancient baobab trees — each one a natural climbing frame for the imagination. Tarangire River valley, termite castle formations ideal for explaining African ecology to children',
    'Tarangire is considered one of the best parks for families — elephant encounters are frequent and close, the landscape is dramatic, and driving distances within the park are manageable for young passengers.',
    'Arusha', 'Tarangire', 120, 1, 'L,D', @now, @now);
SET @day_40_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_1, @park_tarangire, 'SLEEP_OVER', 1, '11:00', NULL, 'Family arrival game drive, overnight family lodge in park', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 2, 'Day 2', 'Full Day Tarangire — Elephant Education & Bush Discovery',
    'A full day in Tarangire dedicated to exploring the park in depth and learning about elephant behaviour, ecology, and conservation. Children engage with nature through interactive activities designed to ignite a lifelong passion for wildlife.',
    'Early morning game drive at sunrise with a junior ranger activity pack — children track animals, identify dung, and observe elephant family dynamics with guided explanation. Visit the Silale Swamp area for large bird concentrations and excellent elephant photography.',
    'Picnic lunch in the bush. Afternoon game drive through the baobab-rich southern circuits. Optional guided bush walk with an armed ranger (for children aged 10+) — learn to identify tracks, dung, and plants of the African savanna.',
    'Return to lodge for children''s bush dinner programme. Family storytelling around the campfire with a Maasai guide sharing traditional wildlife wisdom.',
    'African elephant (observe family hierarchy, matriarch behaviour, baby elephants), lion, leopard, giraffe, zebra, buffalo, impala, warthog, banded mongoose, ground hornbill, lilac-breasted roller',
    'Baobab trees some estimated at 2,000+ years old, the Silale Swamp alive with waterbirds, the Tarangire River snaking through the golden valley at golden hour',
    'The family bush walk requires closed-toe shoes and appropriate sun protection. Junior ranger activity packs are available from family-friendly lodges in the park.',
    'Tarangire', 'Tarangire', 0, 1, 'B,L,D', @now, @now);
SET @day_40_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_2, @park_tarangire, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day family safari including junior ranger programme, overnight in park', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 3, 'Day 3', 'Tarangire → Maasai Village → Ngorongoro Rim',
    'Drive north from Tarangire and stop for an authentic Maasai cultural village visit — a fascinating experience for children and adults alike. Continue to the Ngorongoro Conservation Area and settle into your crater rim lodge as the sun sets over Africa.',
    'Morning game drive exit from Tarangire. Drive north through the Rift Valley toward Karatu. Stop at a genuine Maasai boma — a traditional family compound — for a guided cultural experience. Children learn about Maasai warrior traditions, beadwork, and livestock herding.',
    'Picnic lunch en route. Continue driving through the Ngorongoro Conservation Area gate and up the winding rim road through highland forest. Buffalo and elephant are often spotted near the rim road.',
    'Arrive at crater rim lodge as the sun dips. Sundowner drinks on the lodge terrace with the vast Ngorongoro Crater spread below. Family dinner at the lodge.',
    'Maasai cattle, donkeys, and goats at the village; wildlife along the rim road: elephant, buffalo, hyena; crater rim birds: crowned eagle, mountain buzzard',
    'The Ngorongoro Crater rim at sunset — one of Africa''s most dramatic viewpoints, the winding Highland road through Ngorongoro''s dense forest, the Maasai boma against the backdrop of the open savanna',
    'The Maasai cultural visit should be arranged through your operator with a reputable community that benefits directly from tourism. This experience is authentically educational and respectful.',
    'Tarangire', 'Ngorongoro', 130, 1, 'B,L,D', @now, @now);
SET @day_40_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_3, @park_ngorongoro, 'SLEEP_OVER', 1, '15:00', NULL, 'Afternoon rim arrival via Maasai cultural visit, overnight crater rim lodge', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 4, 'Day 4', 'Ngorongoro Crater — Family Big Five Day',
    'Descend into the Ngorongoro Crater for a full family game drive in search of the Big Five. The crater''s extraordinary wildlife density means children are almost certain to see lion, elephant, buffalo, and hippo — and with luck, the rare black rhino.',
    'Early morning descent into the Ngorongoro Crater — children press their faces to the windows as the crater floor reveals itself 600 metres below the rim. Morning game drive in the Lerai Forest and open grasslands searching for lion prides and elephant families.',
    'Family picnic lunch at the Ngoitokitok Springs hippo pool — children count the hippos wallowing and listen for the thunderous yawns. Afternoon game drive along Lake Magadi for flamingo and wading bird spectacles. Search for the crater''s rare black rhino.',
    'Ascend the crater rim by late afternoon. Family dinner at the lodge — children compare Big Five lists from the day.',
    'Black rhino, lion, African elephant, Cape buffalo, hippo, flamingo, spotted hyena, wildebeest, zebra, Thomson''s gazelle, jackal, crowned crane, secretary bird',
    'The Ngorongoro Crater floor — the world''s greatest natural wildlife arena, Lake Magadi''s flamingo carpet stretching pink in both directions, the dramatic 600m caldera walls, the Lerai Forest in golden morning light',
    'The crater is excellent for family safaris — the enclosed nature of the caldera means wildlife is concentrated and easy to find. Children benefit from the explainable geology of the ancient volcano.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_40_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_4, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '17:00', 'Full day family Big Five crater game drive, overnight rim lodge', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 5, 'Day 5', 'Ngorongoro → Serengeti — Into the Great Migration',
    'Cross into the Serengeti National Park and join the greatest wildlife spectacle on earth. For children, the sight of thousands of wildebeest and zebra stretching across the plains is a defining wildlife experience that stays with them for life.',
    'Morning departure from the rim lodge. Drive through the Ngorongoro Conservation Area with Maasai herders sharing the landscape with giraffe and zebra. Enter the Serengeti through Naabi Hill Gate — where the plains suddenly open to infinity.',
    'Afternoon game drive across the Serengeti''s central circuits. A family guide explains the ecology of the Great Migration, how predators and prey interact, and why the Serengeti is Africa''s greatest wildlife arena. Spot cheetah, lion, and elephant on the open plains.',
    'Arrive at your family camp or lodge. Children''s early dinner programme followed by a family stargazing session — the Serengeti''s sky is amongst the darkest and most spectacular in Africa.',
    'Lion (prides with cubs), cheetah (often with cubs in open plains), elephant, giraffe, wildebeest (Great Migration herds seasonal), zebra, buffalo, topi, eland, bat-eared fox',
    'The endless Serengeti plains stretching to the horizon — a landscape that humbles even the most seasoned traveller, the kopje rock formations, vast herds of wildebeest and zebra moving across the grass',
    'Family camps in the Serengeti offer dedicated children''s programmes, junior ranger activities, and child-safe facilities. The Great Migration timing varies by season — your guide will advise on the best area to find the herds.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_40_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_5, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon arrival game drive into Serengeti, overnight family camp', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 6, 'Day 6', 'Full Day Serengeti — Family Bush Walk & Big Cat Safari',
    'A full day in the Serengeti with a family-appropriate guided bush walk on the open plains. Walk with Maasai guides through the heart of the ecosystem, learning to track wildlife, identify plants, and understand the web of life that makes the Serengeti extraordinary.',
    'Early morning family game drive at dawn — the best time to catch lions and cheetahs active on the plains. Children use binoculars to spot distant herds and predators. The guide explains predator territory and hunting behaviour using real-time observations.',
    'After brunch at camp, embark on a guided family bush walk on the open Serengeti plains (armed ranger escort). Children learn to identify tracks, dung, and plants. Visit a termite mound and learn about the ecological role of these remarkable architects.',
    'Afternoon game drive through the central plains and Seronera Valley. Sundowner on a kopje rock with panoramic views. Family campfire dinner with traditional Swahili songs.',
    'Lion (close encounters during bush walk with guide), cheetah, leopard, elephant, giraffe, buffalo, wildebeest, zebra, topi, Thomson''s gazelle, bat-eared fox, banded mongoose, dung beetle',
    'The open Serengeti plains at eye level during the bush walk — experiencing the scale of the ecosystem on foot, kopje hilltop panoramas of the endless savanna, the Seronera River with crocodiles and hippos',
    'The family bush walk is conducted by experienced armed guides and is suitable for children aged 8 and above. No running, loud voices, or sudden movements — children should be briefed beforehand.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_40_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_6, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day family safari including guided bush walk on plains, overnight family camp', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 7, 'Day 7', 'Serengeti → Fly to Zanzibar — From Safari to Snorkelling',
    'A final sunrise game drive on the Serengeti plains before flying to Zanzibar. Children transition from watching elephants on the savanna to snorkelling with tropical fish in the Indian Ocean — two of Africa''s greatest wildlife experiences in a single day.',
    'Early morning family game drive at dawn — one last chance for lion and cheetah encounters before the family''s safari chapter closes. Return to camp for breakfast and check-out. Transfer to Seronera Airstrip for the charter flight to Zanzibar.',
    'Arrive Zanzibar airport in early afternoon. Transfer to your family beach resort. Children hit the beach immediately — snorkelling on the fringing reef, building sandcastles, and swimming in the shallow turquoise lagoon.',
    'Family beach barbecue dinner. Children fascinated by the stars above the Indian Ocean — a different sky from the mainland.',
    'Final Serengeti wildlife on morning drive; Zanzibar: tropical reef fish (parrotfish, angelfish, clownfish, surgeonfish), sea turtles (seasonal), dolphins offshore, shore crabs and shells on the beach',
    'Aerial views of the Serengeti ecosystem from the charter flight, the Indian Ocean coastline glittering below, Zanzibar''s white beaches and turquoise lagoon visible from the air',
    'The charter flight to Zanzibar via Arusha or Dar es Salaam takes 2–3 hours total. Children must sit upright and wear seatbelts at all times during bush flights. Soft bags only.',
    'Serengeti', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_40_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_7, @park_serengeti, 'DAY_TRIP', 1, '06:00', '10:00', 'Final family morning game drive before charter flight to Zanzibar', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 8, 'Day 8', 'Zanzibar — Jozani Monkeys, Spice Farm & Beach',
    'A wonderful family day combining the endemic red colobus monkeys of Jozani Forest with a hands-on spice plantation tour and an afternoon of beach fun. Children love meeting the monkeys and tasting exotic spices straight from the plant.',
    'Morning drive to Jozani Chwaka Bay National Park. Guided family forest walk through the indigenous groundwater forest — the Zanzibar red colobus monkeys are habituated and very approachable, delighting children of all ages. Walk the mangrove boardwalk over the tidal creek.',
    'Drive to a family-friendly spice plantation in the central highlands. Children identify and taste spices — cloves, cinnamon, vanilla, nutmeg, and tropical fruits. The guide creates animal shapes from palm leaves as gifts for children. Traditional lunch at the farm.',
    'Afternoon return to the beach for family snorkelling, swimming, and beach games. Children''s beach dinner followed by an evening stargazing session with the resort naturalist.',
    'Zanzibar red colobus monkey (highly approachable endemic primate), Sykes'' monkey, bush pig, mangrove kingfisher, 50+ butterfly species; beach: tropical reef fish, hermit crabs, starfish in rock pools',
    'Jozani''s ancient forest with monkeys swinging through the canopy, mangrove creek reflecting the sky, the lush spice plantation alive with colour and fragrance, the beach at low tide with exposed rock pools teeming with life',
    'The Jozani red colobus monkeys are among the most photographed and child-friendly wildlife encounters in East Africa. Remind children not to touch or feed the monkeys. The spice tour is interactive and educational for all ages.',
    'Zanzibar', 'Zanzibar', 80, 1, 'B,L,D', @now, @now);
SET @day_40_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_40_8, @park_jozani, 'DAY_TRIP', 1, '08:30', '12:00', 'Family Jozani Forest monkey walk and mangrove boardwalk, spice farm, beach afternoon', @now);

-- Day 9
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_40, 9, 'Day 9', 'Zanzibar Beach Morning & Family Departure',
    'Savour your final morning on Zanzibar''s iconic beaches before the family heads to the airport. Last swim, last fresh coconut, last footprints in the white sand — an African beach holiday that will be talked about for years to come.',
    'Final family breakfast at the resort. Children''s last swim and snorkel in the lagoon. Pack up final souvenirs — vanilla pods, cloves, and hand-crafted beaded jewellery for grandparents. Check out and transfer to Zanzibar''s international airport.',
    NULL,
    NULL,
    'Shore birds on the beach, tropical fish in the shallows, hermit crabs on the sand — wildlife accessible right from the beach at all ages',
    'Last views of Zanzibar''s white beaches and Indian Ocean horizon, dhow silhouettes on the turquoise water, Mount Kilimanjaro sometimes visible from the airport on exceptionally clear mornings',
    'Allow 3 hours before international departure from Zanzibar. Children travelling with hand luggage from both the safari and beach portions should check weight allowances in advance. Keep spices in checked bags.',
    'Zanzibar', 'Zanzibar Airport', 35, 0, 'B', @now, @now);
SET @day_40_9 = LAST_INSERT_ID();
-- ============================================================
-- ITINERARY 41: Kilimanjaro Summit & Northern Circuit Safari (12 days, 11 nights)
-- ============================================================

-- Day 1: Arusha → Lemosho Gate → Big Tree Camp
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 1, 'Day 1', 'Arusha to Big Tree Camp — Rainforest Entry',
    'Transfer from Arusha to Lemosho Gate and begin the Lemosho Route trek through ancient montane rainforest to Big Tree Camp at 2,780m. This is the most scenic and least-crowded route on Kilimanjaro, offering wide vistas and excellent acclimatization gradients.',
    'Early departure from Arusha and drive to Lemosho Gate (95km, approx 2.5 hours). Complete park registration, meet your mountain crew, and begin trekking through lush montane rainforest teeming with colobus monkeys and hornbills.',
    'Trek through towering ferns and moss-draped trees. Arrive at Big Tree Camp (2,780m) by mid-afternoon — the camp sits beneath enormous Podocarpus trees.',
    'Settle into camp. Hot dinner prepared by the mountain crew. Acclimatization briefing from your guide.',
    'Black-and-white colobus monkey, blue monkey, bushbuck, duiker, Hartlaub''s turaco, silvery-cheeked hornbill, sunbird species',
    'Ancient Podocarpus rainforest canopy, giant tree ferns, moss-covered boulders, streams cutting through dense undergrowth',
    'Trek distance: approx 7km, 3-4 hours. Altitude gain: 500m. Pace is gentle on Day 1 — resist the urge to rush.',
    'Arusha', 'Kilimanjaro', 95, 1, 'L,D', @now, @now);
SET @day_41_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_1, @park_kilimanjaro, 'SLEEP_OVER', 1, '10:00', NULL, 'Lemosho Gate trek to Big Tree Camp, overnight at 2,780m', @now);

-- Day 2: Big Tree Camp → Shira 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 2, 'Day 2', 'Big Tree Camp to Shira 2 — Moorland Traverse',
    'Ascend from the rainforest through heather and moorland zones to Shira 2 Camp at 3,840m on the vast Shira Plateau. The landscape opens dramatically as you leave the forest behind, revealing sweeping views of the plateau and Kibo peak.',
    'Breakfast at Big Tree Camp. Begin climbing through the upper forest fringe into the heather zone. Spot eland and white-necked ravens as the vegetation transitions to giant heather and everlastings.',
    'Cross the Shira Plateau moorland, passing Shira 1 campsite and continuing east to Shira 2 (3,840m). Dramatic views of Kibo summit cone unfold to the east.',
    'Dinner at Shira 2 with sweeping plateau views. Begin to feel the effects of altitude — hydrate well.',
    'Eland, white-necked raven, alpine chat, augur buzzard, four-striped grass mouse',
    'Shira Plateau panoramas, Kibo summit cone emerging above the plateau, giant heather forests, distant Meru volcano views',
    'Trek distance: approx 14km, 6-8 hours. Altitude gain: 1,060m. Drink at least 3 litres of water today.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_41_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_2, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Big Tree Camp to Shira 2 plateau trek, overnight at 3,840m', @now);

-- Day 3: Shira 2 → Lava Tower → Barranco Camp
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 3, 'Day 3', 'Shira 2 to Barranco via Lava Tower — High Altitude Acclimatization',
    'The critical acclimatization day: climb high to the iconic Lava Tower (4,630m) before descending to Barranco Camp (3,950m). This "climb high, sleep low" profile dramatically improves summit success rates.',
    'Breakfast at Shira 2. Trek east across the plateau, passing the Shira Cathedral rock formation. Ascend gradually to the Lava Tower (4,630m) — a massive volcanic plug jutting from the alpine desert.',
    'Lunch at the Lava Tower with views across the southern ice fields. Descend steeply into the lush Barranco Valley — a dramatic contrast of giant groundsels and lobelias at 3,950m.',
    'Dinner at Barranco Camp in the shelter of the valley. The towering Barranco Wall looms overhead.',
    'Alpine swift, lammergeier, rock hyrax, white-necked raven, alpine chat',
    'Lava Tower volcanic plug, Shira Cathedral, giant groundsel forests of the Barranco Valley, southern glacier views from the tower',
    'Trek distance: approx 14km, 7-8 hours. Net altitude gain is minimal but the day peaks at 4,630m — a vital acclimatization push.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_41_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_3, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Shira 2 to Barranco via Lava Tower acclimatization day, overnight at 3,950m', @now);

-- Day 4: Barranco Wall → Karanga Camp
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 4, 'Day 4', 'Barranco Wall Scramble to Karanga Camp',
    'Scale the spectacular Barranco Wall — a hands-and-feet scramble up a dramatic cliff face — then traverse to Karanga Camp (3,995m). The wall is one of the most exhilarating sections of the entire Lemosho Route.',
    'Early breakfast and begin the Barranco Wall climb immediately after camp. The 300m scramble requires using hands and feet on solid rock ledges — guides assist at technical sections.',
    'Top of the wall reveals the full sweep of Kilimanjaro''s southern glaciers. Trek across successive ridges and ravines to Karanga Camp (3,995m), crossing the Karanga Valley stream.',
    'Rest and acclimatization at Karanga. Hot dinner and early sleep in preparation for tomorrow''s push to base camp.',
    'Lammergeier, alpine swift, rock hyrax at wall base',
    'Barranco Wall vertical perspective, southern ice field panorama from the top, Karanga Valley and its seasonal stream, glaciers gleaming above camp',
    'Trek distance: approx 9km, 5-6 hours. The wall scramble is Grade 2 — no technical climbing equipment needed but a head for heights is essential.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_41_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_4, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Barranco Wall scramble to Karanga Camp, overnight at 3,995m', @now);

-- Day 5: Karanga → Barafu Base Camp
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 5, 'Day 5', 'Karanga to Barafu Base Camp — The Final Preparation',
    'A shorter but psychologically significant day as you trek to Barafu Base Camp (4,673m) — the staging post for the summit push. The alpine desert here is stark and otherworldly, with little vegetation and views extending to the distant plains.',
    'Breakfast at Karanga. Trek north up a broad ridge into the high alpine desert zone. The landscape becomes increasingly barren as you gain altitude — no more plants, only rock and ice above.',
    'Arrive at Barafu (4,673m) by early afternoon. Set up tent and rest. Mandatory pre-summit briefing with your guide.',
    'Early dinner at 5:00 PM, then sleep — summit attempt begins at midnight.',
    'Virtually none at this altitude — occasional raven and alpine swift',
    'Rebmann and Ratzel glaciers looming above, Mawenzi peak to the east, the vast Tanzanian plains far below stretching to the horizon',
    'Rest completely this afternoon — no walks, no exertion. Sleep as much as possible. The summit bid starts at 11:00 PM to midnight.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_41_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_5, @park_kilimanjaro, 'SLEEP_OVER', 1, NULL, NULL, 'Karanga to Barafu Base Camp, rest before summit attempt, overnight at 4,673m', @now);

-- Day 6: Summit Uhuru Peak → Millennium Camp
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 6, 'Day 6', 'Summit Day — Uhuru Peak 5,895m & Descent to Millennium Camp',
    'The ultimate achievement: stand on the Roof of Africa at Uhuru Peak (5,895m) at sunrise, then descend all the way to Millennium Camp (3,820m) for a well-earned rest. This is the longest and most demanding day of the entire trek.',
    'Midnight departure from Barafu in head-torches. Ascend the volcanic scree in slow, steady pace through Stella Point (5,739m) — the crater rim — to Uhuru Peak at sunrise. Spend time at the summit sign and the ancient glaciers.',
    'Begin the long descent from Uhuru through Barafu, stopping briefly for lunch. Continue down to Millennium Camp (3,820m) through moorland.',
    'Hot dinner and immediate sleep at Millennium Camp — exhaustion is universal after summit day.',
    'Alpine swift, lammergeier on descent',
    'Uhuru Peak glacier panorama at sunrise, entire crater rim of Kibo, the Rebmann, Furtwängler, and Northern Ice Field glaciers, cloud layer far below on descent',
    'Summit night temperatures can reach -20°C with wind chill — full cold-weather layers, neoprene gloves, and balaclava are essential. Altitude sickness at summit level is common; descend immediately if symptoms worsen.',
    'Kilimanjaro', 'Kilimanjaro', 0, 1, 'B,L,D', @now, @now);
SET @day_41_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_6, @park_kilimanjaro, 'SLEEP_OVER', 1, '00:00', NULL, 'Midnight summit attempt, Uhuru Peak 5,895m, descent to Millennium Camp 3,820m', @now);

-- Day 7: Millennium Camp → Mweka Gate → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 7, 'Day 7', 'Descent to Mweka Gate & Return to Arusha',
    'Descend through the rainforest to Mweka Gate — the final day on the mountain. Receive your summit certificate, tip your crew, and transfer back to Arusha for a celebratory dinner and overnight rest before the safari begins.',
    'Breakfast at Millennium Camp. Begin the final descent through the moorland and back into the montane rainforest. The returning greenery and warmth are deeply restorative after days at altitude.',
    'Arrive at Mweka Gate. Summit certificates presented by park officials. Tip ceremony with the mountain crew.',
    'Drive back to Arusha (approx 1.5 hours). Check in to hotel, shower, and celebratory dinner.',
    'Blue monkey, colobus monkey, hornbills on descent through the forest',
    'Rainforest canopy re-entry after the alpine desert, views back up to Kibo summit, Mweka Village at the forest edge',
    'Trek distance: approx 10km, 4-5 hours to the gate. Sore knees are common — trekking poles are highly recommended for the descent.',
    'Kilimanjaro', 'Arusha', 0, 1, 'B,L,D', @now, @now);
SET @day_41_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_7, @park_kilimanjaro, 'DAY_TRIP', 1, '06:00', '14:00', 'Descent Millennium Camp to Mweka Gate, summit certificates, return to Arusha', @now);

-- Day 8: Rest Day → Tarangire National Park
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 8, 'Day 8', 'Rest & Recovery — Arusha to Tarangire',
    'A gentle morning of rest and recovery in Arusha after the Kilimanjaro summit, followed by an afternoon transfer to Tarangire National Park to begin the Northern Circuit safari. The transition from the world''s highest freestanding mountain to Tanzania''s elephant haven is remarkable.',
    'Leisurely breakfast at the hotel. Free morning for rest, shopping in Arusha town, or a massage. Legs will be grateful for flat ground.',
    'Depart Arusha after lunch (approx 120km drive). Enter Tarangire National Park for an afternoon game drive along the Tarangire River.',
    'Sundowner at the lodge. Dinner with stories of the mountain.',
    'African elephant (largest herds in northern Tanzania), lion, giraffe, zebra, wildebeest, fringe-eared oryx, lesser kudu, over 550 bird species',
    'Ancient baobab tree silhouettes against the evening sky, Tarangire River valley, termite mound savanna',
    'The contrast between Kilimanjaro''s arctic summit and Tarangire''s hot savanna is one of the great juxtapositions in African travel.',
    'Arusha', 'Tarangire', 120, 1, 'B,L,D', @now, @now);
SET @day_41_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_8, @park_tarangire, 'SLEEP_OVER', 1, '14:00', NULL, 'Afternoon arrival and game drive, overnight in Tarangire', @now);

-- Day 9: Tarangire → Ngorongoro
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 9, 'Day 9', 'Full Tarangire Morning & Drive to Ngorongoro',
    'A full morning game drive in Tarangire soaking up the baobab landscapes and elephant herds before the drive to Ngorongoro Conservation Area. Arrive at the crater rim for a spectacular evening.',
    'Dawn game drive along the Tarangire River. Watch elephant herds numbering in the hundreds congregating to drink and bathe. Search for tree-climbing lions and the shy lesser kudu in the dense riverine bush.',
    'Picnic lunch under a massive baobab. Drive west to Ngorongoro (approx 130km). Check in to crater rim lodge as the sun dips toward the caldera.',
    'Crater rim dinner with evening mist rolling into the caldera 600 metres below. Brief orientation for tomorrow''s descent.',
    'African elephant, lion, leopard, giraffe, zebra, buffalo, python, fringe-eared oryx, numerous raptor species',
    'Ancient baobab groves, Tarangire River floodplain, Great Rift Valley escarpment on the drive, Ngorongoro crater rim at sunset',
    'Ngorongoro rim lodges offer outstanding crater views — request a rim-facing room for sunrise.',
    'Tarangire', 'Ngorongoro', 130, 1, 'B,L,D', @now, @now);
SET @day_41_9 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_9, @park_ngorongoro, 'SLEEP_OVER', 1, '16:00', NULL, 'Afternoon arrival at crater rim, overnight at Ngorongoro', @now);

-- Day 10: Ngorongoro Crater Floor
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 10, 'Day 10', 'Ngorongoro Crater — Africa''s Eden',
    'A full day on the Ngorongoro Crater floor, the world''s largest intact volcanic caldera and home to the densest concentration of large mammals in Africa. The crater''s enclosed ecosystem virtually guarantees Big Five sightings.',
    'Dawn descent into the crater (600m down). Morning game drive through the Lerai Forest searching for elephant, lion, and the rare black rhino. Visit Lake Magadi for flamingo flocks and wading birds.',
    'Picnic lunch at the hippo pool. Afternoon game drive across the crater floor for cheetah, leopard, and the resident hyena clans. Extended time at big cat sightings.',
    'Ascend the crater rim and return to lodge. Dinner overlooking the caldera at dusk.',
    'Black rhino, lion, leopard, cheetah, elephant, buffalo, hippopotamus, flamingo, spotted hyena, wildebeest, zebra, eland, crowned crane, secretary bird',
    'The vast crater floor stretching to the caldera walls, Lake Magadi pink with flamingos, Lerai Forest green against the grasslands, panoramic rim views at ascent',
    'Ngorongoro has strict vehicle and descent hour regulations — follow your guide''s timing exactly. Black rhino sightings here are among the best in Tanzania.',
    'Ngorongoro', 'Ngorongoro', 0, 0, 'B,L,D', @now, @now);
SET @day_41_10 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_10, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '17:00', 'Full day crater floor game drive', @now);

-- Day 11: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 11, 'Day 11', 'Ngorongoro to Serengeti — The Endless Plains',
    'Drive from Ngorongoro through the dramatic Ngorongoro-Serengeti highlands and descend onto the endless Serengeti plains. Arrive in time for an afternoon game drive in the Seronera Valley — Tanzania''s premier game-viewing area.',
    'Breakfast on the crater rim with early morning views of the caldera. Pack up and drive west through the highlands, passing Olduvai Gorge — the cradle of mankind. Descend to the Serengeti plains.',
    'Afternoon game drive in the Seronera Valley. Seek out the resident lion prides and leopards resting in the sausage trees along the Seronera River.',
    'Sundowner on the plains. Bush dinner at your Serengeti camp with a star-filled sky above.',
    'Lion, leopard (Seronera sausage trees), cheetah, wildebeest herds, zebra, giraffe, hippo in Seronera River, crocodile',
    'Ngorongoro-Serengeti highland transition, Olduvai Gorge escarpment, the first views of the boundless Serengeti plains, Seronera River circuit at dusk',
    'The 145km drive from Ngorongoro to Seronera takes 3-4 hours. A stop at Olduvai Gorge museum adds 45 minutes and is highly recommended.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_41_11 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_11, @park_serengeti, 'SLEEP_OVER', 1, '14:00', NULL, 'Afternoon arrival and Seronera Valley game drive, overnight in Serengeti', @now);

-- Day 12: Serengeti Morning → Arusha
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_41, 12, 'Day 12', 'Final Serengeti Dawn Drive & Return to Arusha',
    'A final golden-hour game drive across the Serengeti plains at dawn, then the long drive back to Arusha to conclude this extraordinary 12-day Kilimanjaro summit and safari adventure.',
    'Pre-dawn departure for a final game drive as the Serengeti wakes up. Predators are active at first light — excellent chance for cheetah hunts and lion kills. Watch herds moving across the plains in the golden morning light.',
    'Return to camp for late breakfast. Begin the drive back to Arusha (approx 335km, 5-6 hours via Ngorongoro highlands).',
    NULL,
    'Cheetah, lion, hyena, wildebeest, zebra, giraffe, elephant on the morning drive',
    'Serengeti plains at golden hour, dawn light on acacia silhouettes, morning mist over the Seronera River',
    'Fly-out option from Seronera airstrip available to reduce drive time. The road journey back via Ngorongoro is scenic but long.',
    'Serengeti', 'Arusha', 335, 0, 'B,L', @now, @now);
SET @day_41_12 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_41_12, @park_serengeti, 'DAY_TRIP', 1, '05:30', '09:00', 'Dawn game drive before departure to Arusha', @now);

-- ============================================================
-- ITINERARY 42: Ultimate Honeymoon — Safari, Crater & Zanzibar (13 days, 12 nights)
-- ============================================================

-- Day 1: Arusha → Ngorongoro Rim
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 1, 'Day 1', 'Arrival & Drive to Ngorongoro — Rim Romance',
    'Fly into Arusha or Kilimanjaro airport and transfer to Ngorongoro Conservation Area for your first night on the crater rim. The journey through the Great Rift Valley sets the tone for a honeymoon of epic landscapes and intimate wilderness moments.',
    'Airport meet and greet. Drive from Arusha through the Rift Valley and Karatu highlands to Ngorongoro (190km, approx 3.5 hours). Check in to your crater rim lodge.',
    'Afternoon at leisure — relax on the terrace with crater views, enjoy a couples'' massage, or take a short walk along the rim with a guide.',
    'Candlelit dinner on the crater rim with the caldera spread out below. The twinkling lights of wildebeest fires visible in the distance.',
    'Game viewing begins tomorrow — enjoy the crater rim ambience this evening',
    'Panoramic crater rim views, the caldera 600m below, the Ngorongoro Conservation Area highlands, distant Ol Doinyo Lengai volcano',
    'The crater rim sits at 2,300m — bring a warm layer for evenings. Request a honeymoon-facing room with private balcony overlooking the caldera.',
    'Arusha', 'Ngorongoro', 190, 1, 'L,D', @now, @now);
SET @day_42_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_42_1, @park_ngorongoro, 'SLEEP_OVER', 1, '16:00', NULL, 'Afternoon arrival on crater rim, romantic dinner, overnight at Ngorongoro', @now);

-- Day 2: Ngorongoro Crater Floor
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 2, 'Day 2', 'Ngorongoro Crater — Big Five & Private Picnic',
    'Descend into the Ngorongoro Crater for an immersive full-day game drive among Africa''s most spectacular wildlife concentration. A private bush picnic on the crater floor makes this a quintessentially romantic day.',
    'Dawn descent into the caldera as mist clears from the crater floor. Morning game drive seeking the Big Five — black rhino, lion, elephant, buffalo, and leopard are all resident in the crater.',
    'Private picnic lunch at the Ngoitokitok Springs hippo pool. Afternoon drive focusing on predator activity — resident cheetah and hyena clans are highly active in the afternoons.',
    'Ascend the crater rim and return to lodge. Sunset champagne on your private balcony overlooking the caldera.',
    'Black rhino (Tanzania''s best viewing), lion prides, leopard, elephant, buffalo, spotted hyena, flamingo on Lake Magadi, wildebeest, zebra, eland, crowned crane',
    'The intimate enclosed caldera with wildlife visible in every direction, Lake Magadi flamingo shores, Lerai Forest, dramatic caldera walls encircling the floor',
    'This is widely considered the finest single-day wildlife experience in Africa. Ask for the private picnic upgrade for the most romantic experience.',
    'Ngorongoro', 'Ngorongoro', 0, 1, 'B,L,D', @now, @now);
SET @day_42_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_42_2, @park_ngorongoro, 'DAY_TRIP', 1, '07:00', '17:00', 'Full day crater floor Big Five game drive with private picnic', @now);

-- Day 3: Ngorongoro → Serengeti
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 3, 'Day 3', 'Highland Drive to Serengeti — Plains Arrival',
    'Drive from Ngorongoro through the ancient highlands and descend onto the boundless Serengeti plains. The transition from the enclosed caldera to Africa''s most famous open savanna is one of the great reveals in wildlife travel.',
    'Leisurely breakfast at the crater rim. Drive west through the Ngorongoro highlands, stopping at Olduvai Gorge — the cradle of humanity — for a short guided tour of this extraordinary archaeological site.',
    'Descend onto the Serengeti plains. Enter the park and begin an afternoon game drive in the Seronera Valley, searching for the resident lion prides and leopards in the sausage trees.',
    'Romantic bush dinner at your Serengeti luxury camp under a canvas of stars.',
    'Lion, leopard (sausage trees), cheetah, wildebeest, zebra, giraffe, hippo, crocodile in Seronera River',
    'The moment the Serengeti plains open up before you, Olduvai Gorge escarpment, Seronera River with its ancient sausage trees, the Serengeti horizon at sunset',
    'Luxury tented camps in the Serengeti offer private plunge pools and couples'' sleep-out starbed options — inquire at booking.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_42_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_42_3, @park_serengeti, 'SLEEP_OVER', 1, '14:00', NULL, 'Afternoon arrival and Seronera Valley game drive, overnight in Serengeti', @now);

-- Day 4: Full Serengeti — Bush Dinner
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 4, 'Day 4', 'Full Serengeti & Kopje Sundowner',
    'A full day exploring the Serengeti''s Central, Seronera, and Lobo regions. End the day with a private sundowner atop a granite kopje as the African sun sets over the endless plains — one of the most romantic moments on the continent.',
    'Dawn game drive for active predators — early light is prime time for cheetah hunts and lion activity. Track the seasonal wildebeest movement with your guide.',
    'Afternoon game drive across different Serengeti circuits. Follow the Great Migration herds if in season, or seek out elusive leopard, wild dog, and secretive serval.',
    'Private sundowner cocktails on a granite kopje at sunset. Return to camp for a romantic bush dinner under the stars.',
    'Cheetah, lion, leopard, wild dog, wildebeest herds, zebra, giraffe, elephant, hippo, crocodile, serval, caracal',
    'Kopje rock formations at sunset, the vast Serengeti plains stretching to the horizon, Seronera River hippo pools, the Great Migration river crossings (seasonal)',
    'The kopje sundowner is best arranged through your camp — guides know the most spectacular viewing spots depending on the season.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_42_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_42_4, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives and private kopje sundowner, overnight in Serengeti', @now);

-- Day 5: Serengeti Hot Air Balloon
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 5, 'Day 5', 'Serengeti Balloon Safari & Bush Champagne Breakfast',
    'Drift silently over the Serengeti in a hot air balloon at dawn — the ultimate honeymoon experience in Africa. Watch elephants, lions, and herds of wildebeest far below as the sun rises over the plains, then land for a celebratory champagne breakfast in the bush.',
    'Pre-dawn transfer to the balloon launch site. Float over the Serengeti for approximately one hour as the sun rises, witnessing the plains from a perspective available to almost no one. A champagne bush breakfast follows the landing.',
    'Return to camp for rest and a late lunch. Optional afternoon game drive or couples'' spa treatment at camp.',
    'Romantic private dinner at camp with personalized honeymoon setup — speak to camp staff in advance.',
    'Aerial views of elephant herds, lion prides, wildebeest columns, giraffe, zebra — all from above at first light',
    'The Serengeti from 300m in the air at sunrise, the vast plain grid of acacia and savanna, the balloon shadow gliding over the grass',
    'Balloon safaris must be booked well in advance and are weather-dependent. Serengeti Balloon Safaris operates year-round from multiple launch sites.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_42_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_42_5, @park_serengeti, 'SLEEP_OVER', 1, '05:00', NULL, 'Pre-dawn balloon launch, champagne breakfast, afternoon at leisure, overnight', @now);

-- Day 6: Serengeti → Fly to Zanzibar
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 6, 'Day 6', 'Final Serengeti Morning & Fly to Zanzibar',
    'One last morning game drive on the Serengeti plains, then fly from Seronera airstrip to Zanzibar — the Spice Island. Arrive to turquoise Indian Ocean waters, coral-white beaches, and the beginning of a completely different kind of paradise.',
    'Early morning game drive from camp. Final predator sightings as the Serengeti day begins. Return to camp for breakfast and pack-up.',
    'Transfer to Seronera airstrip for the charter flight to Zanzibar (approx 1.5 hours). Arrive at Zanzibar airport and transfer to your beachfront resort. Afternoon beach arrival.',
    'Sunset cocktails with feet in the Indian Ocean. Welcome honeymoon dinner at the resort.',
    'Morning game drive wildlife; Indian Ocean beach birds and tropical marine life at Zanzibar',
    'The Serengeti airstrip and flying over Tanzania''s interior, first views of the Indian Ocean, Zanzibar''s turquoise waters from the plane',
    'The bush-to-beach transition is one of the most celebrated honeymoon contrasts in the world. Pre-arrange a honeymoon beach setup with your resort.',
    'Serengeti', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_42_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_42_6, @park_serengeti, 'DAY_TRIP', 1, '05:30', '10:00', 'Final morning game drive before Seronera airstrip fly-out to Zanzibar', @now);

-- Day 7: Jozani Forest & Beach
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 7, 'Day 7', 'Jozani Forest — Red Colobus & Mangroves',
    'Explore Zanzibar''s ancient Jozani Chwaka Bay National Park, home to the endemic and critically endangered Zanzibar red colobus monkey. Walk the mangrove boardwalk over the tidal waters, then return to the beach for the afternoon.',
    'Morning drive to Jozani Chwaka Bay National Park (45 minutes). Guided forest walk through the indigenous groundwater forest encountering troops of Zanzibar red colobus monkeys. Walk the elevated mangrove boardwalk over Chwaka Bay.',
    'Return to the beach resort for a lazy afternoon. Snorkelling in the coral gardens directly off the beach or relaxing by the pool.',
    'Romantic dinner at the resort. Sunset walk on the beach.',
    'Zanzibar red colobus monkey (endemic, critically endangered), Sykes'' monkey, Ader''s duiker, mangrove crabs, mudskippers, tropical fish in the coral gardens',
    'Ancient indigenous forest canopy, mangrove boardwalk over tidal flats, Chwaka Bay coastal panorama, coral white sand beach in the afternoon',
    'The red colobus is found only on Zanzibar and is one of Africa''s rarest primates. Keep voices low in the forest — the monkeys are habituated but not tame.',
    'Zanzibar', 'Zanzibar', 35, 1, 'B,L,D', @now, @now);
SET @day_42_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_42_7, @park_jozani, 'DAY_TRIP', 1, '08:30', '12:00', 'Jozani Forest and mangrove boardwalk morning excursion', @now);

-- Day 8: Beach & Water Sports
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 8, 'Day 8', 'Beach Day — Water Sports & Ocean Adventures',
    'A full day of Indian Ocean adventure with water sports, snorkelling, and pure beach relaxation. Zanzibar''s coral reefs, glass-clear waters, and consistent trade winds make it one of the world''s premier water sports destinations.',
    'Morning snorkelling excursion to the fringing coral reef. Explore the underwater gardens teeming with colourful reef fish, sea turtles, and rays. Optional dolphin-watching boat trip off Kizimkazi.',
    'Afternoon at leisure — try kitesurfing, windsurfing, or a sunset dhow sailing trip. Or simply relax on the beach with fresh coconuts.',
    'Private beachside dinner set up by the resort. Moonlit ocean sounds.',
    'Green sea turtle, hawksbill turtle, spinner dolphin, reef fish, eagle ray, moray eel in the coral reef',
    'Crystal turquoise Indian Ocean, white coral sand beaches, coral reef gardens underwater, traditional wooden dhow sailing at sunset',
    'Water sports availability depends on tides and wind direction — your resort can advise on the best activities each day. Dolphin-watching trips are best in the morning.',
    'Zanzibar', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_42_8 = LAST_INSERT_ID();

-- Day 9: Stone Town & Spice Tour
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 9, 'Day 9', 'Stone Town UNESCO Heritage & Spice Plantation',
    'Discover Zanzibar''s extraordinary Swahili cultural heritage with a guided tour of UNESCO-listed Stone Town and a hands-on spice plantation tour. This is the most culturally rich day of the honeymoon.',
    'Morning guided walking tour of Stone Town — explore the labyrinthine streets, the Old Fort, the Sultan''s Palace, the famous carved doors, and the vibrant Darajani Market. Learn about the Swahili, Arab, Indian, and colonial influences that shaped the island.',
    'Traditional spice plantation tour — taste fresh cloves, nutmeg, cinnamon, vanilla, cardamom, and black pepper straight from the plant. Learn about Zanzibar''s centuries-old role as the world''s Spice Island.',
    'Return to the beach resort or enjoy dinner at one of Stone Town''s renowned rooftop restaurants overlooking the harbour.',
    'Tropical birds, fruit bats roosting in Stone Town trees',
    'Stone Town''s intricate carved-door architecture, the Stone Town harbour at sunset, tropical spice plantation with fruiting trees',
    'Stone Town is a living city, not a museum — be respectful of local customs, especially near mosques. The spice tour is an excellent immersive experience for all ages.',
    'Zanzibar', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_42_9 = LAST_INSERT_ID();

-- Day 10: Beach & Couples Spa
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 10, 'Day 10', 'Beach Bliss & Couples'' Spa Day',
    'The ultimate relaxation day — a private couples'' spa treatment followed by complete beach indulgence. This day is entirely about rest, pampering, and enjoying each other''s company in paradise.',
    'Late breakfast at leisure. Morning swim or beach walk at low tide exploring the reef flat and rock pools.',
    'Couples'' spa treatment — choose from traditional Zanzibar coconut oil massage, seaweed body wrap, or a full hammam experience using local spice-infused oils.',
    'Private romantic dinner on the beach — candles, flowers, a personal chef, and the sound of the Indian Ocean.',
    'Shore birds, crabs, and reef flat marine life at low tide',
    'Low-tide reef flat stretching to the horizon, the resort''s private beach at golden hour, candlelit beach dinner setup',
    'Book the couples'' spa and private beach dinner well in advance — both require advance arrangement with the resort concierge.',
    'Zanzibar', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_42_10 = LAST_INSERT_ID();

-- Day 11: Beach & Sunset Dhow Cruise
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 11, 'Day 11', 'Lazy Beach Morning & Sunset Dhow Cruise',
    'A relaxed beach morning followed by the quintessential Zanzibar romantic experience — a private sunset dhow cruise along the coast. The traditional wooden sailboat drifts silently as the sky turns gold and then pink over the Indian Ocean.',
    'Free morning — final beach swim, snorkelling, or simply reading in a hammock. Optional cooking class using Zanzibar spices and fresh seafood.',
    'Afternoon at leisure at the resort. Pack for departure preparations for tomorrow.',
    'Private sunset dhow cruise — sail along the Zanzibar coast with a bottle of champagne as the sun dips into the Indian Ocean. The most romantic moment of the entire journey.',
    'Seabirds, flying fish, occasional dolphin pods around the dhow',
    'Zanzibar coastline from the water, the dhow''s billowing white sail against the sunset sky, bioluminescent plankton on night return',
    'Sunset dhow cruises should be booked at least 24 hours in advance through your resort. Choose a full private dhow rather than a shared trip for the most romantic experience.',
    'Zanzibar', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_42_11 = LAST_INSERT_ID();

-- Day 12: Mnemba Atoll Snorkelling
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 12, 'Day 12', 'Mnemba Atoll — Snorkelling Paradise',
    'The crown jewel of Zanzibar''s ocean experiences — a boat trip to Mnemba Atoll, one of the Indian Ocean''s most pristine coral ecosystems. Swim with spinner dolphins, turtles, and an extraordinary diversity of tropical fish on a fringing reef in perfect water clarity.',
    'Boat transfer to Mnemba Atoll (30 minutes). Morning snorkelling session on the outer reef with a marine guide. Encounter spinner dolphins, green turtles, hawksbill turtles, and hundreds of reef fish species in the crystal-clear water.',
    'Beach picnic on a sandbank near the atoll. Second snorkelling session in the afternoon. Relax on the boat with the atoll to yourselves.',
    'Final romantic dinner at the beach resort. Toast to the journey.',
    'Spinner dolphin, green sea turtle, hawksbill turtle, reef white-tip shark, humphead parrotfish, Napoleon wrasse, lionfish, eagle ray, over 200 coral fish species',
    'Mnemba Atoll''s pristine coral gardens, underwater visibility of 20-30 metres, the private sandbank at low tide, Zanzibar coastline visible on the horizon',
    'Mnemba Island itself is private but the atoll reef is accessible. Marine Park fees apply. Bring underwater camera housing for exceptional photography.',
    'Zanzibar', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_42_12 = LAST_INSERT_ID();

-- Day 13: Departure
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_42, 13, 'Day 13', 'Farewell Zanzibar — Departure Day',
    'A final morning walk along the beach before transferring to Zanzibar airport for the journey home. This 13-day honeymoon journey from Africa''s highest summit to the Serengeti plains to the Indian Ocean is one of the world''s great travel experiences.',
    'Final sunrise on the beach. Leisurely breakfast at the resort. Last swim in the Indian Ocean.',
    'Check out and transfer to Zanzibar Abeid Amani Karume International Airport for your onward flight.',
    NULL,
    NULL,
    'The Zanzibar beach at first light for the final time, the resort at morning',
    'Departure airport transfers should be arranged with the resort 24 hours in advance. Allow extra time for Zanzibar airport security.',
    'Zanzibar', 'Zanzibar', 0, 0, 'B', @now, @now);
SET @day_42_13 = LAST_INSERT_ID();
-- ============================================================
-- ITINERARY 43: Complete Northern Tanzania & Zanzibar Experience (14 days, 13 nights)
-- ============================================================

-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 1, 'Day 1', 'Arusha — Gateway to the Northern Circuit',
    'Begin your 14-day adventure with an afternoon visit to Arusha National Park, the compact gem at the foot of Mount Meru. Spot colobus monkeys, giraffes, and zebras against the dramatic backdrop of an ancient volcanic caldera.',
    'Arrive in Arusha, meet your guide, and complete pre-departure briefings. Lunch at your Arusha lodge before driving 35 km to Arusha National Park for the afternoon.',
    'Afternoon game drive through the Momella Lakes section of Arusha National Park. Look for flamingos on the alkaline lakes and buffalos in the forest glades.',
    'Return to Arusha for dinner and overnight at your lodge.',
    'Colobus monkey, giraffe, zebra, buffalo, flamingo, warthog, blue monkey',
    'Momella Lakes, Mount Meru silhouette, fig-forest canopy, Ngurdoto Crater rim views',
    'This is a gentle introduction day — use the afternoon to acclimatise to Tanzania''s altitude and climate before the main safari begins.',
    'Arusha', 'Arusha', 35, 1, 'L,D', @now, @now);
SET @day_43_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_1, @park_arusha, 'DAY_TRIP', 1, '13:00', '17:30', 'Afternoon game drive, return to Arusha for overnight', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 2, 'Day 2', 'Mkomazi — Rhino Sanctuary & Wilderness',
    'Drive east to the remote Mkomazi National Park, home to one of Tanzania''s most important black rhino sanctuaries. This vast wilderness bordering Kenya''s Tsavo offers an off-the-beaten-path experience with exceptional predator sightings.',
    'Early breakfast and depart Arusha for Mkomazi National Park (approximately 110 km). Enter the park and drive through open acacia-commiphora bushland.',
    'Visit the Mkomazi Rhino Sanctuary for a guided briefing and viewing opportunity. Continue afternoon game drive searching for elephant, gerenuk, and lesser kudu.',
    'Settle into your park camp for the night. Sundowner drinks with views over the Tsavo plains.',
    'Black rhino, African wild dog, elephant, gerenuk, lesser kudu, fringe-eared oryx, cheetah, lion',
    'Open acacia-commiphora bushlands, the Pare Mountains backdrop, sweeping views towards Tsavo National Park',
    'Mkomazi is one of Tanzania''s least-visited parks — enjoy the exclusivity. Wild dog packs have established territories here; early morning offers the best sighting chances.',
    'Arusha', 'Mkomazi NP', 110, 1, 'B,L,D', @now, @now);
SET @day_43_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_2, @park_mkomazi, 'SLEEP_OVER', 1, '11:00', NULL, 'Full afternoon game drive and rhino sanctuary visit, overnight in park', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 3, 'Day 3', 'Full Day in Mkomazi — Wild Dogs & Open Wilderness',
    'Spend a full day exploring Mkomazi''s untamed landscapes, dedicated to tracking the resident African wild dog packs and other rare species. Dawn game drives yield the best predator activity across this remote northern park.',
    'Pre-dawn wake-up for a sunrise game drive following known wild dog territories. The early light illuminates the Pare Mountains and Kilimanjaro on clear mornings.',
    'Picnic lunch in the bush. Afternoon drive through the Dindira Dam area and the western game circuits for elephant, lion, and giraffe.',
    'Campfire dinner under a sky undimmed by light pollution. Evening star-gazing briefing by your guide.',
    'African wild dog, black rhino, elephant, lion, giraffe, fringe-eared oryx, gerenuk, bat-eared fox, over 450 bird species',
    'Dindira Dam waterhole, volcanic hills, Tsavo border views, untouched acacia woodland',
    'Mkomazi receives very few visitors — game drives here feel truly wild. This is one of the best parks in East Africa to see wild dogs with minimal vehicle competition.',
    'Mkomazi NP', 'Mkomazi NP', 0, 1, 'B,L,D', @now, @now);
SET @day_43_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_3, @park_mkomazi, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day game drives with wild dog focus, overnight in park', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 4, 'Day 4', 'Mkomazi to Tarangire — Land of Giants',
    'Depart Mkomazi and drive west via Arusha to Tarangire National Park, famed for its enormous elephant herds and ancient baobab trees. Arrive in time for an afternoon game drive along the life-giving Tarangire River.',
    'Early breakfast and long transfer west through Arusha town (approximately 230 km total). Stop in Arusha for a comfort break and lunch before continuing to Tarangire.',
    'Enter Tarangire National Park in the early afternoon. Begin game drives along the Tarangire River where elephants gather to drink in their hundreds.',
    'Sundowner game drive through the baobab-studded hills. Overnight at your Tarangire bush camp.',
    'African elephant, lion, leopard, giraffe, zebra, wildebeest, impala, fringe-eared oryx, python',
    'Ancient baobab landscape, Tarangire River valley with elephant silhouettes, sweeping Masai Steppe views',
    'The transfer via Arusha is long but the scenery changes dramatically — from semi-arid Mkomazi to the lush baobab savanna of Tarangire. Arrive early enough to maximise afternoon game viewing.',
    'Mkomazi NP', 'Tarangire NP', 230, 1, 'B,L,D', @now, @now);
SET @day_43_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_4, @park_tarangire, 'SLEEP_OVER', 1, '14:00', NULL, 'Afternoon arrival and game drive, overnight in park', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 5, 'Day 5', 'Full Day Tarangire — Elephants & Baobabs',
    'A dedicated full day in Tarangire exploring all the park''s diverse habitats, from the river floodplain to the seasonal swamps and acacia woodland. Tarangire''s elephant herds are among the largest anywhere in East Africa.',
    'Dawn game drive targeting lion and leopard in the baobab hills before the heat builds. The river crossing points are busy with elephant families at first light.',
    'Picnic lunch at a designated bush spot. Afternoon circuit through the Silale and Gursi swamps where thousands of birds gather.',
    'Evening game drive for nocturnal species. Bush dinner under the stars at your camp.',
    'African elephant, lion, leopard, cheetah, wild dog (seasonal), giraffe, zebra, buffalo, python, over 550 bird species',
    'Tarangire River flanked by doum palms, ancient baobab groves, the Silale swamps teeming with waterbirds',
    'Tarangire is at its peak in the dry season when animals concentrate around the river. The park is also outstanding for birds — bring your field guide.',
    'Tarangire NP', 'Tarangire NP', 0, 1, 'B,L,D', @now, @now);
SET @day_43_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_5, @park_tarangire, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives across all circuits, overnight in park', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 6, 'Day 6', 'Tarangire to Lake Manyara — Flamingos & Tree Lions',
    'Drive north from Tarangire to the narrow ribbon of Lake Manyara National Park, famous for its tree-climbing lions and immense flocks of flamingos on the soda lake. The lush groundwater forest feels like a different world from the dry savanna.',
    'Early game drive in Tarangire before breakfast. Check out and drive 70 km north to Lake Manyara National Park.',
    'Afternoon game drive through the fig-forest canopy and along the lakeshore. Search the fever trees for the park''s famous tree-climbing lions.',
    'Return to your lodge on the crater escarpment above the park for dinner and overnight.',
    'Tree-climbing lion, elephant, hippo, flamingo, blue monkey, baboon, impala, buffalo, over 400 bird species',
    'Lake Manyara''s pink flamingo shoreline, the dense groundwater forest canopy, Rift Valley escarpment views',
    'Lake Manyara is a compact park best explored in the morning and afternoon light. The flamingos and tree-climbing lions are the signature sightings — ask your guide for the latest lion tree locations.',
    'Tarangire NP', 'Manyara area', 70, 1, 'B,L,D', @now, @now);
SET @day_43_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_6, @park_manyara, 'DAY_TRIP', 1, '13:00', '18:00', 'Afternoon game drive, overnight at lodge outside park', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 7, 'Day 7', 'Ngorongoro Crater — The Eighth Wonder',
    'Descend into the Ngorongoro Crater, the world''s largest intact volcanic caldera and home to the densest concentration of large mammals on earth. A full crater floor game drive offers unparalleled big-five encounters within a stunning natural amphitheatre.',
    'Drive 60 km from the Manyara area up to the Ngorongoro crater rim. Descend 600 metres to the crater floor and begin morning game drives.',
    'Picnic lunch near the Ngoitokitok Springs hippo pool. Afternoon search for black rhino in the open grasslands and observe lion prides near the Lerai Forest.',
    'Ascend to the rim in late afternoon. Dinner and overnight at your rim lodge with sweeping crater views.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, spotted hyena, cheetah, zebra, wildebeest',
    'Panoramic 600-metre crater rim, Lerai Forest, Lake Magadi flamingo shores, vast caldera floor grasslands',
    'Vehicle numbers on the crater floor are limited — early descent is essential. The black rhino here are among the easiest to see anywhere in Tanzania.',
    'Manyara area', 'Ngorongoro rim', 60, 1, 'B,L,D', @now, @now);
SET @day_43_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_7, @park_ngorongoro, 'DAY_TRIP', 1, '08:30', '17:00', 'Full day crater floor game drive, overnight on rim', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 8, 'Day 8', 'Ngorongoro to the Serengeti Plains',
    'Cross from the Ngorongoro highlands into the infinite Serengeti, the most famous wildlife ecosystem on earth. Drive through the Ndutu woodlands and onto the rolling plains where the great wildebeest migration plays out.',
    'Depart the Ngorongoro rim early and drive 145 km west through the Ngorongoro Conservation Area highlands into Serengeti National Park.',
    'Game-drive en route through the Ndutu area and into the central Seronera Valley. Afternoon drives in search of the big five.',
    'Sunset over the Serengeti plains. Dinner and overnight at your Serengeti camp.',
    'Wildebeest, lion, leopard, cheetah, elephant, giraffe, zebra, hyena, crocodile, topi',
    'The endless Serengeti plains, kopje rock outcrops, the Seronera River valley, vast migration herds when in season',
    'Timing your entry to maximise migration viewing is key — your guide will have current intelligence on herd locations. The Seronera area holds resident predators year-round.',
    'Ngorongoro rim', 'Serengeti NP', 145, 1, 'B,L,D', @now, @now);
SET @day_43_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_8, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon arrival and game drive, overnight in park', @now);

-- Day 9
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 9, 'Day 9', 'Full Serengeti — The Great Migration',
    'Dedicate a full day to tracking the wildebeest migration across the Serengeti''s golden plains. Witness predator-prey interactions, river crossings, and the sheer spectacle of over a million animals moving as one living tide.',
    'Pre-dawn departure to position for the migration herds at first light. Follow the wildebeest columns moving across the plains with cheetah and lion in close attendance.',
    'Bush picnic lunch in the field. Afternoon dedicated to finding the day''s river crossings or locating resting big cat families on kopjes.',
    'Campfire dinner with your guide sharing stories of the migration''s history and ecology.',
    'Wildebeest, zebra, Thomson''s gazelle, Grant''s gazelle, lion, leopard, cheetah, hyena, jackal, crocodile, vulture',
    'Sweeping migration herds on the open plains, Mara River crossing sites, kopje silhouettes at sunset',
    'Migration location shifts seasonally — June to July sees the northern push towards the Mara; January to March offers calving in the south. Your guide will plan routes accordingly.',
    'Serengeti NP', 'Serengeti NP', 0, 1, 'B,L,D', @now, @now);
SET @day_43_9 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_9, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day migration game drives, overnight in park', @now);

-- Day 10
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 10, 'Day 10', 'Full Serengeti — Predators & Vast Plains',
    'A second full Serengeti day to explore different circuits and encounter the park''s permanent resident predators. The Seronera Valley''s woodland and river systems support one of Africa''s highest leopard densities.',
    'Early morning drive along the Seronera River searching for leopard in the sausage trees and lion on rocky outcrops. Hyena den visits are possible in the first light.',
    'Explore the Simba Kopjes and the Western Corridor if migration herds have moved. Afternoon dedicated to cheetah tracking on the open plains.',
    'Relaxed evening at camp. Optional guided walking on the camp perimeter with an armed ranger.',
    'Leopard, lion, cheetah, spotted hyena, side-striped jackal, giraffe, buffalo, hippo, African rock python',
    'Seronera River valley, the iconic Simba and Moru Kopjes, golden hour over the endless plains',
    'Allow time to simply sit and observe rather than driving constantly. Some of the finest wildlife encounters happen when you stop, let the animals settle, and watch behaviour unfold.',
    'Serengeti NP', 'Serengeti NP', 0, 1, 'B,L,D', @now, @now);
SET @day_43_10 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_10, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives through multiple circuits, overnight in park', @now);

-- Day 11
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 11, 'Day 11', 'Serengeti Morning & Fly to Zanzibar',
    'A final morning game drive on the Serengeti plains before boarding a scenic flight to Zanzibar Island. Exchange the savanna for white sand beaches and the turquoise waters of the Indian Ocean as the safari transitions to island paradise.',
    'Early morning game drive to make the most of the last hours in the Serengeti. Transfer to the airstrip for the midday flight to Zanzibar.',
    'Arrive Zanzibar, transfer to your beachside hotel, and enjoy the afternoon at leisure on the coast.',
    'Dinner at your Zanzibar hotel. Stroll the beach at sunset as the Indian Ocean glows golden.',
    'Any remaining savanna wildlife during the morning game drive',
    'Aerial views of the Serengeti from the flight, turquoise Zanzibar lagoon and coral-sand beaches',
    'The flight from Serengeti to Zanzibar typically connects via Arusha or Dar es Salaam — confirm timings with your operator. Check-in at Zanzibar is usually in the afternoon.',
    'Serengeti NP', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_43_11 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_11, @park_serengeti, 'DAY_TRIP', 1, '06:00', '11:00', 'Morning game drive before airstrip transfer to Zanzibar', @now);

-- Day 12
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 12, 'Day 12', 'Jozani Forest — Red Colobus & Mangroves',
    'Explore Jozani Chwaka Bay National Park, Zanzibar''s only national park and the last stronghold of the endemic Zanzibar red colobus monkey. A guided forest walk and mangrove boardwalk reveal the island''s extraordinary biodiversity.',
    'Morning guided walk through the ancient coral rag forest of Jozani. The Zanzibar red colobus troops are habitually encountered along the main forest trail.',
    'Walk the mangrove boardwalk at Chwaka Bay. Afternoon at leisure at the beach, or optional spice farm visit nearby.',
    'Dinner at your Zanzibar hotel. Enjoy fresh seafood on the beach.',
    'Zanzibar red colobus monkey (endemic), blue duiker, Ader''s duiker, bush baby, Zanzibar leopard (rarely seen), over 50 bird species including Fischer''s turaco',
    'Ancient coral rag forest interior, mangrove creek boardwalk views, Chwaka Bay tidal estuary',
    'The Zanzibar red colobus is one of Africa''s rarest primates and found nowhere else on earth. Keep voices low and follow guide instructions to avoid disturbing the troops.',
    'Zanzibar', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_43_12 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_43_12, @park_jozani, 'DAY_TRIP', 1, '08:30', '13:00', 'Guided forest and mangrove walk, return to beach hotel', @now);

-- Day 13
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 13, 'Day 13', 'Stone Town & Sunset Dhow Cruise',
    'Discover Stone Town, Zanzibar''s UNESCO-listed historic heart, on a guided walking tour through its labyrinthine coral-stone streets. End the day on a traditional dhow as the sun sets over the Indian Ocean horizon.',
    'Morning guided walking tour of Stone Town visiting the Old Fort, Freddie Mercury birthplace, the House of Wonders, and the former slave market memorial.',
    'Afternoon at leisure for shopping in the aromatic spice markets and craft stalls. Optional visit to the Palace Museum.',
    'Traditional sunset dhow cruise along the Zanzibar shoreline. Return to your hotel for the final night.',
    'Tropical fish and sea turtles visible from the dhow on calm evenings',
    'Stone Town''s coral-stone architecture and carved wooden doors, Indian Ocean sunset from the dhow, the iconic Forodhani waterfront',
    'Stone Town is best explored on foot with a guide who can navigate the unmarked streets. Book your dhow cruise in advance as spots fill quickly during peak season.',
    'Zanzibar', 'Zanzibar', 0, 1, 'B,L,D', @now, @now);
SET @day_43_13 = LAST_INSERT_ID();

-- Day 14
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_43, 14, 'Day 14', 'Zanzibar — Departure',
    'Enjoy a final leisurely breakfast by the Indian Ocean before transferring to Zanzibar International Airport for your homeward flight. The 14-day journey has spanned rhino sanctuaries, endless migration plains, spice islands, and ancient forests.',
    'Final breakfast at your Zanzibar hotel. Last swim or beach walk before checkout.',
    'Transfer to Zanzibar International Airport according to your flight schedule.',
    'N/A',
    'N/A',
    'Zanzibar''s turquoise lagoon for a final farewell',
    'Allow at least 3 hours before departure for airport check-in. International connections usually route via Dar es Salaam or Nairobi.',
    'Zanzibar', 'International departure', 0, 0, 'B', @now, @now);
SET @day_43_14 = LAST_INSERT_ID();


-- ============================================================
-- ITINERARY 44: Tanzania Wildlife Photography Grand Tour (13 days, 12 nights)
-- ============================================================

-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 1, 'Day 1', 'Arusha National Park — Mount Meru & Colobus',
    'Open your photography grand tour with an afternoon in Arusha National Park, where the snow-capped cone of Mount Meru provides a dramatic backdrop for portraits of black-and-white colobus monkeys and giraffe. The park''s compact size makes it ideal for practising camera settings before the main safari.',
    'Arrive Arusha, meet your guide and photography briefing. Lunch at your Arusha hotel before driving 35 km to Arusha National Park.',
    'Afternoon photography drive through the Momella Lakes area and the montane forest zones. Work on colobus monkey portraits and wide-angle Mount Meru compositions.',
    'Return to Arusha for dinner and overnight at your lodge.',
    'Black-and-white colobus monkey, giraffe, zebra, buffalo, flamingo, warthog, bushbuck',
    'Mount Meru volcanic cone, Momella Lakes reflections, fig-forest canopy light',
    'Arusha NP offers excellent light for photography in the late afternoon. The colobus monkeys in the fig forest near Ngurdoto are usually active and approachable.',
    'Arusha', 'Arusha', 35, 1, 'L,D', @now, @now);
SET @day_44_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_1, @park_arusha, 'DAY_TRIP', 1, '13:00', '17:30', 'Afternoon photography drive, return to Arusha for overnight', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 2, 'Day 2', 'Tarangire — Elephant Photography Among the Baobabs',
    'Drive to Tarangire National Park, the crown jewel for elephant photography in northern Tanzania. Hundreds of elephants gather along the Tarangire River, offering intimate close-up and environmental portrait opportunities against iconic baobab backdrops.',
    'Early breakfast and drive 120 km to Tarangire. Enter the park and position along the river for morning elephant activity.',
    'Photograph elephant families at the river crossings and dust-bathing sites. Explore the baobab groves for wide landscape compositions.',
    'Sunset photography from a high vantage point over the river valley. Overnight at your Tarangire bush camp.',
    'African elephant (large herds), lion, leopard, giraffe, zebra, impala, fringe-eared oryx, python',
    'Ancient baobab groves, Tarangire River elephant gatherings, golden-hour savanna light',
    'Arrive before noon to secure the best afternoon light positions along the river. A 500mm+ lens is recommended for intimate elephant portraits while maintaining a respectful distance.',
    'Arusha', 'Tarangire NP', 120, 1, 'B,L,D', @now, @now);
SET @day_44_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_2, @park_tarangire, 'SLEEP_OVER', 1, '11:00', NULL, 'Afternoon photography, overnight in park', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 3, 'Day 3', 'Full Tarangire — Birds, Baobabs & Behaviour',
    'A full dedicated photography day in Tarangire focused on the park''s world-class birdlife and the intimate behavioural shots that only extended time in one place can yield. Over 550 species make this one of Africa''s top birding destinations.',
    'Dawn photography drive targeting yellow-collared lovebirds, red-and-yellow barbets, and the lilac-breasted roller in the baobab canopy.',
    'Midday photography of elephant family dynamics at the river. Afternoon bird photography at the Silale swamps using the vehicle as a mobile hide.',
    'Evening light over the baobab silhouettes — a classic Tarangire composition. Bush dinner under the stars.',
    'Over 550 bird species including lilac-breasted roller, yellow-collared lovebird, red-and-yellow barbet, martial eagle, kori bustard, saddle-billed stork',
    'Baobab silhouettes against sunrise and sunset sky, Silale swamp waterbird reflections, elephant dust clouds in raking light',
    'Use the Silale swamp area in the afternoon for low-angle waterbird shots. The swamp''s shallow edges allow close approach by vehicle for frame-filling bird photography.',
    'Tarangire NP', 'Tarangire NP', 0, 1, 'B,L,D', @now, @now);
SET @day_44_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_3, @park_tarangire, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day photography across all habitats, overnight in park', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 4, 'Day 4', 'Lake Manyara — Tree-Climbing Lions & Forest Light',
    'Drive to Lake Manyara National Park for one of Africa''s most sought-after photography subjects — lions resting in the branches of fever trees above the forest floor. The dappled groundwater forest light creates dramatic contrast for creative compositions.',
    'Early game drive in Tarangire before check-out. Drive 70 km north to Lake Manyara and enter the park for midday.',
    'Afternoon photography in the groundwater forest targeting tree-climbing lions, blue monkeys, and the lakeshore flamingo flocks.',
    'Exit Manyara and overnight at a lodge on the Rift Valley escarpment above the park.',
    'Tree-climbing lion, African elephant, flamingo, blue monkey, baboon, hippo, buffalo, impala',
    'Rift Valley escarpment views over the soda lake, fever tree forest interior light, flamingo-pink lakeshore',
    'Tree-climbing lion positions change daily — ask your driver for the latest sighting reports before entering the park. Mid-day shooting in the forest works well due to the diffused canopy light.',
    'Tarangire NP', 'Manyara area', 70, 1, 'B,L,D', @now, @now);
SET @day_44_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_4, @park_manyara, 'DAY_TRIP', 1, '13:00', '18:00', 'Afternoon photography drive, overnight at escarpment lodge', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 5, 'Day 5', 'Ngorongoro Crater — Rhino & Big Five Photography',
    'Descend into the Ngorongoro Crater for a full day of big-five photography within the world''s largest intact caldera. The concentration of wildlife means multiple species can be photographed in quick succession throughout the day.',
    'Early descent from the rim into the crater at 06:30. Position near the Lerai Forest for lion and elephant in the soft morning light.',
    'Picnic lunch near the hippo pool. Afternoon searching for black rhino in the open grasslands — a rare and prized photographic subject.',
    'Ascend to the rim in late afternoon. Dinner and overnight at your rim lodge.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, spotted hyena, cheetah, zebra, wildebeest',
    'Crater floor panorama from the descent road, Lerai Forest atmosphere, Lake Magadi flamingo reflections',
    'The crater''s restricted vehicle numbers mean less competition at sightings. Bring a teleconverter for rhino shots as safe distances are maintained. The 600-metre ascent road offers dramatic landscape photography.',
    'Manyara area', 'Ngorongoro rim', 60, 1, 'B,L,D', @now, @now);
SET @day_44_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_5, @park_ngorongoro, 'DAY_TRIP', 1, '06:30', '17:00', 'Full day crater photography, overnight on rim', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 6, 'Day 6', 'Ngorongoro — Maasai Cultural Photography',
    'Dedicate a day to cultural photography in the Ngorongoro Conservation Area, where Maasai communities continue their traditional pastoral lifestyle alongside wildlife. A guided village visit provides respectful, consensual portrait and documentary photography opportunities.',
    'Morning photography excursion to a Maasai boma for cultural portraits. Learn about traditional dress, jewelry, and cattle herding practices with your community guide.',
    'Afternoon photography walk on the crater rim capturing the vast panoramic views and the Maasai landscape. Visit the Olduvai Gorge museum for historical context if time allows.',
    'Sundowner on the crater rim. Overnight at your Ngorongoro rim lodge.',
    'Maasai livestock herds, cattle egrets, martial eagle on the rim thermals',
    'The Ngorongoro crater rim panorama, traditional Maasai ochre and beadwork against the highland landscape',
    'Always negotiate and pay agreed photography fees directly with community members. Your guide will facilitate respectful introductions. A 50-85mm lens works well for environmental portraits.',
    'Ngorongoro rim', 'Ngorongoro rim', 0, 1, 'B,L,D', @now, @now);
SET @day_44_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_6, @park_ngorongoro, 'SLEEP_OVER', 1, '07:00', NULL, 'Cultural photography day, overnight on rim', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 7, 'Day 7', 'Ngorongoro to the Serengeti — Migration Landscapes',
    'Cross into the Serengeti ecosystem and begin photographing the planet''s greatest wildlife spectacle. The 145-kilometre drive through the Ngorongoro highlands and into the Ndutu woodlands is itself a gallery of ever-changing landscapes.',
    'Depart the Ngorongoro rim and drive west into Serengeti National Park. Game-drive en route through the Ndutu area photographing wildebeest columns.',
    'Arrive central Serengeti by midday. Afternoon photography session around the Seronera Valley targeting resident leopards in the sausage trees.',
    'Spectacular Serengeti sunset photography. Overnight at your Serengeti camp.',
    'Wildebeest, zebra, Thomson''s gazelle, lion, leopard, cheetah, giraffe, elephant',
    'The transition from highland to open plain, wildebeest columns stretching to the horizon, Seronera kopje silhouettes',
    'The drive from Ngorongoro to Serengeti passes through several distinct vegetation zones — keep your camera accessible throughout the transit for impromptu road-side shots.',
    'Ngorongoro rim', 'Serengeti NP', 145, 1, 'B,L,D', @now, @now);
SET @day_44_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_7, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon arrival and photography session, overnight in park', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 8, 'Day 8', 'Full Serengeti — Predator Photography',
    'A dedicated predator photography day in the Serengeti, focusing on the specialised hunting behaviour and family dynamics of lion, cheetah, and leopard. Patient positioning near known territory centres yields the most intimate results.',
    'Pre-dawn departure to position near known lion pride territories before the light grows. Photograph the pride moving, interacting, and hunting in the golden-hour light.',
    'Midday cheetah search on the open plains — the short grass areas offer unobstructed full-body compositions. Afternoon leopard photography in the Seronera River sausage trees.',
    'Review images at camp. Evening debrief and guidance on the following day''s photographic priorities.',
    'Lion, cheetah, leopard, spotted hyena, black-backed jackal, African wild cat, secretary bird',
    'Open plains with hunting cheetah silhouettes, Seronera River leopard perches, kopje lion pride resting spots',
    'Patience is the defining skill on a predator photography day. Ask your guide to park downwind of target animals and switch off the engine for extended quiet observation periods.',
    'Serengeti NP', 'Serengeti NP', 0, 1, 'B,L,D', @now, @now);
SET @day_44_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_8, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day predator photography, overnight in park', @now);

-- Day 9
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 9, 'Day 9', 'Full Serengeti — River Crossings',
    'Spend the day at the Mara River during the migration crossing season, or in the calving grounds in the south — whichever the calendar dictates. River crossings are one of wildlife photography''s most dramatic and technically demanding subjects.',
    'Early positioning at a known crossing point on the Mara River (July–October) or on the calving plains near Ndutu (January–March). Wait for the herds to build and commit.',
    'Multiple crossing attempts throughout the day. In between crossings, photograph crocodile and hippo activity in and along the river.',
    'Debrief on the day''s take. Camp dinner with guide review of exposure settings and focus techniques for fast action.',
    'Wildebeest, zebra, Nile crocodile, hippo, lion, hyena, vulture, marabou stork',
    'The Mara River crossing spectacle, crocodile-filled pools, wildebeest columns on the opposite bank building momentum',
    'River crossings are unpredictable — allow a full day at the river and prepare for waiting. Use burst mode and a minimum 1/2000s shutter speed for sharp crossing action shots.',
    'Serengeti NP', 'Serengeti NP', 0, 1, 'B,L,D', @now, @now);
SET @day_44_9 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_9, @park_serengeti, 'SLEEP_OVER', 1, '05:30', NULL, 'Full day river crossing photography, overnight in park', @now);

-- Day 10
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 10, 'Day 10', 'Full Serengeti — Dawn & Dusk Golden Hour',
    'The final full Serengeti day is structured entirely around the golden hours at either end of the day, when the low-angle light transforms the plains into a canvas of warm tones and long shadows. This is Tanzania''s most photogenic light.',
    'Alarm before first light for a star-trail or pre-dawn silhouette session. Then transition to golden-hour game drives as the sun lifts over the horizon.',
    'Midday rest and image review at camp. Late afternoon positioning for the sunset game drive.',
    'Extended sunset session as the sun drops behind the Serengeti horizon. Attempt twilight long-exposure images of the camp or nearby kopjes.',
    'All Serengeti species with emphasis on silhouette and rim-lit compositions',
    'Dawn mist over the plains, giraffe and acacia silhouettes at golden hour, star-filled Serengeti night sky',
    'Arrive at your chosen sunrise location 20 minutes before first light. Bring a tripod for the pre-dawn session. The dusk session benefits from a wide-angle lens to capture the full sky.',
    'Serengeti NP', 'Serengeti NP', 0, 1, 'B,L,D', @now, @now);
SET @day_44_10 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_10, @park_serengeti, 'SLEEP_OVER', 1, '05:00', NULL, 'Full day dawn and dusk golden hour photography, overnight in park', @now);

-- Day 11
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 11, 'Day 11', 'Serengeti Morning & Drive to Kilimanjaro Area',
    'Final Serengeti morning game drive before the long drive east through Arusha and on to the Kilimanjaro region. Arrive in the shadow of Africa''s highest peak in time to prepare for the following day''s rainforest hike.',
    'Last dawn game drive in the Serengeti. Check out and drive east approximately 330 km to the Kilimanjaro area via Arusha.',
    'Afternoon road photography as the landscape transitions from Serengeti plains to the fertile Kilimanjaro foothills. Arrive at your Kilimanjaro base hotel.',
    'Dinner at your hotel. Prepare camera gear and clothing for the Mandara Hut rainforest hike.',
    'Any final Serengeti wildlife encountered during the morning drive',
    'Aerial Kilimanjaro views on the approach road, Mount Meru backdrop through the coffee plantations',
    'The drive from Serengeti to Kilimanjaro is long (5–6 hours). Confirm road conditions and fuel stops with your guide. Arriving before dark allows a first glimpse of Kilimanjaro in evening light.',
    'Serengeti NP', 'Kilimanjaro area', 0, 1, 'B,L,D', @now, @now);
SET @day_44_11 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_11, @park_serengeti, 'DAY_TRIP', 1, '06:00', '10:00', 'Morning game drive before departure for Kilimanjaro region', @now);

-- Day 12
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 12, 'Day 12', 'Kilimanjaro NP — Mandara Hut Rainforest Hike',
    'Hike from the Marangu Gate through the ancient montane rainforest of Kilimanjaro National Park to Mandara Hut and back. The mossy forest is home to colobus monkeys and over 180 bird species, offering a dramatic change from the open savanna.',
    'Early departure for the Marangu Gate. Begin the 8.5 km hike to Mandara Hut (2,700m) through lush rainforest. The trail provides canopy-level photography opportunities of colobus troops.',
    'Lunch at Mandara Hut with views of the forest and, on clear days, the snowy Kibo summit. Begin the descent through the forest in the afternoon.',
    'Return to your Kilimanjaro area hotel. Final dinner and celebration of a complete photography grand tour.',
    'Black-and-white colobus monkey, white-necked raven, Hartlaub''s turaco, sunbirds, olive thrush, forest buffalo (rare)',
    'Kilimanjaro''s ancient montane rainforest, cloud-forest moss and lichen, Mandara Hut clearing with mountain views',
    'The Mandara Hut day hike requires a valid Kilimanjaro NP entry permit. Wear waterproof layers as the forest is frequently misty. A 24-105mm lens covers both forest close-ups and wider summit scenes.',
    'Kilimanjaro area', 'Kilimanjaro area', 0, 1, 'B,L,D', @now, @now);
SET @day_44_12 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_44_12, @park_kilimanjaro, 'DAY_TRIP', 1, '08:00', '16:00', 'Marangu Gate to Mandara Hut day hike and return, overnight at base hotel', @now);

-- Day 13
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_44, 13, 'Day 13', 'Kilimanjaro Area — Departure',
    'Conclude the 13-day Tanzania Wildlife Photography Grand Tour with a final breakfast in the shadow of Kilimanjaro before transferring to Kilimanjaro International Airport for your homeward flight.',
    'Final breakfast at your hotel. Last opportunity for a sunrise Kilimanjaro mountain portrait from the hotel grounds before checkout.',
    'Transfer to Kilimanjaro International Airport (KIA) according to your flight schedule.',
    'N/A',
    'N/A',
    'Kilimanjaro''s snow-capped summit on a clear morning departure',
    'Kilimanjaro International Airport is approximately 45 km from Moshi and 50 km from Arusha. Allow ample time for check-in — the airport serves as a hub for international connections.',
    'Kilimanjaro area', 'International departure', 0, 0, 'B', @now, @now);
SET @day_44_13 = LAST_INSERT_ID();
-- ============================================================
-- ITINERARY 45: Tanzania Discovery — Backpacker's Dream (14 days / 13 nights)
-- ============================================================

-- Day 1
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 1, 'Day 1', 'Arusha to Arusha National Park — Budget Safari Intro',
    'Kick off your backpacker''s Tanzania adventure with a short drive to Arusha National Park, the most accessible park in northern Tanzania. Enjoy a day of budget-friendly game viewing before returning to Arusha for the night.',
    'Morning briefing at your Arusha guesthouse before a short drive to the park gate. Begin a game drive through the Momella Lakes and fig forest, spotting black-and-white colobus monkeys, waterbuck, and giraffe.',
    'Afternoon game drive around the Momella Lakes, watching flocks of flamingos and diverse waterbirds. Explore the forest zone on a guided walk (optional) for close encounters with wildlife.',
    'Return to Arusha by early evening. Settle in and connect with fellow travellers over a budget dinner in town.',
    'Black-and-white colobus monkey, giraffe, waterbuck, zebra, buffalo, flamingo, hippo, over 400 bird species',
    'Momella Lakes reflecting Mount Meru, fig forest canopy, views of Kilimanjaro on clear days',
    'Arusha NP is ideal for a low-cost first game day — entry fees are lower than other northern-circuit parks. Walking safaris are permitted with an armed ranger.',
    'Arusha', 'Arusha', 35, 1, 'L,D', @now, @now);
SET @day_45_1 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_1, @park_arusha, 'DAY_TRIP', 1, '08:00', '17:00', 'Full day game drive and optional walking safari', @now);

-- Day 2
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 2, 'Day 2', 'Arusha to Mkomazi — Rhino Sanctuary Drive',
    'Drive east to remote Mkomazi National Park, home to Tanzania''s critical black rhino sanctuary and endangered African wild dog packs. This off-the-beaten-path park rewards budget travellers with a genuine wilderness experience far from the tourist trail.',
    'Early breakfast and depart Arusha heading east through the Usambara foothills. Arrive at Mkomazi mid-morning and check in at the budget campsite before a midday game drive through open savanna.',
    'Afternoon game drive through the rhino sanctuary perimeter and wild dog territory. Scout waterholes as the heat draws animals to drink.',
    'Sundowner at camp watching the Mkomazi plains fade into dusk. Camp dinner under a vast sky far from city lights.',
    'Black rhino, African wild dog, elephant, gerenuk, lesser kudu, fringe-eared oryx, cheetah, giraffe',
    'Wide open Tsavo-like savanna, volcanic Pare Mountains backdrop, remote unspoiled wilderness atmosphere',
    'Mkomazi sees very few visitors — book campsites in advance and bring all supplies from Arusha. The rhino sanctuary requires a special guided visit arranged at the gate.',
    'Arusha', 'Mkomazi', 110, 1, 'B,L,D', @now, @now);
SET @day_45_2 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_2, @park_mkomazi, 'SLEEP_OVER', 1, '11:00', NULL, 'Afternoon arrival, game drive, overnight at park campsite', @now);

-- Day 3
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 3, 'Day 3', 'Full Day Mkomazi — Wild Dogs & Wilderness',
    'Spend a full day exploring Mkomazi''s untouched landscapes in search of African wild dogs, one of the continent''s most endangered carnivores. The park''s remoteness and low visitor numbers make every sighting feel truly exclusive.',
    'Dawn game drive starting before sunrise to catch wild dog packs on their morning hunt. Cover the eastern circuits where packs have established dens near seasonal luggas (dry riverbeds).',
    'Midday rest at camp during the heat of the day. Optional visit to the rhino sanctuary with a park ranger for a rare close encounter with black rhino.',
    'Late afternoon game drive through the open plains, watching elephants and gerenuk feed as the light turns golden. Dinner and star-gazing at camp.',
    'African wild dog, black rhino, elephant, cheetah, gerenuk, lesser kudu, oryx, lion, jackal',
    'Vast semi-arid savanna, Pare Mountains skyline, remote dry riverbeds and acacia woodlands',
    'Wild dog sightings are not guaranteed — early morning drives greatly increase chances. Mkomazi shares a boundary with Kenya''s Tsavo West, creating an enormous wildlife corridor.',
    'Mkomazi', 'Mkomazi', 0, 1, 'B,L,D', @now, @now);
SET @day_45_3 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_3, @park_mkomazi, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives, overnight at park campsite', @now);

-- Day 4
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 4, 'Day 4', 'Mkomazi to Tarangire — Cross-Country Drive',
    'A long but rewarding cross-country transfer takes you from remote Mkomazi to the elephant-rich Tarangire National Park. Arrive in the afternoon for your first game drive through iconic baobab landscapes.',
    'Early breakfast, pack up camp, and depart Mkomazi for the long drive west through Tanzania''s interior. Pass through Same and the Masai Steppe, stopping for roadside lunch at a local town.',
    'Arrive at Tarangire in the early afternoon and enter the park for a game drive along the Tarangire River. Elephants congregate in huge numbers at the river during the dry season.',
    'Settle into the budget campsite as the sun sets over the baobab-studded plains. Dinner at camp with the sounds of the African night all around.',
    'African elephant, lion, leopard, giraffe, zebra, wildebeest, buffalo, lesser kudu, fringe-eared oryx',
    'Ancient baobab tree landscapes, Tarangire River valley, sweeping Masai Steppe views on the drive',
    'The cross-country drive is approximately 4–5 hours. Fuel up in Same as options are limited en route. Tarangire is best explored with a full day — today is a taste of what''s to come.',
    'Mkomazi', 'Tarangire', 230, 1, 'B,L,D', @now, @now);
SET @day_45_4 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_4, @park_tarangire, 'SLEEP_OVER', 1, '14:00', NULL, 'Afternoon arrival and introductory game drive, overnight in park', @now);

-- Day 5
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 5, 'Day 5', 'Full Day Tarangire — Elephants & Baobabs',
    'A full day in Tarangire National Park delivers some of northern Tanzania''s most spectacular wildlife encounters, with massive elephant herds, giant baobab trees, and over 550 bird species. The Tarangire River is the lifeblood of the park, drawing all manner of wildlife throughout the day.',
    'Dawn game drive along the Tarangire River, watching elephant herds drink and bathe in the morning light. Search the ancient baobabs for leopard and python resting in the canopy.',
    'Picnic lunch under a baobab tree. Afternoon game drive through the southern swamp circuits where giraffe, zebra, and large buffalo herds roam open grasslands.',
    'Return to camp as the sun dips below the acacia horizon. Enjoy a sundowner and a hearty camp dinner, sharing stories of the day''s sightings.',
    'African elephant (largest herds in northern Tanzania), lion, leopard, giraffe, zebra, wildebeest, buffalo, fringe-eared oryx, lesser kudu, python, over 550 bird species',
    'Ancient baobab tree landscapes, Tarangire River valley with bathing elephants, swamp areas rich with birds and buffalo',
    'Tarangire is at its finest in the dry season when wildlife concentrates at the river. The park is less crowded than Serengeti, making it a great value destination on a backpacker budget.',
    'Tarangire', 'Tarangire', 0, 1, 'B,L,D', @now, @now);
SET @day_45_5 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_5, @park_tarangire, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day game drives along the Tarangire River, overnight in park', @now);

-- Day 6
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 6, 'Day 6', 'Tarangire to Lake Manyara — Tree-Climbing Lions',
    'Drive north from Tarangire to the dramatic escarpment setting of Lake Manyara National Park, celebrated worldwide for its unusual tree-climbing lions. A day spent here reveals one of Tanzania''s most diverse and compact parks.',
    'Morning departure from Tarangire after breakfast. Arrive at Lake Manyara by mid-morning and enter the park, beginning with a game drive through the dense groundwater forest at the park entrance.',
    'Afternoon game drive along the lakeshore flats where huge hippo pools, thousands of flamingos, and tree-climbing lions in yellow fever acacias delight at every turn.',
    'Exit the park and check in at a budget guesthouse on the escarpment rim above the lake. Dinner with panoramic views over the Rift Valley.',
    'Tree-climbing lion, elephant, hippo, flamingo, buffalo, giraffe, blue monkey, baboon, wildebeest, impala',
    'Rift Valley escarpment rising 600m above the lake, groundwater forest canopy, pink flamingo-fringed Lake Manyara shoreline',
    'Lake Manyara is a compact park best done as a half-day or full-day visit. Arrive early to maximise wildlife sightings before the midday heat. Tree-climbing lions are most commonly seen in the yellow fever acacia belt.',
    'Tarangire', 'Manyara', 70, 1, 'B,L,D', @now, @now);
SET @day_45_6 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_6, @park_manyara, 'DAY_TRIP', 1, '10:00', '17:00', 'Full day game drive through forest and lakeshore', @now);

-- Day 7
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 7, 'Day 7', 'Manyara to Ngorongoro — Crater Floor Game Drive',
    'Drive up into the highlands to the Ngorongoro Conservation Area and descend into the world''s largest intact volcanic caldera for a thrilling day of game viewing. The crater floor hosts one of the densest concentrations of wildlife in Africa, including the rare black rhino.',
    'Early morning departure from Manyara. Drive through lush highland forests to the Ngorongoro Crater rim, arriving at the viewpoint for spectacular panoramas before descending 600m to the crater floor.',
    'Game drive across the vast crater floor, visiting Lake Magadi for flamingos, searching the short-grass plains for black rhino, and watching lion prides laze in the midday sun near the marsh.',
    'Ascend to the crater rim in late afternoon. Overnight at a budget campsite on the rim with cool highland temperatures and starlit skies.',
    'Black rhino, lion, elephant, buffalo, hippo, flamingo, spotted hyena, zebra, wildebeest, golden jackal, serval',
    'Panoramic views from the 2,300m crater rim, Lerai Forest fig trees, Lake Magadi flamingo shores, vast crater floor stretching to the caldera walls',
    'Ngorongoro Crater has a daily vehicle limit — early arrival secures the best experience. Bring a warm fleece for the crater rim camp at night. Crater fees are higher than other parks, budget accordingly.',
    'Manyara', 'Ngorongoro', 60, 1, 'B,L,D', @now, @now);
SET @day_45_7 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_7, @park_ngorongoro, 'DAY_TRIP', 1, '09:00', '17:00', 'Full day crater floor game drive, overnight on rim', @now);

-- Day 8
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 8, 'Day 8', 'Ngorongoro to Serengeti — Entry & Afternoon Game Drive',
    'Drive through the Ngorongoro highlands and descend into the endless plains of the Serengeti — Tanzania''s most iconic national park and the stage for the greatest wildlife migration on Earth. Arrive in time for an afternoon game drive across the central Seronera Valley.',
    'Morning departure from the crater rim camp, driving west through the Ngorongoro highlands. Cross the boundary into the Serengeti and enjoy your first views of the vast short-grass plains stretching to the horizon.',
    'Afternoon game drive through the Seronera Valley, one of Africa''s most productive game-viewing areas. Search riverine fig forests for leopard and scan the open plains for lion prides and cheetah on termite mounds.',
    'Arrive at the budget campsite as the African dusk settles in. Campfire dinner with the distant calls of hyena and lion filling the Serengeti night.',
    'Lion, leopard, cheetah, elephant, giraffe, zebra, wildebeest, hippo, crocodile, topi, kongoni',
    'Endless Serengeti short-grass plains, Seronera River kopjes (granite outcrops), golden sunset over Africa''s most famous savanna',
    'The drive from Ngorongoro to Seronera takes approximately 3 hours. The Serengeti entrance gate provides maps and wildlife tracking boards — check recent sightings to plan your afternoon route.',
    'Ngorongoro', 'Serengeti', 145, 1, 'B,L,D', @now, @now);
SET @day_45_8 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_8, @park_serengeti, 'SLEEP_OVER', 1, '13:00', NULL, 'Afternoon arrival and game drive through Seronera Valley, overnight in park', @now);

-- Day 9
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 9, 'Day 9', 'Full Serengeti — The Great Migration',
    'A full day in the Serengeti chasing the Great Migration — one of nature''s most breathtaking spectacles as over a million wildebeest and hundreds of thousands of zebra and gazelle move across the plains. River crossings are heart-pounding highlights of the migration experience.',
    'Pre-dawn departure to position for the golden hour on the Serengeti plains. Track wildebeest herds with your driver-guide, looking for predator action as packs of hyena and lone cheetah target the moving columns.',
    'Seek out river crossing points on the Grumeti or Mara rivers depending on season. Picnic lunch in the field to maximise time with the migration. Afternoon game drive through wildebeest corridors and lion prides feeding on kills.',
    'Sundowner on a kopje overlooking the migration plains. Return to camp for a well-earned dinner and debrief of the day''s extraordinary wildlife encounters.',
    'Wildebeest (over 1 million), zebra, Thomson''s gazelle, Grant''s gazelle, lion, cheetah, leopard, spotted hyena, African wild dog, crocodile (at river crossings)',
    'Endless wildebeest columns stretching to the horizon, dramatic river crossing action, golden savanna light over Serengeti kopjes',
    'Migration timing varies by season — July to October brings crossings on the northern Mara River, while January to March features calving on the southern Ndutu plains. Your guide will know the current location of the herds.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_45_9 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_9, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day migration tracking and game drives, overnight in park', @now);

-- Day 10
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 10, 'Day 10', 'Full Serengeti — Kopjes, Predators & Sundowners',
    'A second full day in the Serengeti, this time focusing on the dramatic granite kopje formations that serve as dens and look-out posts for the park''s big cat population. These ancient rock islands rising from the plains are among Africa''s most photogenic landscapes.',
    'Early morning game drive to the Simba or Moru Kopjes, searching for lion prides basking on sun-warmed rocks and leopard hidden in crevices. Scan the surrounding plains for cheetah mothers teaching cubs to hunt.',
    'Afternoon exploration of further kopje circuits and open grassland zones where large mixed herds of zebra and wildebeest graze. Look for secretary birds, martial eagles, and other large raptors along the way.',
    'Classic Serengeti sundowner as the sun melts into the horizon behind an acacia silhouette. Final night in the Serengeti — savour every sound of the wild African night.',
    'Lion, leopard, cheetah, rock hyrax (on kopjes), mongoose, eagle, vulture, zebra, wildebeest, giraffe, elephant',
    'Moru Kopjes ancient rock formations, prehistoric Maasai rock paintings (at Moru), panoramic plains views from kopje summits',
    'The Moru Kopjes area contains ancient Maasai rock paintings — ask your guide to include this cultural highlight. Kopjes are also excellent for close-up photography of relaxed big cats.',
    'Serengeti', 'Serengeti', 0, 1, 'B,L,D', @now, @now);
SET @day_45_10 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_10, @park_serengeti, 'SLEEP_OVER', 1, '06:00', NULL, 'Full day kopje circuits and predator game drives, overnight in park', @now);

-- Day 11
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 11, 'Day 11', 'Serengeti to Ngorongoro Area — Maasai Village Visit',
    'Drive east from the Serengeti back towards the Ngorongoro highlands, breaking the journey with a meaningful visit to a traditional Maasai village. Experience the culture and customs of one of Africa''s most iconic peoples before settling in for a final highland night.',
    'Morning departure from the Serengeti after a last game drive on the way out. Drive east through the Ngorongoro Conservation Area''s open plains where Maasai cattle share the land with wild animals.',
    'Stop at an authentic Maasai boma (village) for a guided cultural visit — watch traditional dances, learn about herding life, and browse the local craft market for hand-made jewellery and beadwork souvenirs.',
    'Arrive at the Ngorongoro highland campsite and settle in for the night. Dinner with cool crisp air and views of the caldera under the stars.',
    'Maasai cattle alongside zebra and wildebeest on the NCA plains, elephant and buffalo near the highland forest border',
    'Ngorongoro Conservation Area open plains with Maasai herders, highland crater rim forest, starlit skies at 2,300m altitude',
    'Maasai village visits should be pre-arranged through your operator. A small community fee is paid directly to the village. Respect local customs and always ask permission before photographing people.',
    'Serengeti', 'Ngorongoro', 145, 1, 'B,L,D', @now, @now);
SET @day_45_11 = LAST_INSERT_ID();
INSERT INTO itinerary_day_parks (itinerary_day_id, park_id, entry_type, sort_order, arrival_time, departure_time, notes, created_at) VALUES (@day_45_11, @park_ngorongoro, 'SLEEP_OVER', 1, '12:00', NULL, 'Transit through NCA with Maasai village visit, overnight near crater rim', @now);

-- Day 12
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 12, 'Day 12', 'Ngorongoro to Arusha — Rest & Recovery',
    'Drive back to Arusha from the Ngorongoro highlands, descending from the cool crater rim through coffee and banana plantation country to the safari capital of Tanzania. Enjoy a well-earned rest day after eleven days of adventure.',
    'Leisurely breakfast at the highland camp with final views of the Ngorongoro Conservation Area. Morning drive back to Arusha through the Karatu and Mto wa Mbu corridor.',
    'Arrive in Arusha by early afternoon. Check in to a budget guesthouse, do laundry, recharge devices, and relax. Optional visit to the Cultural Heritage Centre for shopping or the Clock Tower area for local food.',
    'Dinner at a recommended local restaurant in Arusha. Share memories and photos from the bush with fellow travellers at the guesthouse common area.',
    'No specific wildlife — enjoy the human and agricultural landscapes of northern Tanzania on the drive back',
    'Ngorongoro escarpment highland forests, coffee and banana plantation valleys, Mt Meru skyline on approach to Arusha',
    'Use this rest day to sort gear, back up photos, and repack for the final stretch. Arusha has reliable internet, ATMs, and pharmacies for any last-minute needs.',
    'Ngorongoro', 'Arusha', 190, 1, 'B,L,D', @now, @now);
SET @day_45_12 = LAST_INSERT_ID();

-- Day 13
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 13, 'Day 13', 'Arusha City Tour & Maasai Market',
    'Spend your final full day discovering Arusha — the safari gateway city with a vibrant local culture, bustling markets, and a fascinating natural history museum. The famous Maasai market is a must-visit for authentic crafts and souvenirs at bargain prices.',
    'Morning visit to the Arusha Natural History Museum and the Boma (the old colonial German fort) in the town centre. Learn about the geological and human history of northern Tanzania''s extraordinary landscape.',
    'Afternoon browse of the Maasai market for hand-crafted jewellery, textiles, carvings, and beadwork. Bargaining is expected and part of the experience — be respectful and enjoy the lively interaction.',
    'Farewell dinner at one of Arusha''s popular local restaurants, celebrating fourteen days of unforgettable safari adventure. Pack bags and prepare for tomorrow''s early departure.',
    'No wildlife — urban and cultural exploration day',
    'Mount Meru views from the city, colonial-era architecture in the town centre, colourful Maasai market stalls',
    'Confirm your departure transfer and flight details this evening. Arusha''s JRO (Kilimanjaro International Airport) is 45 minutes from the city centre — allow extra time for early morning check-ins.',
    'Arusha', 'Arusha', 0, 1, 'B,L,D', @now, @now);
SET @day_45_13 = LAST_INSERT_ID();

-- Day 14
INSERT INTO itinerary_days (itinerary_id, day_number, day_tag, title, description, morning_activities, afternoon_activities, evening_activities, wildlife_highlights, scenic_highlights, special_notes, start_location, end_location, distance_km, is_overnight, meals_included, created_at, updated_at)
VALUES (@itin_45, 14, 'Day 14', 'Departure Day — Farewell Tanzania',
    'Savour a final breakfast in Arusha before your transfer to Kilimanjaro International Airport and onward journey home. Your backpacker''s Tanzania Discovery has come to an end, but the memories of fourteen extraordinary days will last a lifetime.',
    'Early breakfast at the guesthouse and final bag check. Transfer to Kilimanjaro International Airport (JRO) approximately 45 minutes from Arusha city centre.',
    'Airport check-in and departure formalities. Duty-free shopping for last-minute gifts and Tanzanian coffee.',
    'No evening activities — departure day.',
    'No wildlife',
    'Final views of Mount Kilimanjaro and Mount Meru from the airport on clear mornings',
    'Kilimanjaro International Airport (JRO) serves major international carriers. Allow at least 3 hours before international departure for check-in queues. Keep USD cash for any last-minute airport purchases.',
    'Arusha', 'Kilimanjaro Airport', 45, 0, 'B', @now, @now);
SET @day_45_14 = LAST_INSERT_ID();
