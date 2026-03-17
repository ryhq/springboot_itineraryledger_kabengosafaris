-- =============================================
-- Kabengo Safaris — Seed 46 Itinerary Packages
-- Northern Tanzania, Kilimanjaro & Zanzibar
-- =============================================

SET @now = NOW();

-- =============================================
-- PARK ID VARIABLES
-- =============================================
SET @park_serengeti = 1;
SET @park_ngorongoro = 2;
SET @park_tarangire = 3;
SET @park_manyara = 4;
SET @park_arusha = 9;
SET @park_kilimanjaro = 12;
SET @park_mkomazi = 14;

-- Jozani Chwaka Bay NP assumed to already exist
SET @park_jozani = 27;

-- =============================================
-- SECTION 2: Insert 46 Itineraries (original + 45 new)
-- =============================================

-- ---- ORIGINAL ITINERARY ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Tanzania Safari & Zanzibar Beach Holiday', 'PUBLISHED', 'PRIVATE', 'MID_RANGE', 16, 15, 1,
    'A comprehensive 15-night safari combining Tanzania''s northern circuit parks with cultural experiences and Zanzibar beach relaxation',
    'Great Migration & Mara River Crossing, Ngorongoro Crater - World''s largest intact caldera, Tarangire elephant herds, Hadzabe & Datoga cultural visit, Lake Natron & Ol Doinyo Lengai, Zanzibar beach holiday',
    'Kilimanjaro Airport', 'Zanzibar Airport', 1, 1, @now, @now);
SET @itin_0 = LAST_INSERT_ID();

-- ---- DAY TRIPS (8) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Ngorongoro Crater Discovery', 'PUBLISHED', 'GROUP', 'MID_RANGE', 1, 0, 1,
    'Descend into the world''s largest intact volcanic caldera for a full day of game viewing among the densest concentration of wildlife in Africa. Spot the Big Five, flamingo-lined lakes, and panoramic crater rim views — all in a single unforgettable day from Arusha.',
    '["Ngorongoro Crater floor game drive", "Big Five wildlife viewing", "Flamingo-lined Lake Magadi", "Panoramic crater rim views", "Lerai Forest exploration"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_1 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Tarangire Baobab & Elephant Safari', 'PUBLISHED', 'GROUP', 'MID_RANGE', 1, 0, 1,
    'Journey into Tarangire National Park, famous for its ancient baobab trees and the largest elephant herds in northern Tanzania. Watch hundreds of elephants gather along the Tarangire River while tree-climbing lions rest in the branches above.',
    '["Massive elephant herds along Tarangire River", "Ancient baobab tree landscapes", "Tree-climbing lions", "Over 550 bird species", "Seasonal wildlife migration"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_2 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Lake Manyara Flamingo Safari', 'PUBLISHED', 'GROUP', 'BUDGET', 1, 0, 1,
    'Explore the diverse ecosystems of Lake Manyara National Park in a single action-packed day. From the alkaline lake shore teeming with flamingos to the groundwater forest alive with primates, this compact park delivers extraordinary wildlife encounters.',
    '["Flamingo flocks on Lake Manyara", "Tree-climbing lions", "Baboon troops in groundwater forest", "Hippo pools", "Spectacular Rift Valley escarpment views"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_3 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Arusha Wilderness Family Escape', 'PUBLISHED', 'FAMILY', 'BUDGET', 1, 0, 1,
    'A perfect family-friendly day trip to Arusha National Park, where Mount Meru towers over crater lakes, lush forests, and open meadows. Kids will love spotting giraffes, zebras, and colobus monkeys just 45 minutes from Arusha city.',
    '["Mount Meru views", "Ngurdoto Crater viewpoint", "Momella Lakes with flamingos", "Colobus monkey encounters", "Canopy walkway adventure"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_4 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Rainforest Trek', 'PUBLISHED', 'ADVENTURE', 'BUDGET', 1, 0, 1,
    'Trek through the lush montane rainforest of Africa''s highest peak without committing to a full summit climb. Walk among giant ferns, ancient trees draped in moss, and spot blue monkeys and exotic birds on this refreshing day hike.',
    '["Kilimanjaro rainforest zone trek", "Blue monkey and colobus sightings", "Marangu Gate to Mandara Hut trail", "Waterfall visit", "Mount Kilimanjaro views"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_5 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Ngorongoro Crater Through the Lens', 'PUBLISHED', 'PHOTOGRAPHY', 'LUXURY', 1, 0, 1,
    'A premium photography-focused safari into the Ngorongoro Crater with extended game drives timed for golden hour lighting. Your guide positions you for the best angles on black rhino, lion prides, and the dramatic crater landscape.',
    '["Golden hour crater photography", "Black rhino close encounters", "Lion pride portraits", "Flamingo lake reflections", "Dramatic caldera landscape shots"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_6 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Mkomazi Rhino & Wilderness Safari', 'PUBLISHED', 'PRIVATE', 'MID_RANGE', 1, 0, 1,
    'Venture off the beaten path to Mkomazi National Park, home to the Mkomazi Rhino Sanctuary and a wild landscape untouched by mass tourism. Spot African wild dogs, lesser kudu, and enjoy the dramatic backdrop of the Pare and Usambara Mountains.',
    '["Mkomazi Rhino Sanctuary visit", "African wild dog sightings", "Lesser kudu and gerenuk", "Pare Mountains backdrop", "Off-the-beaten-path wilderness"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_7 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Jozani Forest & Spice Island Explorer', 'PUBLISHED', 'FAMILY', 'MID_RANGE', 1, 0, 1,
    'Discover Zanzibar''s wild side at Jozani Forest, home to the rare red colobus monkey found nowhere else on Earth. Walk the mangrove boardwalk, visit a spice plantation, and soak in the island''s rich cultural heritage.',
    '["Zanzibar red colobus monkey encounter", "Mangrove boardwalk walk", "Spice plantation tour", "Tropical forest exploration", "Zanzibar cultural experience"]',
    'Zanzibar', 'Zanzibar', 1, 1, @now, @now);
