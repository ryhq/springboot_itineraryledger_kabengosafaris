-- =====================================================================
-- Tanzania Tourism Activities Data
-- =====================================================================
-- This file contains comprehensive activity data for Tanzania tourism
-- based on research of actual safari and tourism activities
-- Table: activities
-- =====================================================================

-- Clear existing data (optional - comment out if you want to keep existing data)
-- TRUNCATE TABLE activities;

-- =====================================================================
-- SAFARI & GAME VIEWING ACTIVITIES
-- =====================================================================

INSERT INTO activities (name, slug, has_tariff, is_web_active, charging_basis, description, detailed_description, minimum_age, maximum_participants, equipment_required, season_availability, primary_image, tags, safety_information, is_active) VALUES
('Game Drive Safari', 'game-drive-safari', TRUE, TRUE, 'PER_PERSON',
'Experience the thrill of spotting wildlife in their natural habitat during guided game drives through Tanzania''s renowned national parks.',
'A game drive safari is the quintessential African safari experience. Conducted in specially designed 4x4 safari vehicles with pop-up roofs, these excursions take you through the vast savannas and diverse ecosystems of Tanzania''s parks. Expert guides help you spot and identify the Big Five (lion, leopard, elephant, buffalo, and rhino) along with hundreds of other species including cheetahs, giraffes, zebras, and wildebeest. Game drives typically last 3-4 hours and are conducted during early morning (6:00-10:00 AM) or late afternoon (3:30-6:30 PM) when animals are most active. Full-day drives with packed lunches are also available. Your guide will share insights about animal behavior, ecology, and conservation efforts while ensuring you get the best photographic opportunities.',
5, 6,
'Binoculars, camera with telephoto lens, sunscreen, wide-brimmed hat, sunglasses, water bottle, light jacket for early morning',
'Year-round; Best: June-October (dry season), December-March (calving season)',
NULL,
'Safari, Wildlife, Big Five, Photography, Nature, Conservation, Serengeti, Ngorongoro',
'Remain seated and quiet inside the vehicle at all times unless instructed otherwise by your guide. Do not stand up or lean out when near animals. Follow all instructions from your guide regarding safe distances. Never feed wildlife or attempt to touch animals.',
TRUE),

('Walking Safari', 'walking-safari', TRUE, TRUE, 'PER_PERSON',
'Explore the wilderness on foot with armed guides, experiencing nature at ground level and tracking animals through the bush.',
'Walking safaris offer an intimate and thrilling way to experience Tanzania''s wilderness. Led by armed park rangers and professional guides, you''ll explore the bush on foot, learning to track animals, identify plants, read spoor (animal tracks), and understand the intricate relationships within the ecosystem. This activity engages all your senses - the smell of wild sage, the sound of birds, the texture of tree bark, and the excitement of encountering wildlife at their level. Walking safaris typically last 2-4 hours and cover 5-8 kilometers at a gentle pace with frequent stops for observations and explanations. You''ll learn survival skills, understand animal behavior, and appreciate smaller details often missed from a vehicle - insects, plants, tracks, and signs of animal activity. The experience provides a deeper connection to nature and a sense of adventure that vehicle-based safaris cannot match.',
12, 8,
'Sturdy walking boots, long trousers, long-sleeved shirt (neutral colors), wide-brimmed hat, sunscreen, insect repellent, water bottle, small backpack, binoculars',
'Year-round; Best: June-October (dry season, easier walking)',
NULL,
'Walking, Nature, Adventure, Tracking, Education, Bush Skills, Conservation',
'Always follow guide instructions. Stay together as a group. Walk in single file when instructed. Remain quiet and avoid sudden movements. Do not run if you encounter animals. The armed ranger is for your protection - follow their commands immediately.',
TRUE),

('Night Game Drive', 'night-game-drive', TRUE, TRUE, 'PER_PERSON',
'Discover the nocturnal world of African wildlife on specially equipped night drives with spotlights.',
'Night game drives reveal a completely different side of the African wilderness. As darkness falls, nocturnal predators and prey emerge, creating thrilling opportunities to observe behaviors rarely seen during daylight hours. Equipped with powerful spotlights and red filters (which don''t disturb animals), you''ll search for leopards hunting, lions on the prowl, hyenas scavenging, genets, civets, bush babies, and nightjars. Your guide''s spotlight will illuminate eyes reflecting in the darkness - each color indicating different species. Night drives typically run from 6:30 PM to 9:30 PM and offer cooler temperatures and dramatic encounters. You might witness a predator making a kill, hear the spine-chilling calls of hyenas, or see porcupines, aardvarks, and other rarely-seen nocturnal creatures. The experience includes stargazing opportunities in areas with minimal light pollution.',
8, 6,
'Warm jacket or fleece, long trousers, closed shoes, flashlight/headlamp, camera with good low-light capability, binoculars',
'Year-round; Best: June-October (dry season, better visibility)',
NULL,
'Night Safari, Nocturnal Wildlife, Predators, Adventure, Leopard, Photography',
'Remain seated in the vehicle at all times. Do not shine personal lights at animals. Keep noise to a minimum. Follow guide instructions regarding when to use camera flash. Be aware that nocturnal predators are active.',
TRUE),

