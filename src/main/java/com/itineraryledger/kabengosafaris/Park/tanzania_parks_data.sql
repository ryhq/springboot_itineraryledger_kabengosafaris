-- =====================================================================
-- TANZANIA PARKS DATABASE INSERT STATEMENTS
-- =====================================================================
-- This SQL file contains INSERT statements for Tanzania's National Parks,
-- Conservation Areas, Wildlife Reserves, and Marine Parks.
-- Data compiled from official sources and research conducted on 2025-12-30
--
-- Parks are organized by region:
-- 1. NORTHERN CIRCUIT (Most popular safari destinations)
-- 2. SOUTHERN CIRCUIT (Remote and wild)
-- 3. WESTERN CIRCUIT (Chimpanzee and lake destinations)
-- 4. COASTAL & MARINE PARKS
-- 5. CENTRAL CIRCUIT
--
-- Sources:
-- - Tanzania National Parks Authority (TANAPA)
-- - UNESCO World Heritage Sites
-- - Wildlife Conservation Society of Tanzania
-- =====================================================================

-- =====================================================================
-- NORTHERN CIRCUIT PARKS
-- =====================================================================

-- 1. Serengeti National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Serengeti National Park',
    'serengeti-national-park',
    'NATIONAL_PARK',
    'Mara and Simiyu',
    'Serengeti',
    'Northern Tanzania, 335 km northwest of Arusha',
    -2.3333,
    34.8333,
    '920-1,850 meters',
    '14,763 km²',
    'One of the most famous wildlife sanctuaries in the world, known for the annual Great Migration of over 1.5 million wildebeest and 250,000 zebras.',
    'Serengeti National Park is a vast ecosystem in north-central Tanzania. Established in 1951, it covers 14,763 square kilometers of grassland plains, savanna, riverine forest, and woodlands. The park is divided into three regions: Serengeti Plains, Western Corridor, and Northern Serengeti. It borders the Maasai Mara National Reserve in Kenya to the north. The park is home to the largest lion population in Africa and hosts the spectacular Great Migration, one of the most impressive natural events on Earth. The name "Serengeti" comes from the Maasai language and means "endless plains".',
    'Established in 1951 and designated as a UNESCO World Heritage Site in 1981. The area was first protected as a game reserve in 1929 before being upgraded to national park status.',
    'Grassland savanna, acacia woodlands, riverine forests, and open plains. The ecosystem supports diverse habitats from short grass plains to kopjes (rocky outcrops).',
    'Home to the Big Five (lion, leopard, elephant, buffalo, rhinoceros) and hosts the Great Migration of over 2 million wildebeest, zebras, and gazelles. The park has Africa''s largest lion population with over 3,000 lions. Other species include cheetahs, hyenas, giraffes, hippos, crocodiles, and over 500 bird species.',
    'Grassland plains dominate with scattered acacia trees, particularly umbrella acacias. Western corridor features riverine forests along the Grumeti River. Kopjes support unique plant communities.',
    '/images/parks/serengeti.jpg',
    'June to October (dry season for wildlife viewing), January to February (calving season)',
    '6:00 AM - 6:00 PM daily',
    'Accessible by road from Arusha (8-9 hours) or by air to Seronera, Kogatende, or Lobo airstrips. Multiple entry gates including Naabi Hill, Ndabaka, and Ikoma.',
    'Big Five, Great Migration, Safari, Wildlife, UNESCO World Heritage, Lion, Wildebeest, Zebra, Kopjes, Savanna',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 2. Ngorongoro Conservation Area
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Ngorongoro Conservation Area',
    'ngorongoro-conservation-area',
    'CONSERVATION_AREA',
    'Arusha',
    'Ngorongoro',
    'Crater Highlands, 180 km west of Arusha City',
    -3.2000,
    35.4500,
    '1,030-3,648 meters (Loolmalasin peak)',
    '8,292 km²',
    'Home to the world''s largest intact volcanic caldera and one of Africa''s most spectacular wildlife viewing areas with the highest density of predators.',
    'The Ngorongoro Conservation Area is a protected area and UNESCO World Heritage Site located in the Crater Highlands of northeastern Tanzania. The area is named after Ngorongoro Crater, a large volcanic caldera formed when a giant volcano exploded and collapsed 2-3 million years ago. The crater floor, 600 meters deep and covering 260 square kilometers, is home to approximately 25,000 large animals. The conservation area also includes Olduvai Gorge, one of the most important paleoanthropological sites in the world. Unlike national parks, the Ngorongoro Conservation Area allows human habitation, with Maasai pastoralists living alongside wildlife.',
    'Designated as a UNESCO World Heritage Site in 1979 and as an International Biosphere Reserve in 1981. The area was originally part of Serengeti National Park until it was separated in 1959 to create a multiple land use area.',
    'Highland plains, savanna, savanna woodlands, forests, and the volcanic crater ecosystem. Includes soda lakes, swamps, and montane forests on crater rims.',
    'The crater hosts approximately 25,000 large mammals including the Big Five. It has the densest known population of lions in the world. The area is one of the few places in Africa where you can see the endangered black rhinoceros. Other species include elephants, buffaloes, hippos, wildebeest, zebras, elands, gazelles, and over 500 bird species including flamingos on Lake Magadi.',
    'Ranges from short grass plains on the crater floor to montane forests on the crater rim. Includes fever tree forests, highland grasslands, and diverse plant communities supporting various ecological niches.',
    '/images/parks/ngorongoro.jpg',
    'June to October (dry season), December to March (green season)',
    '6:00 AM - 6:00 PM daily',
    'Accessible by road from Arusha (3-4 hours). Main entrance at Lodoare Gate. Can be combined with Serengeti visits via Naabi Hill Gate.',
    'Crater, UNESCO World Heritage, Big Five, Black Rhino, Maasai Culture, Olduvai Gorge, Volcanic Caldera, Wildlife Dense',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 3. Tarangire National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Tarangire National Park',
    'tarangire-national-park',
    'NATIONAL_PARK',
    'Manyara',
    'Babati',
    'Northern Tanzania, 118 km southwest of Arusha',
    -3.8333,
    36.0000,
    '900-1,250 meters',
    '2,850 km²',
    'Famous for its huge elephant population, iconic baobab trees, and spectacular concentration of wildlife during the dry season.',
    'Tarangire National Park is Tanzania''s sixth-largest national park, covering 2,850 square kilometers. Named after the Tarangire River that flows through the park, it serves as a vital water source during the dry season, attracting massive concentrations of wildlife. The park is renowned for having one of the highest elephant populations in Tanzania, with herds of up to 300 elephants common during the dry season. Ancient baobab trees dot the landscape, some over 1,000 years old. The park also features diverse habitats including swamps, grasslands, and acacia woodlands. During the dry season (June-October), Tarangire has wildlife densities second only to Ngorongoro Crater.',
    'Established as a national park in 1970. The area has been important to wildlife migration patterns for millennia, particularly for elephants moving between seasonal water sources.',
    'Acacia woodlands, riverine forests, seasonal swamps, open grasslands, and ancient baobab-studded landscapes. The Tarangire River and Silale Swamp are key ecosystem features.',
    'Home to large elephant herds (up to 3,000 individuals), lions, leopards, cheetahs, and over 550 bird species making it a paradise for bird watchers. The park has the highest concentration of breeding bird species in the world. Other mammals include buffaloes, giraffes, wildebeest, zebras, impalas, elands, fringe-eared oryx, and pythons in the swamps.',
    'Dominated by ancient baobab trees, umbrella acacias, and sausage trees. Swamp areas feature dense vegetation including palms and tall grasses. Open plains with scattered acacias.',
    '/images/parks/tarangire.jpg',
    'June to October (dry season - best for wildlife concentration)',
    '6:00 AM - 6:00 PM daily',
    'Accessible by road from Arusha (2 hours). Main entrance at the northern gate. Good tarmac road to the park entrance.',
    'Elephants, Baobab Trees, Bird Watching, Wildlife, Dry Season Safari, Tarangire River, Migration, Big Cats',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 4. Lake Manyara National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Lake Manyara National Park',
    'lake-manyara-national-park',
    'NATIONAL_PARK',
    'Manyara',
    'Monduli',
    'Northern Tanzania, 126 km west of Arusha, near Great Rift Valley escarpment',
    -3.5333,
    35.8333,
    '960-1,828 meters',
    '330 km² (including 230 km² of lake surface)',
    'Famous for its tree-climbing lions, large flocks of flamingos, and stunning location at the base of the Great Rift Valley escarpment.',
    'Lake Manyara National Park is a compact gem located between Lake Manyara and the Great Rift Valley escarpment. Despite its small size (330 square kilometers with two-thirds being the lake), it hosts remarkable biodiversity. The park is famous for its unusual tree-climbing lions, which lounge in acacia branches during the heat of the day. The alkaline lake attracts thousands of flamingos, creating spectacular pink shores during certain seasons. The groundwater forest at the park entrance is one of the few in East Africa, fed by underground streams from the rift valley wall. The park offers diverse habitats from the lake and wetlands to acacia woodlands and montane forest.',
    'Established as a national park in 1960. The area was previously a hunting ground and gained protection due to its unique wildlife and ecological importance.',
    'Diverse ecosystems including groundwater forest, acacia woodland, open grassland, swamps, and the alkaline lake. The Great Rift Valley escarpment forms a dramatic backdrop.',
    'Tree-climbing lions are the park''s most famous residents. Large elephant herds, buffaloes, giraffes, zebras, wildebeest, hippos, and a remarkable variety of primates including baboons and blue monkeys. Over 400 bird species recorded, including flamingos, pelicans, storks, and cormorants. The park has one of the highest biomass densities of large mammals in the world.',
    'Groundwater forest with mahogany and wild mango trees at the park entrance. Acacia woodlands dominate the drier areas. Lush vegetation supported by underground water sources from the escarpment.',
    '/images/parks/lake-manyara.jpg',
    'Year-round destination; July to October and January to February best for wildlife; November to June for flamingos',
    '6:30 AM - 6:00 PM daily',
    'Accessible by road from Arusha (2 hours). Located on the main route to Serengeti and Ngorongoro. Good tarmac road to park entrance.',
    'Tree-climbing Lions, Flamingos, Great Rift Valley, Bird Watching, Groundwater Forest, Elephants, Diverse Ecosystems',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 5. Arusha National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Arusha National Park',
    'arusha-national-park',
    'NATIONAL_PARK',
    'Arusha',
    'Arusha',
    'Northern Tanzania, 32 km northeast of Arusha City',
    -3.2500,
    36.8333,
    '1,500-4,566 meters (Mount Meru summit)',
    '137 km²',
    'Home to Mount Meru (4,566m), Tanzania''s second-highest peak, featuring stunning landscapes, diverse wildlife, and unique walking safari opportunities.',
    'Arusha National Park, though small at 137 square kilometers, offers incredible diversity within close proximity to Arusha city. The park is dominated by Mount Meru, an active volcano and Tanzania''s second-highest mountain at 4,566 meters. The park features three distinct areas: Ngurdoto Crater (often called "Little Ngorongoro"), the Momella Lakes, and the rugged Mount Meru. It is one of the few parks in Tanzania where walking safaris are permitted, allowing intimate wildlife encounters. The park is home to black and white colobus monkeys, the rare red duiker, and over 400 bird species. The Momella Lakes, each a different shade of green or blue, attract thousands of flamingos and other water birds.',
    'Established as a national park in 1960, initially as Ngurdoto Crater National Park. Expanded in 1967 to include Mount Meru and renamed Arusha National Park.',
    'Diverse habitats including montane forest, highland moorland, alpine desert, crater floor, alkaline lakes, and open grassland. Ngurdoto Crater is a pristine wilderness.',
    'Black and white colobus monkeys, blue monkeys, elephants, buffaloes, giraffes, leopards, spotted hyenas, and the rare red duiker. The park has over 400 bird species. Wildlife viewing is excellent around Momella Lakes and in Ngurdoto Crater (viewable from rim).',
    'Montane forest with tall trees and dense undergrowth on Mount Meru slopes. Highland moorland and alpine vegetation at higher elevations. Grasslands and swamps around Momella Lakes.',
    '/images/parks/arusha.jpg',
    'Year-round; June to February best for Mount Meru trekking; best wildlife viewing June to October',
    '6:30 AM - 6:30 PM daily',
    'Accessible by road from Arusha (45 minutes). Multiple entry gates including Momella Gate and Ngongongare Gate. Excellent road access.',
    'Mount Meru, Trekking, Colobus Monkeys, Walking Safari, Ngurdoto Crater, Momella Lakes, Bird Watching, Alpine',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 6. Kilimanjaro National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Kilimanjaro National Park',
    'kilimanjaro-national-park',
    'NATIONAL_PARK',
    'Kilimanjaro',
    'Moshi Rural',
    'Northern Tanzania, near Kenyan border, 128 km from Arusha',
    -3.0674,
    37.3556,
    '1,800-5,895 meters (Uhuru Peak)',
    '1,668 km²',
    'Home to Mount Kilimanjaro, Africa''s highest mountain at 5,895 meters, and the world''s tallest free-standing mountain.',
    'Kilimanjaro National Park was established to protect Mount Kilimanjaro, Africa''s highest peak at 5,895 meters (19,341 feet) and the world''s tallest free-standing mountain. The park covers 1,668 square kilometers and includes the mountain''s moorland and highland zones above 2,700 meters. Mount Kilimanjaro is a dormant volcano with three volcanic cones: Kibo (the highest), Mawenzi, and Shira. The mountain features five distinct climatic zones from cultivated farmland at the base through rainforest, heath and moorland, alpine desert, to the arctic summit zone with glaciers and ice fields. Over 35,000 people attempt to reach Uhuru Peak annually via six official routes. The park is a UNESCO World Heritage Site.',
    'Established as a national park in 1973 and designated a UNESCO World Heritage Site in 1987. First summited by Hans Meyer and Ludwig Purtscheller in 1889.',
    'Five distinct vegetation zones: montane forest (1,800-2,800m), heath and moorland (2,800-4,000m), alpine desert (4,000-5,000m), and arctic summit zone (above 5,000m). Each zone has unique flora and fauna.',
    'Wildlife is limited due to high altitude but includes elephants, buffaloes, leopards, bushbucks, red duikers, and various primates in lower forests. Over 140 bird species. Blue monkeys and colobus monkeys common in forest zones.',
    'Montane forest with camphor, fig, and yellowwood trees. Heath zone with giant heathers. Moorland with tussock grasses. Alpine zone with unique species like giant groundsels and lobelias. Glaciers at summit.',
    '/images/parks/kilimanjaro.jpg',
    'January to March and June to October (dry seasons for climbing)',
    '24 hours (climbers register during day)',
    'Main gate at Marangu. Other gates: Machame, Londorossi, Lemosho, Rongai, Umbwe. Accessible from Moshi (1 hour) or Arusha (2.5 hours).',
    'Mount Kilimanjaro, Uhuru Peak, Trekking, UNESCO World Heritage, Africa Highest Mountain, Glaciers, Alpine, Seven Summits',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================================