SET @itin_8 = LAST_INSERT_ID();

-- ---- 2-DAY SAFARIS (5) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Tarangire & Ngorongoro Express Safari', 'PUBLISHED', 'GROUP', 'MID_RANGE', 2, 1, 1,
    'Experience two of northern Tanzania''s greatest parks in a fast-paced two-day adventure. Day one among Tarangire''s elephants and baobabs, then descend into the Ngorongoro Crater for a morning of Big Five game viewing before returning to Arusha.',
    '["Tarangire elephant herds", "Ancient baobab landscapes", "Ngorongoro Crater descent", "Big Five game viewing", "Two iconic parks in two days"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_9 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Lake Manyara & Ngorongoro Luxury Retreat', 'PUBLISHED', 'PRIVATE', 'LUXURY', 2, 1, 1,
    'A refined two-day escape combining Lake Manyara''s tree-climbing lions and flamingo shores with a full morning exploring the Ngorongoro Crater floor. Luxury lodging on the crater rim with sunset views over the caldera.',
    '["Lake Manyara tree-climbing lions", "Ngorongoro Crater floor exploration", "Crater rim sunset views", "Luxury crater rim lodge", "Flamingo-lined alkaline lake"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_10 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Serengeti Fly-In Exclusive Safari', 'PUBLISHED', 'PRIVATE', 'ULTRA_LUXURY', 2, 1, 1,
    'Skip the long drive and fly directly into the heart of the Serengeti for an exclusive two-day safari. Enjoy morning and afternoon game drives across the endless plains with top-tier guiding and luxury tented camp accommodation.',
    '["Scenic bush flight over Serengeti", "Exclusive game drives on endless plains", "Big cat encounters", "Luxury tented camp experience", "Wildebeest and zebra herds"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_11 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Arusha & Tarangire Family Wildlife Safari', 'PUBLISHED', 'FAMILY', 'MID_RANGE', 2, 1, 1,
    'A gentle two-day family safari starting with kid-friendly Arusha National Park — home to giraffes, colobus monkeys, and the Momella Lakes — before heading to Tarangire for elephants and baobabs on day two.',
    '["Arusha NP colobus monkeys", "Momella Lakes flamingos", "Tarangire elephant herds", "Baobab tree landscapes", "Family-friendly game drives"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_12 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Foothills & Arusha Adventure', 'PUBLISHED', 'ADVENTURE', 'BUDGET', 2, 1, 1,
    'Combine a Kilimanjaro rainforest day hike with a wildlife safari in Arusha National Park. Trek to Mandara Hut through misty montane forest, then explore crater lakes and open meadows teeming with wildlife at the foot of Mount Meru.',
    '["Kilimanjaro rainforest trek", "Mandara Hut trail", "Arusha NP crater lakes", "Mount Meru views", "Blue monkey sightings"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_13 = LAST_INSERT_ID();