('Hot Air Balloon Safari', 'hot-air-balloon-safari', TRUE, TRUE, 'PER_PERSON',
'Float silently over the Serengeti plains at sunrise, enjoying breathtaking aerial views of wildlife and landscapes.',
'A hot air balloon safari is an unforgettable bucket-list experience. Beginning before dawn, you''ll watch the balloon inflation before climbing into the basket for a magical hour-long flight over the Serengeti or Tarangire. As the sun rises, casting golden light across the plains, you''ll drift silently at treetop level, witnessing wildlife from a unique perspective - herds of elephants, grazing wildebeest, giraffes running beneath you, and predators on the hunt. The pilot can adjust altitude from just above the ground to several hundred feet, providing varied perspectives. The flight covers 10-20 kilometers depending on wind conditions. After landing, celebrate with a champagne breakfast in the bush, a tradition dating back to the first balloon flights. Professional photographers capture your experience, and you''ll receive a flight certificate. This is one of the most spectacular and romantic ways to experience Tanzania''s wilderness.',
7, 16,
'Warm layers (early morning can be cold), sturdy shoes, hat, sunglasses, camera, sunscreen',
'Year-round; Best: June-October, January-February (optimal weather and wildlife viewing)',
NULL,
'Hot Air Balloon, Aerial Safari, Serengeti, Sunrise, Luxury, Romance, Photography, Champagne Breakfast',
'Follow all pilot instructions. Remain standing during flight - no sitting allowed. Hold on during landing which can be bumpy. Pregnant women and people with serious back problems should not participate. Minimum height restrictions apply for children.',
TRUE),

-- =====================================================================
-- WATER-BASED ACTIVITIES
-- =====================================================================

('Canoeing Safari', 'canoeing-safari', TRUE, TRUE, 'PER_PERSON',
'Paddle peacefully through rivers and lakes, observing hippos, crocodiles, and abundant birdlife from the water.',
'Canoeing safaris offer a unique aquatic perspective on Tanzania''s wildlife, particularly in areas like Arusha National Park''s Momella Lakes or along the Rufiji River in Selous/Nyerere. Paddling in stable two-person canoes, you''ll glide silently across calm waters, getting remarkably close to hippos (from a safe distance), observing crocodiles basking on riverbanks, and spotting diverse waterbirds including African fish eagles, kingfishers, herons, and storks. The experience typically lasts 2-3 hours and requires no previous canoeing experience - guides provide instruction and accompany you. You''ll learn about aquatic ecosystems, see animals coming to drink, and enjoy the serenity of being on the water. This activity is excellent for photographers wanting different angles and for those seeking a peaceful, contemplative wildlife experience. Some locations offer sunset canoeing for spectacular lighting and increased wildlife activity.',
10, 12,
'Life jacket (provided), sun protection, quick-dry clothing, waterproof bag for camera/phone, water bottle, binoculars',
'Year-round; Best: June-October (lower water levels, wildlife concentrated)',
NULL,
'Canoeing, Water Safari, Hippos, Birdwatching, Adventure, Peaceful, Photography',
'Always wear the provided life jacket. Follow guide instructions regarding distance from hippos and crocodiles. Stay together as a group. Do not stand up in the canoe. Avoid sudden movements or loud noises near hippos - they are territorial and dangerous.',
TRUE),

('Boat Safari', 'boat-safari', TRUE, TRUE, 'PER_GROUP',
'Cruise along rivers and lakes in motorized boats, perfect for wildlife viewing and sunset experiences.',
'Boat safaris provide comfortable wildlife viewing from motorized vessels on Tanzania''s rivers and lakes. Popular in Nyerere National Park (Selous), Lake Manyara, and along the Rufiji River, these cruises offer excellent opportunities to see hippos, crocodiles, elephants coming to drink, and an abundance of waterbirds. Boats accommodate 6-12 passengers and trips last 2-4 hours. Morning cruises focus on wildlife and bird activity, while sunset cruises offer spectacular photography opportunities with animals silhouetted against colorful skies. You''ll see fish eagles diving for prey, monitor lizards on riverbanks, and pods of hippos wallowing in the water. Some boat safaris include fishing opportunities. The stable platform is excellent for photography, and the quiet electric motors allow close approaches to wildlife without disturbance. Refreshments are usually provided on longer cruises.',
5, 12,
'Sunscreen, hat, sunglasses, camera, binoculars, light jacket for wind, water bottle',
'Year-round; Sunset cruises particularly stunning in dry season (June-October)',
NULL,
'Boat Safari, River Cruise, Hippos, Crocodiles, Sunset, Birdwatching, Photography, Relaxing',
'Remain seated while boat is moving. Keep hands inside the boat at all times. Do not lean over the sides. Follow guide instructions regarding safe distances from hippos. Lifejackets are available and should be worn by non-swimmers.',
TRUE),