-- SOUTHERN CIRCUIT PARKS
-- =====================================================================

-- 7. Nyerere National Park (formerly Selous Game Reserve - northern sector)
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Nyerere National Park',
    'nyerere-national-park',
    'NATIONAL_PARK',
    'Morogoro, Ruvuma, and Lindi',
    'Liwale',
    'Southern Tanzania, 219 km from Dar es Salaam',
    -7.6667,
    37.6167,
    '100-1,300 meters',
    '30,893 km² (total Selous area: 54,600 km²)',
    'Africa''s largest game reserve and UNESCO World Heritage Site, renamed in 2019 to honor President Julius Nyerere, featuring vast wilderness and the Rufiji River.',
    'Nyerere National Park, created in 2019 from the northern portion of the Selous Game Reserve, is Tanzania''s largest national park at 30,893 square kilometers. The broader Selous ecosystem covers 54,600 square kilometers, making it one of the largest protected areas in Africa. The park was renamed to honor Julius Kambarage Nyerere, Tanzania''s founding father. The Rufiji River, Tanzania''s largest river, flows through the reserve creating a network of channels, lakes, and swamps that support incredible biodiversity. The area is a UNESCO World Heritage Site and remains one of Africa''s last great wilderness areas with relatively few tourists. The landscape includes miombo woodland, open grasslands, riverine forests, and swamps.',
    'Originally established as a game reserve in 1905 during German colonial rule. Designated a UNESCO World Heritage Site in 1982. Northern sector declared Nyerere National Park in 2019.',
    'Miombo woodland dominates with acacia, riverine forests along the Rufiji River, open grasslands, seasonal swamps, and lakes. The Rufiji River system is the ecosystem''s heart.',
    'One of the largest populations of elephants in Africa (over 15,000), plus significant populations of lions, leopards, wild dogs, buffaloes, hippos, and crocodiles. The reserve has Tanzania''s largest population of endangered African wild dogs. Over 440 bird species including rare species like Pel''s fishing owl.',
    'Dominated by miombo woodland (Brachystegia species), borassus palms along rivers, grasslands, and riverine forests with mahogany and ebony trees.',
    '/images/parks/nyerere.jpg',
    'June to October (dry season), also good December to February',
    '6:00 AM - 6:00 PM daily',
    'Accessible by air to Mtemere, Kiba, or Siwandu airstrips. Road access from Dar es Salaam (5-6 hours) via Kibiti. Boat safaris available.',
    'UNESCO World Heritage, Rufiji River, Wild Dogs, Wilderness, Boat Safari, Elephants, Remote, Nyerere',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 8. Ruaha National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Ruaha National Park',
    'ruaha-national-park',
    'NATIONAL_PARK',
    'Iringa and Dodoma',
    'Iringa',
    'Central-southern Tanzania, 130 km from Iringa',
    -7.9000,
    34.8333,
    '750-1,868 meters',
    '20,226 km²',
    'Tanzania''s largest national park, featuring dramatic landscapes, the Great Ruaha River, and one of Africa''s largest elephant populations.',
    'Ruaha National Park is Tanzania''s largest national park at 20,226 square kilometers and forms the core of a 45,000 square kilometer ecosystem. Named after the Great Ruaha River that flows along its southeastern boundary, the park features dramatic landscapes of rolling hills, rocky escarpments, and the river valley. Ruaha is a transition zone where eastern and southern African flora and fauna meet, creating exceptional biodiversity. The park is famous for its large elephant population (over 12,000) and has one of the highest concentrations of elephants in East Africa. It''s also renowned for its predators, particularly lions, with some of the largest prides in Africa. The park remains relatively remote and uncrowded, offering authentic wilderness experiences.',
    'Originally part of Rungwa Game Reserve established in 1910. Upgraded to national park status in 1964. Expanded in 2008 to include Usangu Game Reserve.',
    'Diverse landscapes including miombo woodland, acacia savanna, riverine forests, baobab-studded plains, rocky hills, and the Great Ruaha River valley with its wetlands.',
    'Over 12,000 elephants, one of Tanzania''s largest populations of buffaloes (over 30,000), and significant predator numbers including over 10% of Africa''s lions. Also leopards, cheetahs, wild dogs, spotted hyenas, and both greater and lesser kudus. Over 571 bird species recorded.',
    'Dominated by miombo woodland with Brachystegia and Julbernardia trees. Ancient baobabs, acacia species, and riverine forests along watercourses. Commiphora-Combretum woodlands on rocky hills.',
    '/images/parks/ruaha.jpg',
    'June to October (dry season - best wildlife viewing)',
    '6:00 AM - 6:00 PM daily',
    'Accessible by air to Msembe airstrip (daily flights from Dar es Salaam). Road access from Iringa (4-5 hours on rough roads).',
    'Elephants, Lions, Great Ruaha River, Remote, Wild Dogs, Baobabs, Predators, Wilderness, Southern Circuit',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 9. Mikumi National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Mikumi National Park',
    'mikumi-national-park',
    'NATIONAL_PARK',
    'Morogoro',
    'Kilosa',
    'Southern-central Tanzania, 283 km west of Dar es Salaam',
    -7.3833,
    36.9000,
    '550-1,250 meters',
    '3,230 km²',
    'The fourth-largest national park in Tanzania, known for reliable wildlife viewing and its location along the main Dar es Salaam-Iringa highway.',
    'Mikumi National Park covers 3,230 square kilometers and is Tanzania''s fourth-largest national park. The park''s proximity to Dar es Salaam (4-5 hours drive) makes it the most accessible southern park. The Mkata River floodplain dominates the park center, often compared to the Serengeti due to its open grasslands and abundant wildlife. The park is bordered by the Uluguru Mountains to the northeast, Udzungwa Mountains to the southwest, and is part of a larger ecosystem that includes Selous Game Reserve to the south. Wildlife viewing is excellent year-round, with animals easily spotted in the open plains. The Tanzania-Zambia highway bisects the park, making it one of the few parks you can view wildlife from a public road.',
    'Established as a national park in 1964. Originally part of the greater Selous ecosystem. The area was a hunting reserve before receiving park protection.',
    'Open grassland plains (Mkata floodplain), miombo woodland, acacia savanna, and riverine forests. Mountains border the park creating scenic backdrops.',
    'Large populations of elephants, buffaloes, giraffes, zebras, wildebeest, impalas, and elands. Lions, leopards, and wild dogs present. Hippo pools along the Mkata River. Over 400 bird species including Eurasian migrants. The park has healthy populations of yellow baboons.',
    'Mkata floodplain grasslands, miombo woodland dominated by Brachystegia species, acacia trees, and borassus palms in wetter areas. Riverine forests along watercourses.',
    '/images/parks/mikumi.jpg',
    'Year-round destination; June to October best for wildlife; November to May for bird watching',
    '6:00 AM - 6:00 PM daily',
    'Main gate accessible from Dar es Salaam (4-5 hours) via Morogoro. Park is bisected by the Dar-Iringa-Zambia highway (A7).',
    'Accessible, Mkata Floodplain, Elephants, Lions, Bird Watching, Uluguru Mountains, Southern Circuit',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 10. Udzungwa Mountains National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Udzungwa Mountains National Park',
    'udzungwa-mountains-national-park',
    'NATIONAL_PARK',
    'Morogoro',
    'Kilolo',
    'Southern-central Tanzania, 350 km southwest of Dar es Salaam',
    -7.7500,
    36.5000,
    '250-2,576 meters (Luhombero Peak)',
    '1,990 km²',
    'A biodiversity hotspot featuring pristine tropical rainforest, endemic primates, spectacular waterfalls, and excellent hiking trails.',
    'Udzungwa Mountains National Park protects 1,990 square kilometers of the Eastern Arc Mountains, one of the world''s biodiversity hotspots. The park is unique among Tanzania''s national parks as it has no roads and can only be explored on foot, making it a paradise for hikers. The mountains harbor exceptional biodiversity with numerous endemic species found nowhere else on Earth. The park contains Africa''s second-largest forest elephant population and is home to several primate species including two found only in Udzungwa: the Iringa red colobus and Sanje mangabey. The Sanje Waterfall, plunging 170 meters in three cascades, is the park''s most spectacular feature. The park ranges from lowland rainforest to montane forest and grassland plateaus.',
    'Gazetted as a national park in 1992. The area was previously managed as a forest reserve. Conservation efforts intensified due to the discovery of endemic species.',
    'Tropical rainforest, montane forest, miombo woodland, grassland plateaus, streams, and waterfalls. Altitudinal range creates diverse habitats from lowland to montane zones.',
    'Six primate species including endemic Iringa red colobus and Sanje mangabey. Other primates: black and white colobus, blue monkeys, yellow baboons. Forest elephants, buffaloes, duikers, bush pigs. Over 400 bird species with many endemics including the Udzungwa forest partridge.',
    'Pristine tropical rainforest with diverse tree species. Montane forest at higher elevations. Highland grasslands on plateaus. Rich understory vegetation and epiphytes.',
    '/images/parks/udzungwa.jpg',
    'Year-round; June to October (dry season) best for hiking; December to April for waterfalls',
    '7:00 AM - 5:00 PM daily',
    'Main entrance at Mang''ula village, accessible from Mikumi (65 km southwest) or Dar es Salaam (350 km, 6-7 hours).',
    'Hiking, Waterfalls, Endemic Species, Primates, Rainforest, Biodiversity Hotspot, Eastern Arc, Trekking',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================================