-- ---- 3-DAY SAFARIS (6) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Serengeti Express — Endless Plains Safari', 'PUBLISHED', 'GROUP', 'MID_RANGE', 3, 2, 1,
    'Reach the legendary Serengeti in just three days. Drive through Lake Manyara and across the Ngorongoro highlands, arriving on the Serengeti plains for afternoon and morning game drives before returning via the crater.',
    '["Serengeti endless plains game drives", "Big cat territory exploration", "Ngorongoro highlands transit views", "Wildebeest and zebra herds", "Seronera Valley wildlife"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_14 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Ngorongoro, Tarangire & Manyara Highlights', 'PUBLISHED', 'PRIVATE', 'LUXURY', 3, 2, 1,
    'Three days exploring the jewels of northern Tanzania with luxury lodging. Tarangire''s elephants, Ngorongoro''s crater, and Lake Manyara''s diverse ecosystems — each park offering a completely different safari experience.',
    '["Tarangire elephant herds and baobabs", "Ngorongoro Crater Big Five", "Lake Manyara tree-climbing lions", "Luxury safari lodges", "Three distinct ecosystems"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_15 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Serengeti Wildlife Photography Expedition', 'PUBLISHED', 'PHOTOGRAPHY', 'LUXURY', 3, 2, 1,
    'Three days dedicated to capturing the Serengeti''s wildlife. Extended game drives timed for golden hour, strategic positioning for big cat hunts, and two full days in the Seronera Valley — the predator capital of Africa.',
    '["Golden hour big cat photography", "Seronera Valley predator action", "Kopje landscapes with wildlife", "River crossing opportunities", "Professional photography guidance"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_16 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Northern Circuit Budget Explorer', 'PUBLISHED', 'GROUP', 'BUDGET', 3, 2, 1,
    'See the best of northern Tanzania without breaking the bank. Visit Lake Manyara, descend into the Ngorongoro Crater, and explore Tarangire — three iconic parks with comfortable budget camping and group game drives.',
    '["Three parks in three days", "Ngorongoro Crater descent", "Tarangire elephants", "Lake Manyara flamingos", "Budget-friendly camping safari"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_17 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Machame — Shira Plateau Trek', 'PUBLISHED', 'ADVENTURE', 'MID_RANGE', 3, 2, 1,
    'A three-day taste of Kilimanjaro via the scenic Machame Route. Trek through rainforest to moorland, reaching the stunning Shira Plateau at 3,800m with breathtaking views of Kibo peak before descending.',
    '["Machame Route rainforest trek", "Shira Plateau panoramic views", "Kibo peak vistas", "Moorland ecosystem crossing", "High-altitude adventure"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_18 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Family Safari — Arusha, Tarangire & Manyara', 'PUBLISHED', 'FAMILY', 'MID_RANGE', 3, 2, 1,
    'A gentle three-day family safari through three parks perfect for children. Start at Arusha NP with its playful colobus monkeys, then marvel at Tarangire''s giants and finish at Lake Manyara''s flamingo-pink shores.',
    '["Kid-friendly game drives", "Colobus monkey encounters", "Elephant herd sightings", "Lake Manyara flamingos", "Three parks, three ecosystems"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_19 = LAST_INSERT_ID();