('Sport Fishing', 'sport-fishing', TRUE, TRUE, 'PER_PERSON',
'Enjoy catch-and-release fishing in designated park areas for tilapia, catfish, and other species.',
'Sport fishing in Tanzania''s national parks and reserves is a regulated catch-and-release activity designed to provide recreational enjoyment while protecting fish populations. Available in select areas like Lake Manyara, Rubondo Island, and some Rufiji River sections, fishing trips last 3-4 hours and target species including tilapia, catfish (vundu), tiger fish, and others. Experienced guides provide equipment (rods, reels, bait) and instruction for beginners, while experienced anglers can bring their own gear. You''ll learn about aquatic ecosystems, fish behavior, and conservation efforts while enjoying the peaceful experience of fishing in stunning natural settings. All catches must be released unharmed, and barbless hooks are required. The activity combines fishing with wildlife viewing as you''ll see hippos, crocodiles, and birds while waiting for bites. Photography of catches before release is encouraged.',
12, 6,
'Hat, sunscreen, sunglasses, camera, water bottle, light snacks. Fishing equipment provided or bring your own.',
'Year-round; Best: June-October (clearer water, fish more active)',
NULL,
'Fishing, Catch and Release, Conservation, Lake Manyara, Peaceful, Recreation',
'Use only barbless hooks. Release all fish carefully and immediately. Do not fish near hippos or crocodiles. Follow guide instructions regarding safe fishing locations. Wear sunscreen and stay hydrated.',
TRUE),

-- =====================================================================
-- HIKING & TREKKING ACTIVITIES
-- =====================================================================

('Mountain Trekking', 'mountain-trekking', TRUE, TRUE, 'PER_PERSON',
'Challenge yourself with guided treks through mountain forests and highlands, including routes on Kilimanjaro and surrounding peaks.',
'Mountain trekking in Tanzania ranges from day hikes to multi-day expeditions. While Kilimanjaro summit attempts require separate specialized permits, this activity covers trekking on Kilimanjaro''s lower slopes, Mount Meru, the Usambara Mountains, and Udzungwa Mountains. Guided treks typically last 4-8 hours and take you through diverse ecosystems - montane forests with colobus monkeys, bamboo zones, heath, and moorland. You''ll experience dramatic elevation changes, waterfalls, unique flora including giant lobelias, and stunning panoramic views. Trails vary in difficulty from moderate to challenging, with options for different fitness levels. Guides provide information about geology, plants, animals, and local culture. Some treks include visits to local communities. Porters can carry heavier gear, and camping options are available for multi-day treks. The cooler mountain climate provides relief from lowland heat.',
14, 10,
'Hiking boots with ankle support, layered clothing (temperature varies with altitude), rain jacket, walking poles, backpack, water bottle (2L), snacks, sunscreen, first aid kit',
'Year-round; Best: June-October, January-February (dry seasons, best visibility)',
NULL,
'Trekking, Hiking, Mountains, Kilimanjaro, Mount Meru, Fitness, Adventure, Views',
'Acclimatize properly to altitude. Stay hydrated. Inform guides of any health issues. Walk at a steady, sustainable pace. Use walking poles on steep sections. Be prepared for rapid weather changes. Stay on marked trails.',
TRUE),

('Canopy Walk', 'canopy-walk', TRUE, TRUE, 'PER_PERSON',
'Walk through the forest canopy on suspended bridges, experiencing the treetop ecosystem and stunning views.',
'Canopy walks provide a thrilling perspective on Tanzania''s forests from elevated walkways 20-40 meters above the ground. Located in parks like Udzungwa Mountains, these suspended bridges and platforms allow you to walk through the treetops, experiencing the forest from the monkeys'' perspective. The walkway sways gently, adding to the excitement while remaining completely safe. From this vantage point, you''ll observe different bird species, colobus monkeys, butterflies, and unique plants that thrive in the canopy layer. The experience lasts 1-2 hours and includes educational information about forest ecology, the importance of canopy layers, and conservation. Stunning views extend across the forest to distant mountains. The activity is suitable for most fitness levels, though those with severe fear of heights should consider carefully. Sunrise and sunset canopy walks offer special lighting and increased animal activity.',
8, 15,
'Comfortable walking shoes, light clothing, hat, sunscreen, camera, water bottle, binoculars',
'Year-round; Best: June-October (clearer weather, better views)',
NULL,
'Canopy Walk, Forest, Heights, Adventure, Monkeys, Birdwatching, Udzungwa, Views',
'Follow guide instructions. Hold handrails at all times. Walk carefully on bridges. Do not jump or rock the walkway. Maximum weight limits apply. Not suitable for those with extreme fear of heights or serious balance issues.',
TRUE),