-- WESTERN CIRCUIT PARKS
-- =====================================================================

-- 11. Katavi National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Katavi National Park',
    'katavi-national-park',
    'NATIONAL_PARK',
    'Katavi',
    'Mpanda',
    'Western Tanzania, 40 km south of Mpanda',
    -6.9167,
    31.2000,
    '900-1,200 meters',
    '4,471 km²',
    'One of Tanzania''s most remote and unspoiled wilderness areas, famous for huge buffalo herds and hippo concentrations in the dry season.',
    'Katavi National Park is Tanzania''s third-largest park at 4,471 square kilometers and one of its most remote and least visited. This isolation has preserved its wild character, offering visitors an authentic African wilderness experience. During the dry season (May-October), the Katuma River and seasonal lakes Katavi and Chada become wildlife magnets. The park is famous for having some of the highest concentrations of hippos and crocodiles in Africa, with hundreds of hippos crowding together in shrinking pools. Buffalo herds can number in the thousands. The park''s remoteness means there are often more lions than tourists. The floodplains of the Katuma River dominate the park, surrounded by miombo woodlands.',
    'Established as a national park in 1974. Previously part of a hunting reserve. Named after the legendary spirit Katabi who is believed to have lived in a tamarind tree near Lake Katavi.',
    'Katuma River floodplains, seasonal lakes (Katavi and Chada), miombo woodland, riverine forests, and open grasslands. Seasonal variation dramatically changes the landscape.',
    'Huge herds of buffaloes (sometimes 1,000+), Tanzania''s densest concentrations of hippos and crocodiles. Large populations of elephants, lions, leopards, hyenas, giraffes, zebras, and various antelope species including roan and sable. Over 400 bird species.',
    'Miombo woodland dominated by Brachystegia species. Floodplain grasslands, palm-fringed pools, and riverine forests. The landscape transforms dramatically between wet and dry seasons.',
    '/images/parks/katavi.jpg',
    'June to October (dry season - spectacular wildlife concentrations)',
    '6:00 AM - 6:00 PM daily',
    'Accessible by air to Ikuu airstrip (flights from Arusha or Dar es Salaam). Road access very difficult, 40 km from Mpanda (rough roads).',
    'Remote, Wilderness, Buffalo Herds, Hippos, Crocodiles, Western Circuit, Unspoiled, Adventure',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 12. Mahale Mountains National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Mahale Mountains National Park',
    'mahale-mountains-national-park',
    'NATIONAL_PARK',
    'Kigoma',
    'Kigoma Rural',
    'Western Tanzania, on Lake Tanganyika shore, 120 km south of Kigoma',
    -6.2500,
    29.9167,
    '772-2,462 meters (Mount Nkungwe)',
    '1,613 km²',
    'A remote paradise on Lake Tanganyika shores, home to one of Africa''s last remaining wild chimpanzee populations and accessible only by boat.',
    'Mahale Mountains National Park protects 1,613 square kilometers on the shores of Lake Tanganyika, the world''s longest and second-deepest freshwater lake. The park is home to approximately 1,000 wild chimpanzees, and one habituated group (the M-group) can be tracked by visitors. Chimpanzee trekking here is considered one of Africa''s greatest wildlife experiences. The park is completely remote with no roads; access is only by boat across Lake Tanganyika. The Mahale Mountains rise dramatically from the lakeshore to over 2,400 meters, covered in pristine forests. The combination of lake, mountains, forests, and primates creates a unique ecosystem. Besides chimpanzees, the park has eight other primate species. The crystal-clear waters of Lake Tanganyika offer excellent snorkeling and kayaking.',
    'Established as a national park in 1985. Japanese primatologist Dr. Toshisada Nishida began chimpanzee research here in the 1960s, which continues today.',
    'Montane forest, lowland rainforest, miombo woodland, grassland, bamboo forests, and Lake Tanganyika shoreline. The altitudinal range creates diverse habitats.',
    'Approximately 1,000 wild chimpanzees with one habituated group of about 60 individuals. Eight other primate species including red colobus, blue monkeys, red-tailed monkeys, and yellow baboons. Leopards, bushbucks, and various small mammals. Over 355 bird species. Lake Tanganyika has over 250 endemic fish species.',
    'Montane evergreen forest on mountain slopes, lowland rainforest, miombo woodland, and bamboo at higher elevations. Rich biodiversity with many endemic plant species.',
    '/images/parks/mahale.jpg',
    'May to October (dry season - best chimpanzee tracking)',
    '6:00 AM - 6:00 PM daily (trekking permits required)',
    'Accessible only by boat or chartered flight. 2-3 hour boat ride from Kigoma or fly to Mahale airstrip. No roads to the park.',
    'Chimpanzees, Lake Tanganyika, Remote, Primate Trekking, Snorkeling, Mountains, Western Circuit, Pristine',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 13. Gombe Stream National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Gombe Stream National Park',
    'gombe-stream-national-park',
    'NATIONAL_PARK',
    'Kigoma',
    'Kigoma Rural',
    'Western Tanzania, on Lake Tanganyika shore, 16 km north of Kigoma',
    -4.6667,
    29.6167,
    '772-1,500 meters',
    '52 km²',
    'Tanzania''s smallest national park, world-famous as the site of Jane Goodall''s pioneering chimpanzee research since 1960.',
    'Gombe Stream National Park is Tanzania''s smallest national park at just 52 square kilometers but is globally renowned as the site of Dr. Jane Goodall''s groundbreaking chimpanzee research, which began in 1960 and continues today as the world''s longest-running wildlife research project. The park protects a narrow strip of mountainous forest along the eastern shore of Lake Tanganyika. About 100 chimpanzees live in Gombe, with some individuals habituated to human presence for research and tourism. The park''s steep valleys are covered in forest and separated by ridges. Besides chimpanzees, Gombe hosts other primates including red-tailed monkeys, red colobus, blue monkeys, and olive baboons. The park offers chimpanzee tracking, forest walks, and swimming in Lake Tanganyika.',
    'Established as a game reserve in 1943 and upgraded to national park status in 1968. Jane Goodall arrived in 1960 to study chimpanzees, making revolutionary discoveries about tool use and social behavior.',
    'Lowland evergreen forest in valleys, miombo woodland on ridges, grassland plateaus, and Lake Tanganyika shoreline. Steep terrain with streams flowing to the lake.',
    'Approximately 100 chimpanzees with several habituated groups. Other primates: red-tailed monkeys, red colobus, blue monkeys, olive baboons. Bushbucks, bushpigs, leopards. Over 200 bird species. Lake fish species.',
    'Gallery forest in valleys with dense canopy, miombo woodland on slopes and ridges, grasslands on plateaus. Diverse plant species adapted to varied topography.',
    '/images/parks/gombe.jpg',
    'Year-round; dry season (June to October) best for chimpanzee tracking',
    '6:00 AM - 6:00 PM daily (trekking permits required)',
    'Accessible only by boat from Kigoma (30 minutes to 1 hour). No road access. Can combine with Mahale visit.',
    'Chimpanzees, Jane Goodall, Research, Lake Tanganyika, Primates, Western Circuit, Small Park, Historical',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 14. Rubondo Island National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Rubondo Island National Park',
    'rubondo-island-national-park',
    'NATIONAL_PARK',
    'Geita',
    'Biharamulo',
    'Northwestern Tanzania, island in Lake Victoria, west of Mwanza',
    -2.3000,
    31.8333,
    '1,134-1,486 meters',
    '457 km² (240 km² is land, rest is water)',
    'A pristine island sanctuary in Lake Victoria, offering unique combination of forest, wetlands, and lake ecosystems with introduced chimps and rare sitatunga.',
    'Rubondo Island National Park comprises Rubondo Island and several smaller islets in the southwestern corner of Lake Victoria, Africa''s largest lake. Established in 1977, about 90% of the island is covered in dense forest, making it one of East Africa''s most pristine forest reserves. The park is unique as a successful wildlife introduction site: chimpanzees were introduced in the 1960s, along with other species like elephants, giraffes, black rhinos (no longer present), roan antelopes, and suni. The island is a bird-watcher''s paradise with over 400 species, including rare endemic species. Rubondo is one of the few places in Tanzania where you can observe wild chimpanzees, spot sitatunga (a rare aquatic antelope), and enjoy fishing for massive Nile perch. The park remains off the beaten track.',
    'Declared a national park in 1977. Previously served as a wildlife introduction site starting in the 1960s under Frankfurt Zoological Society. Chimpanzee reintroduction project was pioneering conservation work.',
    'Closed-canopy tropical forest, papyrus wetlands, savanna, rocky shores, sandy beaches, and Lake Victoria waters. The island has diverse microhabitats.',
    'Introduced species: chimpanzees, elephants, giraffes, suni, roan antelopes, black and white colobus monkeys. Indigenous: sitatunga, hippos, crocodiles, otters, bushbucks, cane rats, marsh mongooses. Over 400 bird species including fish eagles, kingfishers, storks, and migratory species. Lake Victoria has tilapia and Nile perch.',
    'Dense tropical forest with mahogany, fig, and other hardwood species. Papyrus swamps, open grasslands, and aquatic vegetation. Rich plant diversity.',
    '/images/parks/rubondo.jpg',
    'Year-round; June to October (dry season) best for chimpanzee tracking and fishing',
    '6:00 AM - 6:00 PM daily',
    'Accessible by boat from Nkome (3.5 hours) or Muganza (1 hour). Fly to Rubondo airstrip from Mwanza or Arusha. No road access.',
    'Island, Lake Victoria, Chimpanzees, Sitatunga, Bird Watching, Fishing, Remote, Forest, Western Circuit',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================================