-- ---- 4-DAY SAFARIS (4) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Northern Circuit Classic Safari', 'PUBLISHED', 'GROUP', 'MID_RANGE', 4, 3, 1,
    'The quintessential northern Tanzania safari covering Lake Manyara, Ngorongoro Crater, and the Serengeti. Four days of world-class game viewing across three legendary destinations with comfortable mid-range accommodation.',
    '["Lake Manyara game drive", "Ngorongoro Crater descent", "Serengeti plains exploration", "Big Five encounters", "Classic northern circuit route"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_20 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Serengeti Great Migration Safari', 'PUBLISHED', 'PRIVATE', 'LUXURY', 4, 3, 1,
    'Witness the greatest wildlife spectacle on Earth — the Great Migration. Two nights in the Serengeti give you time to track the massive herds, witness predator-prey drama, and explore the Ngorongoro Crater on the way.',
    '["Great Migration herds", "Serengeti predator encounters", "Ngorongoro Crater Big Five", "Two nights in the Serengeti", "Luxury tented camps"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_21 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Romantic Crater & Serengeti Escape', 'PUBLISHED', 'HONEYMOON', 'ULTRA_LUXURY', 4, 3, 1,
    'An intimate four-day escape designed for couples. Private game drives in the Ngorongoro Crater, sundowner cocktails on the Serengeti plains, and ultra-luxury lodges with romantic touches — the perfect honeymoon safari.',
    '["Private crater game drive", "Serengeti sundowner cocktails", "Ultra-luxury lodges", "Romantic bush dinners", "Exclusive couple''s experience"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_22 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Northern Tanzania Backpacker Safari', 'PUBLISHED', 'GROUP', 'BACKPACKER', 4, 3, 1,
    'The ultimate budget safari covering Lake Manyara, Ngorongoro Crater, and Tarangire. Travel with fellow adventurers, camp under the African stars, and experience world-class wildlife at backpacker-friendly prices.',
    '["Three parks on a budget", "Camping under African stars", "Ngorongoro Crater descent", "Tarangire elephant encounters", "Group adventure experience"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_23 = LAST_INSERT_ID();

-- ---- 5-DAY SAFARIS (5) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Northern Circuit Grand Safari', 'PUBLISHED', 'PRIVATE', 'LUXURY', 5, 4, 1,
    'The definitive northern Tanzania safari spanning five days across four iconic parks. From Tarangire''s elephants to Manyara''s flamingos, the Ngorongoro Crater, and the vast Serengeti plains — every highlight of the northern circuit.',
    '["Four iconic parks in five days", "Tarangire elephant herds", "Ngorongoro Crater floor", "Serengeti endless plains", "Luxury lodge accommodation"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_24 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Serengeti & Ngorongoro Romantic Getaway', 'PUBLISHED', 'HONEYMOON', 'ULTRA_LUXURY', 5, 4, 1,
    'Five days of romance in Africa''s most spectacular settings. Three nights in the Serengeti with private sundowners and bush dinners, plus the awe of the Ngorongoro Crater — designed for couples seeking an unforgettable honeymoon.',
    '["Three nights in Serengeti", "Private sundowner experiences", "Ngorongoro Crater exploration", "Bush dinner under the stars", "Ultra-luxury romantic lodges"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_25 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Big Five Photography Safari', 'PUBLISHED', 'PHOTOGRAPHY', 'LUXURY', 5, 4, 1,
    'Five days dedicated to photographing the Big Five across Tarangire, Ngorongoro, and the Serengeti. Strategic positioning, golden hour drives, and expert guidance to capture Africa''s most iconic wildlife.',
    '["Big Five photography across three parks", "Tarangire elephant close-ups", "Ngorongoro rhino tracking", "Serengeti big cat portraits", "Golden hour game drives"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_26 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Marangu — Roof of Africa Trek', 'PUBLISHED', 'ADVENTURE', 'MID_RANGE', 5, 4, 1,
    'Summit Africa''s highest peak via the classic Marangu Route — the only path with hut accommodation. Trek through five distinct climate zones from tropical rainforest to arctic glaciers, reaching Uhuru Peak at 5,895m.',
    '["Uhuru Peak summit attempt", "Five climate zones", "Marangu Route hut accommodation", "Kibo Hut to summit sunrise trek", "Kilimanjaro glaciers"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_27 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Northern Tanzania Budget Explorer', 'PUBLISHED', 'GROUP', 'BUDGET', 5, 4, 1,
    'Five days covering four parks at budget-friendly prices. Lake Manyara, the Ngorongoro Crater, the Serengeti, and Tarangire — all the highlights of northern Tanzania with group game drives and comfortable camping.',
    '["Four parks in five days", "Ngorongoro Crater descent", "Serengeti game drives", "Tarangire elephants", "Budget camping safari"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_28 = LAST_INSERT_ID();