('Nature Hiking', 'nature-hiking', TRUE, TRUE, 'PER_PERSON',
'Explore diverse ecosystems on guided nature hikes, from coastal forests to highland trails.',
'Nature hiking offers immersive experiences through Tanzania''s varied ecosystems beyond game viewing. These guided walks, typically 2-4 hours, focus on understanding the intricate relationships between plants, animals, and environment. You''ll visit waterfalls, crater lakes, hot springs, caves, and unique geological formations. Guides point out medicinal plants, edible fruits, animal tracks, and explain traditional uses of natural resources. Hikes might include visiting archaeological sites, rock paintings, or historical locations. The pace is leisurely with frequent stops for observations, photography, and explanations. Popular locations include Lake Manyara''s escarpment trails, Arusha National Park''s forest paths, and Mahale Mountains'' chimp trekking routes. You''ll develop tracking skills, learn bird calls, and understand ecosystem dynamics. Some hikes offer cultural elements, visiting local communities or farms.',
8, 12,
'Comfortable hiking shoes, light breathable clothing, hat, sunscreen, insect repellent, camera, water bottle, small backpack',
'Year-round; Best: June-October (dry trails, pleasant weather)',
NULL,
'Hiking, Nature, Waterfalls, Ecosystem, Education, Photography, Forest, Culture',
'Stay with the group. Inform guide of any fitness limitations. Bring adequate water. Wear appropriate footwear to prevent slips. Be aware of insects and plants that may cause irritation. Follow safety briefings.',
TRUE),

-- =====================================================================
-- CULTURAL & EDUCATIONAL ACTIVITIES
-- =====================================================================

('Cultural Village Tours', 'cultural-village-tours', TRUE, TRUE, 'PER_GROUP',
'Experience authentic Tanzanian culture through visits to Maasai, Hadzabe, Datoga, and other traditional communities.',
'Cultural village tours provide meaningful interactions with Tanzania''s indigenous communities, offering insights into traditional lifestyles that have endured for centuries. Maasai village visits are most common, where you''ll participate in traditional dances, learn about cattle herding, visit homes (bomas), observe traditional medicine practices, and purchase authentic crafts. Hadzabe visits with the last remaining hunter-gatherer tribe include demonstrations of bow-making, traditional hunting techniques, and gathering honey. Datoga communities showcase metalworking and jewelry-making. Tours last 2-3 hours and are conducted respectfully with community consent, ensuring tourism benefits local people directly. You''ll learn about social structures, ceremonies, traditional clothing, food preparation, and challenges of maintaining culture in modern times. Photography is usually permitted, and guides facilitate communication. These experiences foster cultural understanding and support community development.',
6, 15,
'Respectful clothing (covering shoulders and knees), camera, cash for purchasing crafts, water bottle, hat',
'Year-round',
NULL,
'Culture, Maasai, Hadzabe, Datoga, Traditional, Community, Crafts, Dance, Education',
'Dress respectfully. Ask permission before photographing people. Do not give children candy or money directly. Purchase crafts to support the community. Listen attentively and show respect for different customs. Follow guide instructions regarding appropriate behavior.',
TRUE),

('Olduvai Gorge Visit', 'olduvai-gorge-visit', TRUE, TRUE, 'PER_PERSON',
'Explore the "Cradle of Mankind" where some of the earliest human ancestor fossils were discovered.',
'Olduvai Gorge, often called the "Cradle of Mankind," is one of the world''s most important paleoanthropological sites. Located in the Ngorongoro Conservation Area, this steep-sided ravine has yielded fossils and stone tools dating back 2 million years, providing crucial evidence of human evolution. Your visit includes the small museum displaying replica fossils, stone tools, and information about Louis and Mary Leakey''s groundbreaking discoveries. Expert guides explain the significance of findings including Homo habilis, Australopithecus boisei, and various animal fossils. You''ll walk along the gorge rim, viewing the different geological layers representing millions of years of history. The site offers spectacular views and fascinating insights into human origins. The visit typically lasts 1.5-2 hours and combines education with stunning landscapes. Ongoing excavations occasionally add new discoveries.',
8, 30,
'Comfortable walking shoes, hat, sunscreen, camera, water bottle, notebook for educational information',
'Year-round',
NULL,
'Archaeology, Human Evolution, Olduvai, Leakey, History, Education, Science, Fossils',
'Stay on designated paths. Do not remove any rocks or materials. Follow museum rules regarding photography. Bring sun protection as there is limited shade. Respect the archaeological significance of the site.',
TRUE),