-- COASTAL & MARINE PARKS
-- =====================================================================

-- 15. Saadani National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Saadani National Park',
    'saadani-national-park',
    'NATIONAL_PARK',
    'Pwani',
    'Bagamoyo and Pangani',
    'Coastal Tanzania, 45 km north of Bagamoyo, 100 km north of Dar es Salaam',
    -6.0000,
    38.7667,
    '0-200 meters',
    '1,062 km²',
    'Tanzania''s only wildlife sanctuary bordering the Indian Ocean, offering a unique combination of beach and bush safari experiences.',
    'Saadani National Park is unique as Tanzania''s only coastal wildlife sanctuary, where the beach meets the bush. Covering 1,062 square kilometers with 30 kilometers of Indian Ocean coastline, the park offers the rare experience of viewing wildlife with ocean views. The Wami River forms the park''s southern boundary and attracts large numbers of hippos, crocodiles, and water birds. Saadani hosts four of the Big Five (lions, leopards, elephants, and buffaloes - no rhinos) alongside coastal species. The beaches are important green turtle nesting sites. The park includes the historic Saadani village, which was once a trading port in the 19th century. Boat safaris on the Wami River offer different perspectives of wildlife. The park combines traditional safari activities with beach relaxation.',
    'Gazetted as a national park in 2005, combining the former Saadani Game Reserve, Mkwaja Ranch, Wami River, and Zaraninge Forest. The area has historical significance as a trading port.',
    'Coastal ecosystems including beaches, mangrove forests, salt flats, acacia woodlands, riverine forests, open grasslands, and the Wami River system.',
    'Four of the Big Five: elephants, lions, leopards, buffaloes. Also giraffes, wildebeest, hartebeest, waterbucks, reedbucks, warthogs, hippos, crocodiles. Green turtles nest on beaches. Over 40 species of fish, humpback whales and dolphins offshore. Over 300 bird species.',
    'Coastal mangrove forests, acacia woodlands, palm trees, riverine vegetation along the Wami River, and coastal dune vegetation. Mix of terrestrial and marine ecosystems.',
    '/images/parks/saadani.jpg',
    'Year-round; June to October (dry season) best for wildlife; November to February for turtle nesting',
    '6:00 AM - 6:00 PM daily',
    'Accessible by road from Dar es Salaam (2.5-3 hours to Saadani village). Also accessible by boat along the coast or via Wami River.',
    'Beach, Coastal, Ocean, Unique, Wami River, Turtles, Boat Safari, Historic, Beach Safari',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 16. Mafia Island Marine Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Mafia Island Marine Park',
    'mafia-island-marine-park',
    'MARINE_PARK',
    'Pwani',
    'Mafia',
    'Indian Ocean, 120 km southeast of Dar es Salaam',
    -7.9167,
    39.7500,
    '0-50 meters (mostly marine)',
    '822 km² (marine and terrestrial)',
    'Tanzania''s first marine protected area, renowned for pristine coral reefs, whale sharks, and world-class diving and snorkeling opportunities.',
    'Mafia Island Marine Park was established in 1995 as Tanzania''s first marine park, protecting 822 square kilometers around Mafia Island and its surrounding waters. The park encompasses diverse marine ecosystems including coral reefs, mangrove forests, seagrass beds, and marine channels. Chole Bay, partially enclosed by Chole, Jibondo, and Juani islands, forms the park''s heart with exceptional snorkeling and diving. The park is globally renowned for whale shark encounters (October to March), with these gentle giants commonly seen feeding near the surface. The coral reefs support over 400 fish species and are among the richest in East Africa. Five species of sea turtles nest on park beaches. Traditional fishing communities have used these waters for centuries, and the park incorporates community-based management. The island offers a tranquil alternative to Zanzibar.',
    'Gazetted as Tanzania''s first marine park in 1995. The area has historical significance with 13th-century ruins and was part of the Indian Ocean trading network.',
    'Diverse marine ecosystems: coral reefs, seagrass beds, mangrove forests, tidal channels, sandy beaches, and open ocean. Includes both shallow lagoons and deeper waters.',
    'Over 400 species of fish including groupers, snappers, parrotfish, angelfish, and butterflyfish. Whale sharks (seasonal), manta rays, five species of sea turtles, dolphins, dugongs (rare), and humpback whales (seasonal). Rich invertebrate life including octopus, lobsters, and sea urchins.',
    'Marine vegetation includes extensive seagrass beds (important for dugongs and turtles) and mangrove forests (eight species) along coastlines. Coral reefs with diverse hard and soft coral species.',
    '/images/parks/mafia-marine.jpg',
    'Year-round; October to March for whale sharks; June to October for diving visibility',
    'Park accessible 24/7; diving/snorkeling during daylight',
    'Accessible by air (30-minute flight from Dar es Salaam) or boat. Main settlements: Kilindoni (port), Utende (hotels), and Chole Island.',
    'Marine Park, Diving, Snorkeling, Whale Sharks, Coral Reefs, Sea Turtles, Island, Conservation, Indian Ocean',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 17. Mnazi Bay-Ruvuma Estuary Marine Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Mnazi Bay-Ruvuma Estuary Marine Park',
    'mnazi-bay-ruvuma-estuary-marine-park',
    'MARINE_PARK',
    'Mtwara',
    'Mtwara Rural',
    'Far southern Tanzania, 396 km south of Dar es Salaam, near Mozambique border',
    -10.3833,
    40.3500,
    '0-100 meters (mostly marine)',
    '650 km² (33% land, 67% marine)',
    'A pristine marine park at Tanzania''s southern border featuring diverse coral reefs, mangroves, and important turtle nesting sites in a remote location.',
    'Mnazi Bay-Ruvuma Estuary Marine Park protects 650 square kilometers of coastal and marine ecosystems in far southern Tanzania, extending from Mnazi Bay to the Ruvuma River Estuary at the Mozambique border. Established in 2000, the park is one of Tanzania''s most pristine and least visited marine areas. The park encompasses coral reefs, mangrove forests, seagrass beds, sandy beaches, and the Ruvuma River estuary. Five species of sea turtles nest on the beaches, and the area is critical for marine biodiversity. The park has excellent snorkeling and diving with coral reefs in good condition due to limited human impact. Humpback whales migrate through these waters, and dolphins are commonly sighted. The park faces minimal tourist pressure, offering an off-the-beaten-path marine experience. Local fishing communities are involved in conservation management.',
    'Gazetted as a marine park in 2000. The area has minimal development and retains its wild character. Conservation efforts focus on protecting marine biodiversity and supporting sustainable fishing.',
    'Diverse coastal and marine ecosystems: coral reefs, extensive mangrove forests, seagrass meadows, sandy beaches, rocky shores, and the Ruvuma River estuary with freshwater-saltwater mixing.',
    'Over 400 fish species, five sea turtle species (green, hawksbill, loggerhead, olive ridley, leatherback), humpback whales (seasonal migration), dolphins, dugongs (rare), rays, and diverse invertebrates. Important bird habitat with coastal and marine species.',
    'Extensive mangrove forests (nine species), seagrass beds supporting turtles and dugongs, diverse coral species forming reefs, and coastal vegetation including coconut palms and casuarina trees.',
    '/images/parks/mnazi-bay.jpg',
    'Year-round; June to October (dry season) best for diving; November to February for turtle nesting',
    'Park accessible 24/7; activities during daylight hours',
    'Accessible from Mtwara town (20 km). Fly to Mtwara from Dar es Salaam or drive (10-12 hours). Boat access to various park sites.',
    'Marine Park, Remote, Pristine, Diving, Snorkeling, Sea Turtles, Mangroves, Coral Reefs, Southern Tanzania',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================================