-- ---- 6-DAY SAFARIS (3) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Machame — Whiskey Route Trek', 'PUBLISHED', 'ADVENTURE', 'MID_RANGE', 6, 5, 1,
    'Conquer Kilimanjaro via the spectacular Machame Route, known as the "Whiskey Route" for its challenging terrain. Six days through rainforest, moorland, alpine desert, and glaciers to Uhuru Peak — Africa''s rooftop.',
    '["Uhuru Peak summit via Machame", "Barranco Wall scramble", "Shira Plateau views", "Five climate zones", "Sunrise summit from Stella Point"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_29 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Ultimate Northern Circuit Safari', 'PUBLISHED', 'PRIVATE', 'LUXURY', 6, 5, 1,
    'Six days exploring every gem of the northern circuit. Start at Arusha NP, then Tarangire, Lake Manyara, the Ngorongoro Crater, and finish with two days in the Serengeti. The most complete northern Tanzania safari experience.',
    '["Five parks in six days", "Arusha NP Mount Meru views", "Tarangire elephant herds", "Ngorongoro Crater descent", "Two days in Serengeti"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_30 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Family Northern Tanzania Safari Adventure', 'PUBLISHED', 'FAMILY', 'MID_RANGE', 6, 5, 1,
    'Six days of family adventure across four parks with age-appropriate game drives and plenty of wonder. From Arusha''s colobus monkeys to Tarangire''s elephants, the Ngorongoro Crater, and the Serengeti plains.',
    '["Family-friendly game drives", "Arusha NP nature walks", "Tarangire elephant encounters", "Ngorongoro Crater exploration", "Serengeti wildlife spectacle"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_31 = LAST_INSERT_ID();

-- ---- 7-DAY SAFARIS (4) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Bush to Beach — Northern Circuit & Zanzibar', 'PUBLISHED', 'PRIVATE', 'LUXURY', 7, 6, 1,
    'The perfect combination of bush and beach. Four days exploring the Ngorongoro Crater and Serengeti, then fly to Zanzibar for three days of turquoise waters, spice tours, and the enchanting Stone Town.',
    '["Ngorongoro Crater game drive", "Serengeti big cat encounters", "Zanzibar beach relaxation", "Stone Town exploration", "Jozani Forest red colobus monkeys"]',
    'Arusha', 'Zanzibar', 1, 1, @now, @now);