('Laetoli Footprints Visit', 'laetoli-footprints-visit', TRUE, TRUE, 'PER_PERSON',
'Visit the site of 3.6 million-year-old hominid footprints preserved in volcanic ash.',
'The Laetoli footprints are among the most remarkable paleoanthropological discoveries ever made. Preserved in volcanic ash 3.6 million years ago, these footprints provide definitive evidence that early human ancestors walked upright. Located near Olduvai Gorge, visits to the actual footprint site require special arrangements and are limited to preserve the delicate fossil evidence. Most visitors view detailed replicas at the Olduvai Museum and learn about the discovery''s significance through expert presentations. The footprints were made by three individuals - possibly a family group - walking across wet volcanic ash after an eruption. When the ash hardened, it preserved their footsteps for millions of years. The discovery revolutionized understanding of human evolution, proving bipedalism evolved earlier than previously thought. Guides explain the scientific methods used to date and interpret the footprints.',
10, 20,
'Comfortable walking shoes, hat, sunscreen, camera, water bottle, interest in human evolution',
'Year-round (museum); Special site visits by arrangement only',
NULL,
'Archaeology, Human Evolution, Footprints, Science, Education, Fossils, History',
'Follow all conservation guidelines. Photography may be restricted at the actual site. Do not touch any fossil material. Respect the scientific importance of the location. Listen to educational briefings.',
TRUE),

-- =====================================================================
-- SPECIALIZED WILDLIFE ACTIVITIES
-- =====================================================================

('Chimpanzee Trekking', 'chimpanzee-trekking', TRUE, TRUE, 'PER_PERSON',
'Track and observe wild chimpanzees in their natural rainforest habitat in Mahale Mountains or Gombe Stream.',
'Chimpanzee trekking is one of Tanzania''s most extraordinary wildlife experiences, available in Mahale Mountains and Gombe Stream National Parks. After a briefing on safety and behavior protocols, you''ll trek through dense rainforest with experienced guides and trackers who locate chimp groups using knowledge of their territories and feeding patterns. Treks can last 2-6 hours depending on chimp locations. Once found, you''ll spend up to one hour observing these remarkable primates - our closest genetic relatives sharing 98.7% of our DNA. Watch them feed, groom each other, play, rest, and interact in complex social dynamics. You might see dominant males asserting authority, mothers caring for infants, or youngsters playing. The forest is also home to red colobus monkeys, blue monkeys, forest birds, and unique plants. Gombe is famous as Jane Goodall''s research site. The experience is deeply moving and provides profound insights into primate behavior.',
15, 6,
'Sturdy hiking boots, long trousers (protect against nettles), long-sleeved shirt, rain jacket, gardening gloves (for grabbing vegetation), water bottle, small backpack',
'Year-round; Best: May-October (drier, easier trekking)',
NULL,
'Chimpanzee, Primates, Mahale, Gombe, Jane Goodall, Rainforest, Wildlife, Tracking',
'Maintain 10-meter distance from chimps. Do not eat or drink near chimps. Turn away and cover mouth if you cough or sneeze. Do not use flash photography. Keep voices low. Follow guide instructions immediately. Not suitable for those with contagious illnesses. Minimum age restrictions apply.',
TRUE),

('Rhino Tracking', 'rhino-tracking', TRUE, TRUE, 'PER_PERSON',
'Participate in conservation efforts by tracking black rhinos on foot in protected sanctuaries.',
'Rhino tracking offers a unique opportunity to observe critically endangered black rhinos while supporting conservation efforts. Available in specially protected areas like the Ngorongoro Conservation Area''s crater highlands, this activity involves trekking with armed rangers to locate and observe rhinos from a safe distance. Black rhinos are extremely rare with fewer than 5,500 remaining worldwide, making this experience particularly special. The trek typically lasts 3-4 hours through varied terrain. Rangers use tracking skills to find rhinos based on footprints, dung, feeding signs, and radio telemetry data from collared individuals. Once located, you''ll observe these magnificent but temperamental animals from 50-100 meters, learning about their behavior, conservation challenges, and anti-poaching efforts. Your participation fees directly support rhino protection programs. The experience provides insights into modern conservation, the challenges of protecting endangered species, and the importance of ecotourism in funding conservation.',
16, 6,
'Sturdy hiking boots, neutral-colored clothing, hat, sunscreen, binoculars, camera with telephoto lens, water bottle',
'Year-round; Success rate varies by season',
NULL,
'Rhino, Conservation, Endangered Species, Tracking, Wildlife, Protection, Ngorongoro',
'Maintain absolute silence when near rhinos. Follow ranger instructions immediately. Be prepared to retreat quickly if needed. Rhinos have poor eyesight but excellent smell and hearing. Keep downwind. Do not approach closer than instructed. Rhinos can charge without warning.',
TRUE),