-- ADDITIONAL NOTABLE PARKS
-- =====================================================================

-- 18. Mkomazi National Park
INSERT INTO parks (
    name, slug, park_type, region, district, location,
    latitude, longitude, elevation, size,
    short_description, full_description, history, ecosystem, wildlife, vegetation,
    primary_image, best_time_to_visit, opening_hours, access_information, tags, is_active,
    created_at, updated_at
) VALUES (
    'Mkomazi National Park',
    'mkomazi-national-park',
    'NATIONAL_PARK',
    'Kilimanjaro and Tanga',
    'Same',
    'Northern Tanzania, 112 km from Moshi, borders Kenya',
    -4.2500,
    37.9167,
    '230-1,570 meters',
    '3,245 km²',
    'A vital sanctuary for endangered black rhinos and African wild dogs, with spectacular views of Kilimanjaro and the Usambara Mountains.',
    'Mkomazi National Park covers 3,245 square kilometers in northern Tanzania, forming a continuous ecosystem with Kenya''s Tsavo West National Park. Upgraded from a game reserve to national park status in 2008, Mkomazi is particularly significant for its black rhino and African wild dog conservation programs. The park operates a successful rhino sanctuary protecting critically endangered black rhinos in a fenced area. Similarly, a wild dog breeding program helps conserve these endangered predators. The park''s landscape features acacia-commiphora bushland, baobabs, and dramatic views of Mount Kilimanjaro to the northwest and the Usambara Mountains to the southeast. Despite being relatively close to tourist circuits, Mkomazi remains undervisited, offering wilderness experiences. The park has populations of elephants, giraffes, oryx, gerenuk, and various antelope species.',
    'Originally gazetted as a game reserve in 1951. Upgraded to national park status in 2008. Black rhino sanctuary established in 1997; wild dog program started in 2003.',
    'Acacia-commiphora bushland, open grasslands, rocky hills, seasonal rivers, and baobab-studded landscapes. Elevation ranges create habitat diversity.',
    'Black rhinos (in sanctuary), African wild dogs (breeding program), elephants, giraffes, elands, hartebeest, lesser kudu, gerenuk, fringe-eared oryx, Grant''s gazelles, lions, leopards, cheetahs, and spotted hyenas. Over 450 bird species including dry-country specialists.',
    'Dominated by acacia and commiphora bushland. Ancient baobabs scattered throughout. Grasslands with scattered trees. Vegetation adapted to semi-arid conditions.',
    '/images/parks/mkomazi.jpg',
    'June to February; avoid March to May (wet season)',
    '6:00 AM - 6:00 PM daily',
    'Main gate at Zange, 112 km from Moshi. Accessible from Arusha (5-6 hours) or from Dar es Salaam via Tanga. Airstrip available.',
    'Black Rhino, Wild Dogs, Conservation, Kilimanjaro Views, Endangered Species, Remote, Northern Circuit',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =====================================================================
-- NOTES ON DATA SOURCES AND ACCURACY
-- =====================================================================
-- All data compiled from:
-- 1. Tanzania National Parks Authority (TANAPA) official information
-- 2. UNESCO World Heritage Site documentation
-- 3. Wildlife Conservation Society reports
-- 4. Safari guide resources and conservation databases
-- 5. Geographic coordinate databases
--
-- Coordinates are approximate center points of parks
-- Park sizes are official figures where available
-- Best times to visit based on seasonal wildlife patterns and weather
-- All parks are active and open to visitors as of December 2025
-- =====================================================================