SET @itin_32 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Lemosho — Summit the Roof of Africa', 'PUBLISHED', 'ADVENTURE', 'MID_RANGE', 7, 6, 1,
    'The Lemosho Route is considered the most scenic path to Kilimanjaro''s summit. Seven days through pristine rainforest, the Shira Plateau, the dramatic Barranco Wall, and finally the glaciated summit of Uhuru Peak.',
    '["Lemosho Route scenic trek", "Shira Plateau camping", "Barranco Wall adventure", "Uhuru Peak summit", "Five distinct climate zones"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_33 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Great Migration Photography Expedition', 'PUBLISHED', 'PHOTOGRAPHY', 'ULTRA_LUXURY', 7, 6, 1,
    'Seven days dedicated to documenting the Great Migration across Tarangire, Ngorongoro, and four nights in the Serengeti. Extended game drives, strategic positioning for river crossings, and luxury camps in prime migration territory.',
    '["Four nights in Serengeti migration zones", "River crossing photography", "Tarangire baobab landscapes", "Ngorongoro Crater wildlife portraits", "Professional photography support"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_34 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Tanzania Backpacker Grand Tour', 'PUBLISHED', 'GROUP', 'BACKPACKER', 7, 6, 1,
    'Seven days covering five parks — the most comprehensive budget safari in northern Tanzania. Lake Manyara, Ngorongoro, Serengeti, Tarangire, and the remote Mkomazi — all with group camping and shared game drives.',
    '["Five parks in seven days", "Mkomazi off-the-beaten-path", "Serengeti camping safari", "Ngorongoro Crater descent", "Budget-friendly adventure"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_35 = LAST_INSERT_ID();

-- ---- 8-10 DAY SAFARIS (5) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Safari & Zanzibar Beach Honeymoon', 'PUBLISHED', 'HONEYMOON', 'LUXURY', 8, 7, 1,
    'Eight days blending wildlife romance with tropical paradise. Tarangire''s elephants, the Ngorongoro Crater, and the Serengeti plains followed by Zanzibar''s white-sand beaches and candlelit dinners by the Indian Ocean.',
    '["Tarangire to Serengeti safari", "Ngorongoro Crater romance", "Zanzibar beach relaxation", "Stone Town sunset dhow cruise", "Luxury honeymoon lodges"]',
    'Arusha', 'Zanzibar', 1, 1, @now, @now);
SET @itin_36 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Summit & Crater Safari Combo', 'PUBLISHED', 'ADVENTURE', 'MID_RANGE', 8, 7, 1,
    'Summit Africa''s highest peak via the Lemosho Route, then celebrate with a game drive in the Ngorongoro Crater. Eight days of mountain adventure followed by the reward of the world''s most spectacular wildlife caldera.',
    '["Kilimanjaro Lemosho summit", "Uhuru Peak achievement", "Ngorongoro Crater celebration drive", "Five climate zones", "Mountain and wildlife combo"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_37 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Ultimate Tanzania — Safari & Spice Island', 'PUBLISHED', 'PRIVATE', 'ULTRA_LUXURY', 9, 8, 1,
    'Nine days of the finest Tanzania has to offer. Tarangire, Lake Manyara, Ngorongoro Crater, and the Serengeti — then fly to Zanzibar for spice tours, snorkeling, and beach bliss. Ultra-luxury throughout.',
    '["Four mainland parks", "Serengeti exclusive game drives", "Ngorongoro Crater descent", "Zanzibar beach and culture", "Ultra-luxury lodges throughout"]',
    'Arusha', 'Zanzibar', 1, 1, @now, @now);
SET @itin_38 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Northern Tanzania & Zanzibar Grand Tour', 'PUBLISHED', 'PRIVATE', 'LUXURY', 10, 9, 1,
    'Ten days spanning six destinations — the most comprehensive northern Tanzania and Zanzibar experience. Arusha NP, Tarangire, Lake Manyara, Ngorongoro, Serengeti, and Zanzibar island life.',
    '["Six destinations in ten days", "Arusha to Serengeti safari", "Ngorongoro Crater exploration", "Zanzibar beach and culture", "Complete Tanzania experience"]',
    'Arusha', 'Zanzibar', 1, 1, @now, @now);
SET @itin_39 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Family Safari & Zanzibar Beach Holiday', 'PUBLISHED', 'FAMILY', 'MID_RANGE', 10, 9, 1,
    'Ten days of family adventure combining northern Tanzania''s best wildlife parks with Zanzibar''s beaches. Tarangire''s elephants, the Ngorongoro Crater, Serengeti game drives, then tropical island fun for the whole family.',
    '["Family-friendly game drives", "Tarangire elephants", "Ngorongoro Crater adventure", "Serengeti wildlife", "Zanzibar beach family time"]',
    'Arusha', 'Zanzibar', 1, 1, @now, @now);