('Bird Watching Safaris', 'bird-watching-safaris', TRUE, TRUE, 'PER_PERSON',
'Discover Tanzania''s incredible avian diversity with over 1,100 species including numerous endemic and migratory birds.',
'Tanzania is a premier birding destination with over 1,100 recorded species, including 33 endemics. Specialized birding safaris focus on identifying and observing this remarkable diversity across various habitats - savanna, forest, wetlands, and mountains. Led by expert birding guides with comprehensive species knowledge, these safaris last 3-6 hours and target specific habitats for particular species. You''ll observe spectacular birds including the lilac-breasted roller, secretary bird, African fish eagle, various hornbills, weavers, and starlings. Special areas include Lake Manyara for flamingos and water birds, Tarangire for endemic species, Udzungwa Mountains for forest specialists, and coastal areas for migrants. Morning sessions (6:00-10:00 AM) are most productive when birds are most active. Guides help identify species by sight and call, explaining behaviors, feeding strategies, and ecological roles. Photography opportunities are excellent. Both serious birders building life lists and casual nature lovers enjoy these specialized experiences.',
10, 8,
'Binoculars (essential), bird field guide, camera with telephoto lens, notebook, hat, sunscreen, insect repellent, water bottle',
'Year-round; Palearctic migrants present November-April; Resident breeders best October-April',
NULL,
'Birdwatching, Ornithology, Endemic Species, Flamingos, Nature, Photography, Conservation',
'Move slowly and quietly. Avoid sudden movements. Wear neutral colors. Follow guide instructions to avoid disturbing nesting sites. Stay hydrated. Use insect repellent in forested areas.',
TRUE),

-- =====================================================================
-- ADVENTURE & RECREATION ACTIVITIES
-- =====================================================================

('Cycling Safaris', 'cycling-safaris', TRUE, TRUE, 'PER_PERSON',
'Explore parks and conservation areas by mountain bike, combining exercise with wildlife viewing.',
'Cycling safaris offer an adventurous and eco-friendly way to experience Tanzania''s wilderness. Available in select areas including Arusha National Park, Lake Manyara surroundings, and some conservancies, these guided rides typically cover 15-30 kilometers over 3-4 hours. Mountain bikes suitable for rough terrain are provided. Routes are chosen to balance adventure with safety, avoiding areas with dangerous predators. You''ll cycle through varied landscapes - grasslands, forests, alongside lakes - seeing zebras, giraffes, antelopes, and numerous bird species. The physical activity enhances the sense of adventure while the quiet nature of cycling allows closer approaches to some wildlife. Guides ensure safety and provide information about the environment. Rides are paced according to group fitness levels with rest stops at scenic viewpoints. Some operators offer multi-day cycling safaris with camping. This activity appeals to active travelers wanting to combine fitness with nature exploration.',
14, 10,
'Comfortable cycling clothes, closed sports shoes, helmet (provided), sunscreen, sunglasses, water bottle, small backpack, camera',
'Year-round; Best: June-October (dry trails, cooler temperatures)',
NULL,
'Cycling, Mountain Biking, Adventure, Fitness, Eco-Tourism, Active Safari',
'Wear provided helmet at all times. Stay with the group. Follow guide instructions regarding wildlife encounters. Maintain safe braking distance. Inform guide of any mechanical issues. Bring adequate water. Know your fitness limits.',
TRUE),

('Horse Riding Safaris', 'horse-riding-safaris', TRUE, TRUE, 'PER_PERSON',
'Experience the African wilderness on horseback, getting close to wildlife in a unique and peaceful way.',
'Horse riding safaris provide a unique perspective on wildlife viewing, available in select conservancies and private concessions near major parks. Well-trained horses accustomed to wildlife allow you to ride among zebras, giraffes, wildebeest, and other herbivores without alarming them, as they perceive you as another large animal rather than a human threat. Rides last 2-4 hours for beginners or full days for experienced riders, traversing diverse terrain from open plains to riverbanks and forests. Professional guides assess riding abilities and match you with appropriate horses. You''ll experience the thrill of cantering across African plains, the serenity of walking through forests, and extraordinary photo opportunities from an elevated vantage point. Sunset rides are particularly magical. Some operators offer multi-day horseback safaris with camping. The activity requires basic riding skills and reasonable fitness. It''s a romantic and adventurous way to connect with nature.',
12, 8,
'Long trousers (preferably riding pants), boots with small heel or provided riding boots, hat with chin strap, sunscreen, sunglasses, camera, water bottle',
'Year-round; Best: June-October (optimal weather)',
NULL,
'Horse Riding, Equestrian, Adventure, Wildlife, Unique Safari, Romance, Photography',
'Wear appropriate riding attire. Follow guide instructions regarding horse handling. Stay with the group. Do not approach predators on horseback. Inform guides of any riding experience and injuries. Minimum riding skill level required.',
TRUE),

