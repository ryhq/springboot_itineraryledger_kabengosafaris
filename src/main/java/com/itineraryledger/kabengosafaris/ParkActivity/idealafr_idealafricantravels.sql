-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Dec 31, 2025 at 11:57 PM
-- Server version: 8.0.44
-- PHP Version: 8.4.16

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `idealafr_idealafricantravels`
--

-- --------------------------------------------------------

--
-- Table structure for table `activity`
--

CREATE TABLE `activity` (
  `activity_id` bigint NOT NULL,
  `activity_description` longtext,
  `activity_name` varchar(64) NOT NULL,
  `charging_basis` enum('PER_GROUP','PER_PERSON','PER_VEHICLE') DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `has_tariff` tinyint(1) DEFAULT '0',
  `is_web_active` tinyint(1) DEFAULT '0',
  `updated_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `activity`
--

INSERT INTO `activity` (`activity_id`, `activity_description`, `activity_name`, `charging_basis`, `created_at`, `has_tariff`, `is_web_active`, `updated_at`) VALUES
(1, 'The \"Entrance\" activity often includes access to various amenities and the ability to experience the park\'s offerings, such as game drives, walking safaris, or simply enjoying the natural scenery and wildlife.', 'Entrance', 'PER_PERSON', '2025-05-08 12:54:18', 1, 0, '2025-09-30 12:52:27.698617'),
(2, 'A guided nature walk that allows visitors to explore the park on foot, offering a close-up experience with the environment, flora, and fauna. Provides an opportunity to learn about the ecosystem and track animals with the help of experienced guides or rangers.', 'Walking Safari', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:49:56.877779'),
(3, 'These are charges associated with hiring a park ranger or guide to accompany visitors during activities such as game drives or walking safaris. Rangers provide safety, guidance, and expert knowledge about the park’s wildlife and conservation efforts.', 'Ranger', 'PER_PERSON', '2025-05-08 12:54:18', 1, 0, '2025-05-28 13:29:46.000000'),
(4, 'A unique dining experience where visitors enjoy meals in a scenic outdoor setting within the park, often surrounded by wildlife. It’s an excellent way to combine gastronomy with the natural beauty of the park.', 'Bush Meals', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:12.045155'),
(5, 'An adventurous activity that involves walking on elevated pathways or bridges through the forest canopy. This offers a bird’s-eye view of the park, allowing visitors to appreciate the landscape, observe treetop wildlife, and experience the park from a different perspective.', 'Canopy Walk', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:14.677461'),
(6, 'A thrilling activity that takes place after sunset, allowing visitors to explore the park and observe nocturnal wildlife. Specially equipped vehicles with spotlights are used to enhance visibility, offering a chance to see species like leopards, hyenas, and owls that are more active at night.', 'Night Game Drive', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-05-28 13:30:29.000000'),
(7, 'A water-based activity where visitors paddle through rivers or lakes within the park. It’s a peaceful way to explore aquatic ecosystems, observe water birds, and sometimes spot hippos or crocodiles from a safe distance.', 'Canoeing', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:17.832612'),
(8, 'An activity involving professional or amateur filming within the park for documentaries, personal projects, or other purposes, often requiring permits.', 'Filming', 'PER_PERSON', '2025-05-08 12:54:18', 1, 0, '2025-05-28 13:30:35.000000'),
(9, 'A general category that includes any other activities not explicitly listed, which may vary depending on the park.', 'Others', 'PER_PERSON', '2025-05-08 12:54:18', 1, 0, '2025-05-28 13:30:39.000000'),
(10, 'A recreational fishing activity conducted in designated areas within the park, often involving catch-and-release fishing and requiring permits to protect the ecosystem.', 'Sport Fishing', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:20.205928'),
(11, 'An adventurous activity that allows visitors to explore designated areas of the park on horseback, offering a unique and tranquil way to experience the park\'s landscapes and wildlife.', 'Horse Riding', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:21.365571'),
(12, 'A thrilling outdoor activity that lets visitors explore park trails and enjoy scenic views while cycling through diverse terrains, providing a unique combination of adventure and exercise.', 'Cycling', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:23.228695'),
(13, 'A relaxing water-based activity that allows visitors to explore the park\'s lakes or rivers by boat, offering stunning views of the surrounding landscapes and opportunities to observe aquatic wildlife.', 'Boating', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:25.494716'),
(14, 'A unique wildlife experience that allows visitors to observe endangered rhinos in their natural habitat. This activity promotes conservation awareness and offers a chance to learn about rhino protection efforts while enjoying a close encounter with these magnificent creatures.', 'Rhino Viewing', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:27.989403'),
(15, 'The Crater Service fee is required for vehicles entering the Ngorongoro Crater, one of Tanzania\'s most renowned attractions. The crater, the world\'s largest unbroken caldera, offers a diverse range of activities such as game drives to spot the Big Five, nature walks through scenic landscapes, birdwatching to observe rare species, hiking along the crater rim, visiting the historic Olduvai Gorge, and engaging in cultural experiences with the Maasai people and other local tribes.', 'Crater Service', 'PER_VEHICLE', '2025-05-08 12:54:18', 1, 0, '2025-05-28 13:36:25.000000'),
(16, 'The \'Sun Down\' activity allows visitors to enjoy breathtaking views of the sunset, especially magical at Lake Magadi. The lake\'s reflective surface beautifully mirrors the sun\'s rays, creating an ethereal glow that illuminates the surrounding wildlife. The experience includes game drives, offering incredible opportunities to witness animals against the backdrop of a golden horizon.', 'Sun Down', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:45.010198'),
(17, 'Hot air balloon rides provide a one-of-a-kind opportunity to glide over Tanzania\'s vast and stunning landscapes. These rides offer unparalleled views of the Serengeti plains and their abundant wildlife, creating a serene and unforgettable way to connect with nature from above.', 'Hot Air Balloon', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:49.109906'),
(18, 'Olduvai Gorge, often referred to as the \'Cradle of Mankind,\' is one of the most significant archaeological sites in the world. It offers visitors a chance to explore the area where some of the earliest human ancestors\' fossils were discovered, providing an unparalleled glimpse into our evolutionary history.', 'Olduvai Gorge', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:53.500951'),
(19, 'The Shifting Sand is a fascinating natural phenomenon found in the Ngorongoro Conservation Area. These crescent-shaped dunes, formed from volcanic ash, mysteriously move across the plains over time, guided by prevailing winds. Visitors can marvel at this unique spectacle and learn about the geological forces shaping the landscape.', 'Shifting Sand', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:50:58.102059'),
(20, 'The Laetoli Footprints are among the most remarkable discoveries in paleoanthropology. Preserved in volcanic ash over 3.6 million years ago, these footprints provide evidence of early human ancestors walking upright. A visit to the site offers an extraordinary connection to the distant past and insights into human evolution.', 'Laetoli Footprints', 'PER_PERSON', '2025-05-08 12:54:18', 1, 1, '2025-10-19 12:51:00.655997'),
(21, 'Educational talks led by expert guides or park rangers, providing insights into the park’s ecology, conservation efforts, and cultural significance. These sessions are often interactive and tailored to the interests of visitors.', 'Guided Lectures', 'PER_PERSON', '2025-05-08 12:54:18', 1, 0, '2025-09-09 14:14:04.000000'),
(22, 'Tanzania’s parks are home to hundreds of bird species, making it a paradise for bird enthusiasts. Visitors can enjoy spotting colorful birds like flamingos, hornbills, and kingfishers in their natural habitats.', 'Bird Watching', NULL, '2025-05-08 12:54:18', 0, 1, '2025-10-19 12:51:02.947892'),
(23, 'Visitors can engage with local communities, such as the Maasai, Hadzabe, or Datoga tribes, to learn about their traditions, customs, and daily life. Activities often include dance performances, storytelling, and visits to traditional villages.', 'Cultural Tours', NULL, '2025-05-08 12:54:18', 0, 1, '2025-10-19 12:51:03.879726'),
(24, 'A specialized activity for photography enthusiasts, focusing on capturing the stunning landscapes, wildlife, and unique moments in the park. These safaris often involve expert guidance to find the best locations and angles.', 'Photography Safaris', NULL, '2025-05-08 12:54:18', 0, 1, '2025-10-19 12:51:11.426757'),
(25, 'Certain parks with rugged landscapes, like Udzungwa Mountains National Park, offer rock climbing opportunities for adventurous visitors.', 'Rock Climbing', NULL, '2025-05-08 12:54:18', 0, 1, '2025-10-19 12:51:14.902630'),
(26, 'A guided activity where visitors track specific animals, such as lions or elephants, with the help of expert trackers, providing an intimate understanding of wildlife behavior.', 'Wildlife Tracking', NULL, '2025-05-08 12:54:19', 0, 1, '2025-10-19 12:51:16.856112');

-- --------------------------------------------------------

--
-- Table structure for table `park`
--

CREATE TABLE `park` (
  `park_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `park_description` longtext,
  `park_location` varchar(255) NOT NULL,
  `park_name` varchar(64) NOT NULL,
  `park_size` double DEFAULT '0',
  `updated_at` datetime(6) NOT NULL,
  `district_code` int DEFAULT NULL,
  `place_code` bigint DEFAULT NULL,
  `region_code` int NOT NULL,
  `street_code` bigint DEFAULT NULL,
  `ward_code` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `park`
--

INSERT INTO `park` (`park_id`, `created_at`, `park_description`, `park_location`, `park_name`, `park_size`, `updated_at`, `district_code`, `place_code`, `region_code`, `street_code`, `ward_code`) VALUES
(1, '2025-05-08 11:51:09', 'Famous for its annual migration of over 1.5 million white-bearded wildebeest and 250,000 zebras.', 'MARA, SERENGETI', 'Serengeti National Park', 14763, '2025-07-28 14:50:01.000000', 316, NULL, 31, NULL, NULL),
(2, '2025-05-08 11:52:23', 'A UNESCO World Heritage Site, home to the Ngorongoro Crater and abundant wildlife.', 'ARUSHA, NGORONGORO', 'Ngorongoro Conservation Area', 8292, '2025-05-08 11:52:23.000000', 237, NULL, 23, NULL, NULL),
(3, '2025-05-08 11:53:17', 'Known for its elephant herds and baobab trees, with a river that attracts many animals during the dry season.', 'MANYARA', 'Tarangire National Park', 2850, '2025-05-08 11:53:17.000000', NULL, NULL, 27, NULL, NULL),
(4, '2025-05-08 11:54:20', 'Famous for its tree-climbing lions and flocks of flamingos that feed on the algae in the lake.', 'MANYARA', 'Lake Manyara National Park', 325, '2025-05-08 11:54:20.000000', NULL, NULL, 27, NULL, NULL),
(5, '2025-05-08 11:56:53', 'The largest national park in Tanzania, known for its large elephant populations and diverse wildlife. Its situated in central to southern Tanzania, approximately 130 kilometers west of Iringa. Covering an area of about 20,226 square kilometers, it stands as the largest national park in Tanzania and one of the largest in East Africa.\n\nThe park derives its name from the Great Ruaha River, which flows along its southeastern boundary and serves as a vital water source for the diverse wildlife inhabiting the area . Ruaha is renowned for its rich biodiversity, including significant populations of elephants, lions, leopards, cheetahs, African wild dogs, and over 570 bird species.', 'IRINGA', 'Ruaha National Park', 20226, '2025-05-08 11:57:55.000000', NULL, NULL, 51, NULL, NULL),
(6, '2025-05-08 12:00:41', 'Located near Morogoro, approximately 283 kilometers (175 miles) west of Dar es Salaam, along the Dar-Mbeya highway.\n\nCovers an area of 3,230 square kilometers (1,250 square miles), making it the fourth-largest national park in Tanzania.\n\nBorders the Selous Game Reserve to the south and is flanked by the Uluguru, Rubeho, and Udzungwa mountain ranges, creating a diverse ecological zone\n\nFeatures a variety of wildlife including elephants, giraffes, zebras, wildebeests, lions, and over 400 bird species. The Mkata Floodplain is particularly renowned for wildlife viewing', 'MOROGORO', 'Mikumi National Park', 3230, '2025-07-30 16:31:27.000000', NULL, NULL, 67, NULL, NULL),
(7, '2025-05-08 12:01:56', 'A remote park known for its large hippo and crocodile populations and vast plains.', 'KATAVI', 'Katavi National Park', 4471, '2025-07-29 16:46:09.000000', NULL, NULL, 50, NULL, NULL),
(8, '2025-05-08 12:19:26', 'One of the largest faunal reserves in the world, home to a variety of species, including elephants, rhinos, and lions.', 'MOROGORO', 'Selous Game Reserve', 54600, '2025-05-08 12:19:26.000000', NULL, NULL, 67, NULL, NULL),
(9, '2025-05-08 12:20:22', 'A scenic national park known for its diverse landscapes, including Mount Meru, lush forests, and Momella Lakes, as well as its rich wildlife such as giraffes, buffaloes, and flamingos.', 'ARUSHA, ARUMERU', 'Arusha National Park', 552, '2025-05-08 12:20:22.000000', 233, NULL, 23, NULL, NULL),
(10, '2025-05-08 12:21:30', 'Renowned for its chimpanzee populations and the research conducted by Dr. Jane Goodall.', 'KIGOMA', 'Gombe National Park', 56, '2025-05-08 12:21:30.000000', NULL, NULL, 47, NULL, NULL),
(11, '2025-05-08 12:22:16', 'One of the newest parks in Tanzania, featuring a mix of woodland, lakes, and savanna, home to diverse wildlife.', 'KAGERA', 'Burigi-Chato National Park', 4707, '2025-05-08 12:22:16.000000', NULL, NULL, 35, NULL, NULL),
(12, '2025-05-08 12:22:47', 'Home to Mount Kilimanjaro, Africa\'s highest peak, and a popular destination for trekkers and climbers.', 'KILIMANJARO', 'Kilimanjaro National Park', 1668, '2025-05-08 12:22:47.000000', NULL, NULL, 25, NULL, NULL),
(13, '2025-05-08 12:23:47', 'A pristine park known for its remote location, crystal-clear lakes, and chimpanzee tracking opportunities.', 'KIGOMA', 'Mahale Mountains National Park', 1613, '2025-05-08 12:23:47.000000', NULL, NULL, 47, NULL, NULL),
(14, '2025-05-08 12:24:29', 'A vital sanctuary for the endangered black rhino and African wild dog.', 'KILIMANJARO', 'Mkomazi National Park', 3245, '2025-05-08 12:24:29.000000', NULL, NULL, 25, NULL, NULL),
(15, '2025-05-08 12:40:39', 'An island park offering pristine wildlife and bird-watching experiences on Lake Victoria.', 'MWANZA', 'Rubondo Island National Park', 456.8, '2025-05-08 12:40:39.000000', NULL, NULL, 33, NULL, NULL),
(16, '2025-05-08 12:41:10', 'The only park in Tanzania with a coastline, offering a unique mix of marine and terrestrial wildlife.', 'TANGA', 'Saadani National Park', 1062, '2025-05-08 12:41:10.000000', NULL, NULL, 21, NULL, NULL),
(17, '2025-05-08 12:41:49', 'Known for its hiking trails and unique biodiversity, including endemic primates and bird species.', 'MOROGORO', 'Udzungwa Mountains National Park', 1990, '2025-05-08 12:41:49.000000', NULL, NULL, 67, NULL, NULL),
(18, '2025-05-08 12:42:33', 'A small island park on Lake Victoria offering scenic views, bird watching, and recreational activities.', 'MWANZA', 'Saanane National Park', 2.18, '2025-05-08 12:42:33.000000', NULL, NULL, 33, NULL, NULL),
(19, '2025-05-08 12:43:07', 'The park is known for its scenic beauty, which includes rolling hills, valleys, plains, seasonal rivers, lakes, and swamps. The park is also home to a variety of wildlife, including hippos, antelopes, Thomson gazelles, impalas, elands, baboons, African buffalo, leopards, and a variety of bird species.', 'KAGERA', 'Ibanda-Kyerwa National Park', 2.18, '2025-05-08 12:43:07.000000', NULL, NULL, 35, NULL, NULL),
(20, '2025-05-08 12:44:28', 'Also known as, \"The Garden of God\" by locals, \"The Serengeti of Flowers\" by botanists. Its Unique flora, including 350 species of vascular plants and 45 types of terrestrial orchids', 'MBEYA', 'Kitulo National Park', 412.9, '2025-05-08 12:44:28.000000', NULL, NULL, 53, NULL, NULL),
(21, '2025-05-08 12:47:26', 'Nyerere National Park is located in southeastern Tanzania and is part of a larger ecosystem that includes the Selous Game Reserve. The park is home to a wide variety of wildlife, including the Big Five (elephants, buffalos, lions, and leopards), as well as other large game animals like giraffes, rhinos, and zebras. The park\'s waterways, including the Rufiji River, are a popular destination for boat safaris.\n\nNyerere National Park is located in southeastern Tanzania and primarily falls within the Lindi and Morogoro Regions. Located primarily in the Morogoro Region, which accounts for the highest percentage of the park’s area.\n\n', 'MOROGORO', 'Nyerere National Park', 30893, '2025-05-08 12:47:26.000000', NULL, NULL, 67, NULL, NULL),
(22, '2025-05-08 12:48:20', 'The park was established in 2019 and is located in the Kagera region of northwest Tanzania, next to Lake Victoria. It\'s a popular destination for safari tours and is home to a variety of animals, including:- Lions, Leopards, Elephants, Hippopotamuses, Giraffes, Buffalo, and Antelopes. The park also has a deep valley with a slow-moving river that divides into lakes and bogs. The lower slopes of the valley are covered with forest and thick bush, while the higher slopes have acacia tall grass woodlands.', 'KAGERA', 'Rumanyika-Karagwe National Park', 247, '2025-05-08 12:48:20.000000', NULL, NULL, 35, NULL, NULL),
(23, '2025-05-08 12:50:04', 'Ugalla River National Park, was established in 2019 and protects a wetland area that was previously used for big-game hunting. The park is home to many species of birds, reptiles, and monkey, and is considered a conservation success.', 'TABORA', 'Ugalla River National Park', 3865, '2025-05-08 12:50:04.000000', NULL, NULL, 45, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `park_activity`
--

CREATE TABLE `park_activity` (
  `activity_id` bigint NOT NULL,
  `park_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `park_activity`
--

INSERT INTO `park_activity` (`activity_id`, `park_id`) VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(8, 1),
(9, 1),
(1, 2),
(4, 2),
(11, 2),
(15, 2),
(16, 2),
(17, 2),
(18, 2),
(19, 2),
(20, 2),
(1, 3),
(2, 3),
(3, 3),
(4, 3),
(6, 3),
(8, 3),
(9, 3),
(1, 4),
(2, 4),
(3, 4),
(4, 4),
(5, 4),
(6, 4),
(7, 4),
(8, 4),
(9, 4),
(10, 4),
(1, 5),
(2, 5),
(3, 5),
(4, 5),
(6, 5),
(8, 5),
(9, 5),
(10, 5),
(13, 5),
(1, 6),
(2, 6),
(3, 6),
(4, 6),
(6, 6),
(8, 6),
(9, 6),
(1, 7),
(2, 7),
(3, 7),
(4, 7),
(6, 7),
(8, 7),
(9, 7),
(1, 9),
(2, 9),
(3, 9),
(4, 9),
(7, 9),
(8, 9),
(9, 9),
(11, 9),
(12, 9),
(1, 10),
(2, 10),
(3, 10),
(4, 10),
(8, 10),
(9, 10),
(10, 10),
(13, 10),
(1, 11),
(2, 11),
(3, 11),
(4, 11),
(6, 11),
(7, 11),
(8, 11),
(9, 11),
(10, 11),
(13, 11),
(1, 12),
(12, 12),
(1, 13),
(2, 13),
(3, 13),
(4, 13),
(7, 13),
(8, 13),
(9, 13),
(10, 13),
(13, 13),
(1, 14),
(2, 14),
(3, 14),
(4, 14),
(6, 14),
(8, 14),
(9, 14),
(14, 14),
(1, 15),
(2, 15),
(3, 15),
(4, 15),
(7, 15),
(8, 15),
(9, 15),
(10, 15),
(13, 15),
(1, 16),
(2, 16),
(3, 16),
(4, 16),
(6, 16),
(8, 16),
(9, 16),
(10, 16),
(12, 16),
(13, 16),
(1, 17),
(2, 17),
(3, 17),
(4, 17),
(8, 17),
(9, 17),
(1, 18),
(2, 18),
(3, 18),
(4, 18),
(8, 18),
(9, 18),
(10, 18),
(1, 19),
(2, 19),
(3, 19),
(4, 19),
(6, 19),
(8, 19),
(9, 19),
(10, 19),
(1, 20),
(2, 20),
(3, 20),
(4, 20),
(8, 20),
(9, 20),
(1, 21),
(2, 21),
(3, 21),
(4, 21),
(6, 21),
(7, 21),
(8, 21),
(9, 21),
(10, 21),
(13, 21),
(1, 22),
(2, 22),
(3, 22),
(4, 22),
(7, 22),
(8, 22),
(9, 22),
(10, 22),
(13, 22),
(1, 23),
(2, 23),
(3, 23),
(4, 23),
(6, 23),
(8, 23),
(9, 23),
(10, 23),
(12, 23),
(13, 23);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `activity`
--
ALTER TABLE `activity`
  ADD PRIMARY KEY (`activity_id`),
  ADD UNIQUE KEY `UK5r4i41tgx3i6b91p5wvsmfqvp` (`activity_name`);

--
-- Indexes for table `park`
--
ALTER TABLE `park`
  ADD PRIMARY KEY (`park_id`),
  ADD KEY `FK4ranfi4q3t3o37r0aosbsow1g` (`district_code`),
  ADD KEY `FKf8wqnyta6bf20jrdwm54rtqts` (`place_code`),
  ADD KEY `FK9n5fs2hou7xkaetbv84dqb7kx` (`region_code`),
  ADD KEY `FK2paaw4e2shfujqnvvoffpq2f4` (`street_code`),
  ADD KEY `FKsxqu38yjgtse2towvuidsnkxv` (`ward_code`);

--
-- Indexes for table `park_activity`
--
ALTER TABLE `park_activity`
  ADD PRIMARY KEY (`activity_id`,`park_id`),
  ADD KEY `FKmotk0y6d1kif4qi438m5jshpw` (`park_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activity`
--
ALTER TABLE `activity`
  MODIFY `activity_id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT for table `park`
--
ALTER TABLE `park`
  MODIFY `park_id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `park`
--
ALTER TABLE `park`
  ADD CONSTRAINT `FK2paaw4e2shfujqnvvoffpq2f4` FOREIGN KEY (`street_code`) REFERENCES `streets` (`id`),
  ADD CONSTRAINT `FK4ranfi4q3t3o37r0aosbsow1g` FOREIGN KEY (`district_code`) REFERENCES `districts` (`district_code`),
  ADD CONSTRAINT `FK9n5fs2hou7xkaetbv84dqb7kx` FOREIGN KEY (`region_code`) REFERENCES `regions` (`region_code`),
  ADD CONSTRAINT `FKf8wqnyta6bf20jrdwm54rtqts` FOREIGN KEY (`place_code`) REFERENCES `places` (`id`),
  ADD CONSTRAINT `FKsxqu38yjgtse2towvuidsnkxv` FOREIGN KEY (`ward_code`) REFERENCES `wards` (`ward_code`);

--
-- Constraints for table `park_activity`
--
ALTER TABLE `park_activity`
  ADD CONSTRAINT `FKfrfgvxxe5bbw0jm8kklt3r519` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`activity_id`),
  ADD CONSTRAINT `FKmotk0y6d1kif4qi438m5jshpw` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