SET @itin_40 = LAST_INSERT_ID();

-- ---- 12-14 DAY SAFARIS (5) ----

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Kilimanjaro Summit & Northern Circuit Safari', 'PUBLISHED', 'ADVENTURE', 'LUXURY', 12, 11, 1,
    'The ultimate adventure — summit Kilimanjaro via the Lemosho Route, then embark on a luxury northern circuit safari. Seven days on the mountain followed by five days exploring Tarangire, Ngorongoro, and the Serengeti.',
    '["Kilimanjaro Uhuru Peak summit", "Lemosho Route scenic trek", "Tarangire post-climb safari", "Ngorongoro Crater descent", "Serengeti game drives"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_41 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Ultimate Honeymoon — Safari, Crater & Zanzibar', 'PUBLISHED', 'HONEYMOON', 'ULTRA_LUXURY', 12, 11, 1,
    'Twelve days of pure romance across Tanzania''s finest destinations. The Ngorongoro Crater, extended Serengeti exploration, and a Zanzibar beach finale — ultra-luxury lodges, private dinners, and unforgettable couple''s experiences.',
    '["Ngorongoro Crater romance", "Five nights in Serengeti", "Private bush dinners", "Zanzibar beach paradise", "Ultra-luxury honeymoon lodges"]',
    'Arusha', 'Zanzibar', 1, 1, @now, @now);
SET @itin_42 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Complete Northern Tanzania & Zanzibar Experience', 'PUBLISHED', 'PRIVATE', 'LUXURY', 14, 13, 1,
    'Fourteen days covering every highlight of northern Tanzania and Zanzibar. From Arusha NP to remote Mkomazi, through Tarangire, Manyara, Ngorongoro, and the vast Serengeti — capped with Zanzibar''s tropical paradise.',
    '["Seven destinations in fourteen days", "Mkomazi rhino sanctuary", "Complete northern circuit", "Extended Serengeti exploration", "Zanzibar beach finale"]',
    'Arusha', 'Zanzibar', 1, 1, @now, @now);
SET @itin_43 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Tanzania Wildlife Photography Grand Tour', 'PUBLISHED', 'PHOTOGRAPHY', 'ULTRA_LUXURY', 14, 13, 1,
    'Fourteen days capturing Tanzania''s greatest wildlife moments. Six parks, extended Serengeti sessions, Kilimanjaro landscapes, and every photographic opportunity the northern circuit offers — with ultra-luxury camps throughout.',
    '["Six parks for photography", "Extended Serengeti sessions", "Kilimanjaro landscape photography", "Ngorongoro Crater wildlife", "Professional photography support"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_44 = LAST_INSERT_ID();

INSERT INTO itineraries (name, status, trip_type, budget_category, total_days, total_nights, car_count, description, highlights, start_location, end_location, is_active, created_by, created_at, updated_at)
VALUES ('Tanzania Discovery — Backpacker''s Dream', 'PUBLISHED', 'GROUP', 'BACKPACKER', 13, 12, 1,
    'Thirteen days of pure adventure across six parks on a backpacker budget. From Arusha NP to remote Mkomazi, through Tarangire, Manyara, Ngorongoro, and the Serengeti — the most complete budget safari in Tanzania.',
    '["Six parks in thirteen days", "Mkomazi wilderness experience", "Complete northern circuit", "Serengeti camping", "Ultimate budget adventure"]',
    'Arusha', 'Arusha', 1, 1, @now, @now);
SET @itin_45 = LAST_INSERT_ID();

-- =============================================
-- Batch-update itinerary codes
-- =============================================
UPDATE itineraries SET code = CONCAT('ITI-', total_days, 'D', total_nights, 'N-', LPAD(1000 + id, 4, '0'))
WHERE code IS NULL;