('Photography Workshops', 'photography-workshops', TRUE, TRUE, 'PER_PERSON',
'Improve your wildlife photography skills with professional guidance during dedicated photo safaris.',
'Photography workshops combine safari experiences with expert instruction in wildlife photography techniques. Led by professional wildlife photographers, these specialized safaris focus on getting the perfect shot rather than just seeing animals. You''ll learn about camera settings for different lighting conditions, composition techniques, animal behavior prediction, panning for action shots, and post-processing basics. Workshops typically last full or half days and target specific photographic opportunities - predator hunts, bird flights, sunrise/sunset lighting, elephants at waterholes, or the Great Migration. Instructors provide individual coaching on your camera settings and technique. Morning and evening sessions take advantage of golden hour lighting. You''ll learn to anticipate action, frame shots effectively, and tell visual stories. Some workshops include classroom sessions on equipment, software, and editing. Suitable for all skill levels from beginners wanting to maximize their vacation photos to advanced photographers seeking portfolio shots.',
16, 6,
'DSLR or mirrorless camera, telephoto lens (300mm+), wide-angle lens, extra batteries, multiple memory cards, laptop for reviews, bean bag or tripod',
'Year-round; Great Migration timing (July-October) offers spectacular opportunities',
NULL,
'Photography, Wildlife Photography, Workshop, Education, Professional, Great Migration, Portraits',
'Protect camera equipment from dust - bring covers and cleaning supplies. Avoid changing lenses in dusty conditions. Use high shutter speeds for action. Be patient for perfect moments. Respect wildlife - don''t stress animals for photos.',
TRUE),

-- =====================================================================
-- CRATER & GEOLOGICAL ACTIVITIES
-- =====================================================================

('Ngorongoro Crater Descent', 'ngorongoro-crater-descent', TRUE, TRUE, 'PER_VEHICLE',
'Descend into the world''s largest intact volcanic caldera for unparalleled wildlife viewing.',
'The Ngorongoro Crater descent is one of Africa''s most spectacular wildlife experiences. This massive volcanic caldera, 610 meters deep and 260 square kilometers in area, contains the world''s highest density of large mammals. The steep descent takes 30-45 minutes down winding roads with breathtaking views. Once on the crater floor, you''ll spend 5-6 hours game driving through diverse habitats - grasslands, swamps, forests, and Lake Magadi. The crater is home to approximately 25,000 large animals including the Big Five (with particularly good chances of seeing the rare black rhino), large prides of lions, numerous elephants, buffalo herds, hippos, flamingos, and over 500 bird species. The enclosed ecosystem creates reliable wildlife viewing year-round. You''ll see the dramatic crater walls rising all around, creating a natural amphitheater. Picnic lunch is taken at designated sites near hippo pools. The ascent back offers equally stunning views.',
5, 6,
'Warm layers (crater floor can be cool and windy), rain jacket, camera with good zoom, binoculars, picnic lunch, water, sunscreen',
'Year-round; Wildlife concentrated year-round due to enclosed ecosystem',
NULL,
'Ngorongoro, Crater, Big Five, Black Rhino, Caldera, World Heritage, Spectacular Views',
'Stay in vehicle except at designated picnic areas. Follow guide instructions. Do not approach animals closely. Respect distance from black rhinos - they are endangered and temperamental. Be prepared for changeable weather.',
TRUE),

('Shifting Sands Visit', 'shifting-sands-visit', TRUE, TRUE, 'PER_PERSON',
'Witness the mysterious moving sand dunes that slowly travel across the Ngorongoro plains.',
'The Shifting Sands are a unique geological phenomenon in the Ngorongoro Conservation Area. These crescent-shaped barchan dunes, composed of volcanic ash from Ol Doinyo Lengai volcano, mysteriously move across the plains at a rate of about 15-20 meters per year, driven by prevailing winds. The main dune is approximately 9 meters high and 100 meters long. Local Maasai consider them sacred and use their movement to calculate time. Visiting the Shifting Sands combines geology, culture, and stunning landscapes. The excursion typically involves a short walk from the road to the dunes, where you can climb and photograph them against the backdrop of the crater highlands. Guides explain the scientific reasons for the dunes'' formation and movement, as well as cultural significance to local communities. The best photography is during golden hour - sunrise or sunset. The visit can be combined with trips to Olduvai Gorge or crater rim viewpoints.',
6, 25,
'Comfortable walking shoes, hat, sunscreen, sunglasses, camera, water bottle, scarf (for wind protection)',
'Year-round',
NULL,
'Geology, Shifting Sands, Ngorongoro, Maasai Culture, Unique Phenomenon, Photography',
'Watch for wind-blown sand. Protect cameras and eyes during windy conditions. Follow guide instructions regarding where to walk. Respect any cultural ceremonies that may be taking place. Stay hydrated.',
TRUE),

-- =====================================================================
-- EDUCATIONAL & INTERPRETIVE ACTIVITIES
-- =====================================================================

('Bush Meals & Sundowners', 'bush-meals-sundowners', TRUE, TRUE, 'PER_PERSON',
'Enjoy gourmet dining experiences in the wilderness, from bush breakfasts to sundowner cocktails.',
'Bush meals and sundowners transform dining into unforgettable safari experiences. Bush breakfasts after early morning balloon safaris or game drives feature freshly cooked meals under acacia trees with white-tablecloth service in the wilderness. Bush lunches during full-day safaris are enjoyed at scenic spots - beside rivers, under shade trees, overlooking valleys. Sunset sundowners involve stopping at spectacular viewpoints as the sun sets, enjoying cocktails and snacks while watching the golden hour transform the landscape. Some lodges offer candlelit dinners under the stars, complete with traditional dancers, bonfire, and gourmet multi-course meals. These experiences combine exceptional food with spectacular settings and often include wildlife sightings - elephants approaching at dusk, nocturnal animals beginning their activities. Professional staff set up elegant dining areas in remote locations, creating magical moments that epitomize luxury safari experiences. Photography opportunities are superb, especially during golden hour.',
8, 15,
'Light jacket for evening events, camera, binoculars, appropriate dining attire (smart casual)',
'Year-round; Dry season (June-October) most reliable for outdoor events',
NULL,
'Bush Meals, Sundowners, Luxury, Dining, Sunset, Romance, Photography, Gourmet',
'Follow staff instructions regarding wildlife safety. Do not wander from dining areas. Keep food secured. Enjoy responsibly if alcohol is served. Bring warm layers for evening events.',
TRUE),

('Guided Nature Lectures', 'guided-nature-lectures', TRUE, TRUE, 'PER_SESSION',
'Attend educational presentations by expert naturalists covering ecology, conservation, and wildlife behavior.',
'Guided nature lectures provide in-depth education about Tanzania''s ecosystems, wildlife, and conservation challenges. Typically offered at lodges and camps, these sessions last 1-2 hours and cover topics like animal behavior, predator-prey relationships, migration patterns, bird identification, plant ecology, conservation success stories, and current environmental challenges. Expert guides, researchers, or conservation specialists present using visual aids, specimens, maps, and sometimes live demonstrations. Some lodges offer specialized topics - tracking skills workshop, astronomy sessions under African skies, traditional medicine plant walks, or photography technique seminars. Lectures are interactive, encouraging questions and discussions. Many camps offer night lectures about nocturnal wildlife before night drives, or morning briefings about the day''s game drive strategies. These educational components enhance appreciation of what you''re seeing and understanding of conservation importance. Particularly valuable for families with children or guests wanting deeper knowledge beyond sightseeing.',
8, 30,
'Notebook, camera for presentation materials, curiosity and questions',
'Year-round; Often scheduled in afternoon/evening',
NULL,
'Education, Conservation, Ecology, Wildlife, Lectures, Learning, Nature, Science',
'Arrive on time. Silence mobile devices. Feel free to ask questions. Take notes if desired. Respect presenter expertise.',
TRUE),

('Junior Ranger Programs', 'junior-ranger-programs', TRUE, TRUE, 'PER_PERSON',
'Educational programs designed specifically for children to learn about wildlife, conservation, and bush skills.',
'Junior Ranger programs offer children aged 6-14 specialized safari experiences focusing on education, adventure, and fun. These programs, offered at select lodges and camps, include age-appropriate activities like animal tracking, plaster casting of footprints, learning bush survival skills, identifying birds and insects, understanding food chains, conservation lessons, and earning certificates. Experienced guides trained in working with children lead activities that balance education with entertainment. Kids might learn to identify animal dung, track creatures through the bush, make fire using traditional methods, learn basic Swahili wildlife terms, or create nature journals documenting their discoveries. Programs often include hands-on conservation activities like tree planting. Activities are designed to instill environmental awareness and respect for nature. Many resorts offer these while parents enjoy adult-focused activities. The programs create lasting memories and often inspire lifelong interest in nature and conservation.',
6, 12,
'Comfortable outdoor clothing, closed shoes, hat, sunscreen, water bottle, notebook and pencils for nature journal',
'Year-round',
NULL,
'Children, Education, Junior Rangers, Conservation, Family, Learning, Adventure, Fun',
'Children must be supervised by parents or program staff. Follow all safety instructions. Stay hydrated. Use sun protection. Respect wildlife. Participate enthusiastically.',
TRUE);

-- =====================================================================
-- Additional specialty activities can be added below
-- =====================================================================

-- Note: The primaryImage field is left as NULL as requested.
-- Images can be added later through application updates or separate SQL update statements.

-- =====================================================================
-- END OF FILE
-- =====================================================================
