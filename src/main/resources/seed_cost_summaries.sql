-- =============================================
-- Seed cost summaries for all 46 itineraries
-- Realistic per-person USD prices for Non-Resident Adult
-- Based on Tanzania safari market rates by budget tier
-- =============================================
--
-- Pricing basis (per person per day, approximate):
--   BACKPACKER:   $150-200/day (basic camping, shared transport)
--   BUDGET:       $200-280/day (budget lodges, park fees)
--   MID_RANGE:    $300-450/day (mid-range lodges, private vehicle)
--   LUXURY:       $500-750/day (luxury lodges, premium service)
--   ULTRA_LUXURY: $800-1200/day (exclusive camps, fly-in options)
--
-- Kilimanjaro treks: $200-350/day (crew, permits, camping)
-- Zanzibar beach days: $100-400/day depending on tier
-- =============================================

SET @now = NOW();
SET @start_date = CURDATE();

-- Clear existing cost summaries (idempotent re-run)
DELETE FROM itinerary_cost_summaries;

INSERT INTO itinerary_cost_summaries
  (itinerary_id, currency, accommodation_rack, park_fees_rack, activities_rack, grand_total_rack,
   accommodation_sto, park_fees_sto, activities_sto, grand_total_sto,
   has_incomplete_rates, calculated_at, start_date_used)
VALUES
-- =============================================
-- DAY TRIPS (1 day, 0 nights)
-- =============================================

-- #2: Ngorongoro Crater Discovery (MID_RANGE, 1 day)
-- Park fees ~$295 (conservation + crater service), transport/guide ~$150, lunch ~$25
(2, 'USD', 0.00, 295.00, 175.00, 470.00,
             0.00, 250.00, 140.00, 390.00, 0, @now, @start_date),

-- #3: Tarangire Baobab & Elephant Safari (MID_RANGE, 1 day)
-- Park fees ~$78, transport/guide ~$150, lunch ~$25
(3, 'USD', 0.00, 78.00, 172.00, 250.00,
             0.00, 65.00, 135.00, 200.00, 0, @now, @start_date),

-- #4: Lake Manyara Flamingo Safari (BUDGET, 1 day)
-- Park fees ~$53, transport/guide ~$120, lunch ~$20
(4, 'USD', 0.00, 53.00, 137.00, 190.00,
             0.00, 45.00, 105.00, 150.00, 0, @now, @start_date),

-- #5: Arusha Wilderness Family Escape (BUDGET, 1 day)
-- Park fees ~$53, transport/guide ~$120, lunch ~$20
(5, 'USD', 0.00, 53.00, 137.00, 190.00,
             0.00, 45.00, 105.00, 150.00, 0, @now, @start_date),

-- #6: Kilimanjaro Rainforest Trek (BUDGET, 1 day)
-- Park fees ~$70 (hiking permit), guide/crew ~$80, lunch ~$20
(6, 'USD', 0.00, 70.00, 100.00, 170.00,
             0.00, 55.00, 80.00, 135.00, 0, @now, @start_date),

-- #7: Ngorongoro Crater Through the Lens (LUXURY, 1 day)
-- Park fees ~$295, premium guide ~$250, luxury picnic ~$55
(7, 'USD', 0.00, 295.00, 305.00, 600.00,
             0.00, 250.00, 230.00, 480.00, 0, @now, @start_date),

-- #8: Mkomazi Rhino & Wilderness Safari (MID_RANGE, 1 day)
-- Park fees ~$30, transport/guide ~$160, lunch ~$25
(8, 'USD', 0.00, 30.00, 185.00, 215.00,
             0.00, 25.00, 145.00, 170.00, 0, @now, @start_date),

-- #9: Jozani Forest & Spice Island Explorer (MID_RANGE, 1 day)
-- Park fees ~$18 (Jozani), spice tour ~$35, transport ~$50, lunch ~$20
(9, 'USD', 0.00, 18.00, 107.00, 125.00,
             0.00, 15.00, 85.00, 100.00, 0, @now, @start_date),

-- =============================================
-- 2-DAY SAFARIS (1 night)
-- =============================================

-- #10: Tarangire & Ngorongoro Express (MID_RANGE, 2D1N)
-- Accommodation ~$180, park fees ~$373, activities ~$197
(10, 'USD', 180.00, 373.00, 197.00, 750.00,
              140.00, 310.00, 150.00, 600.00, 0, @now, @start_date),

-- #11: Lake Manyara & Ngorongoro Luxury Retreat (LUXURY, 2D1N)
-- Accommodation ~$450, park fees ~$348, activities ~$302
(11, 'USD', 450.00, 348.00, 302.00, 1100.00,
              350.00, 290.00, 240.00, 880.00, 0, @now, @start_date),

-- #12: Serengeti Fly-In Exclusive (ULTRA_LUXURY, 2D1N)
-- Accommodation ~$800, park fees ~$82, flights ~$600, activities ~$318
(12, 'USD', 800.00, 82.00, 918.00, 1800.00,
              600.00, 70.00, 730.00, 1400.00, 0, @now, @start_date),

-- #13: Arusha & Tarangire Family Wildlife (MID_RANGE, 2D1N)
-- Accommodation ~$180, park fees ~$131, activities ~$189
(13, 'USD', 180.00, 131.00, 189.00, 500.00,
              140.00, 110.00, 150.00, 400.00, 0, @now, @start_date),

-- #14: Kilimanjaro Foothills & Arusha Adventure (BUDGET, 2D1N)
-- Accommodation ~$80, park fees ~$123, activities ~$147
(14, 'USD', 80.00, 123.00, 147.00, 350.00,
              60.00, 100.00, 120.00, 280.00, 0, @now, @start_date),

-- =============================================
-- 3-DAY SAFARIS (2 nights)
-- =============================================

-- #15: Serengeti Express (MID_RANGE, 3D2N)
-- Accommodation ~$360, park fees ~$220, activities ~$320
(15, 'USD', 360.00, 220.00, 320.00, 900.00,
              280.00, 180.00, 260.00, 720.00, 0, @now, @start_date),

-- #16: Ngorongoro, Tarangire & Manyara Highlights (LUXURY, 3D2N)
-- Accommodation ~$900, park fees ~$426, activities ~$374
(16, 'USD', 900.00, 426.00, 374.00, 1700.00,
              700.00, 355.00, 295.00, 1350.00, 0, @now, @start_date),

-- #17: Serengeti Wildlife Photography Expedition (LUXURY, 3D2N)
-- Accommodation ~$900, park fees ~$246, activities ~$554
(17, 'USD', 900.00, 246.00, 554.00, 1700.00,
              700.00, 205.00, 445.00, 1350.00, 0, @now, @start_date),

-- #18: Northern Circuit Budget Explorer (BUDGET, 3D2N)
-- Accommodation ~$160, park fees ~$340, activities ~$200
(18, 'USD', 160.00, 340.00, 200.00, 700.00,
              120.00, 280.00, 160.00, 560.00, 0, @now, @start_date),

-- #19: Kilimanjaro Machame — Shira Plateau Trek (MID_RANGE, 3D2N)
-- Accommodation ~$0 (camping), park fees ~$280 (permits), crew/activities ~$520
(19, 'USD', 0.00, 280.00, 520.00, 800.00,
              0.00, 230.00, 410.00, 640.00, 0, @now, @start_date),

-- #20: Family Safari — Arusha, Tarangire & Manyara (MID_RANGE, 3D2N)
-- Accommodation ~$360, park fees ~$184, activities ~$306
(20, 'USD', 360.00, 184.00, 306.00, 850.00,
              280.00, 150.00, 250.00, 680.00, 0, @now, @start_date),

-- =============================================
-- 4-DAY SAFARIS (3 nights)
-- =============================================

-- #21: Northern Circuit Classic Safari (MID_RANGE, 4D3N)
-- Accommodation ~$540, park fees ~$498, activities ~$362
(21, 'USD', 540.00, 498.00, 362.00, 1400.00,
              420.00, 415.00, 285.00, 1120.00, 0, @now, @start_date),

-- #22: Serengeti Great Migration Safari (LUXURY, 4D3N)
-- Accommodation ~$1350, park fees ~$400, activities ~$550
(22, 'USD', 1350.00, 400.00, 550.00, 2300.00,
              1050.00, 330.00, 440.00, 1820.00, 0, @now, @start_date),

-- #23: Romantic Crater & Serengeti Escape (ULTRA_LUXURY, 4D3N)
-- Accommodation ~$2100, park fees ~$400, activities ~$600
(23, 'USD', 2100.00, 400.00, 600.00, 3100.00,
              1600.00, 330.00, 470.00, 2400.00, 0, @now, @start_date),

-- #24: Northern Tanzania Backpacker Safari (BACKPACKER, 4D3N)
-- Accommodation ~$150, park fees ~$450, activities ~$200
(24, 'USD', 150.00, 450.00, 200.00, 800.00,
              110.00, 370.00, 160.00, 640.00, 0, @now, @start_date),

-- =============================================
-- 5-DAY SAFARIS (4 nights)
-- =============================================

-- #25: Northern Circuit Grand Safari (LUXURY, 5D4N)
-- Accommodation ~$1800, park fees ~$550, activities ~$650
(25, 'USD', 1800.00, 550.00, 650.00, 3000.00,
              1400.00, 460.00, 520.00, 2380.00, 0, @now, @start_date),

-- #26: Serengeti & Ngorongoro Romantic Getaway (ULTRA_LUXURY, 5D4N)
-- Accommodation ~$2800, park fees ~$500, activities ~$700
(26, 'USD', 2800.00, 500.00, 700.00, 4000.00,
              2200.00, 420.00, 560.00, 3180.00, 0, @now, @start_date),

-- #27: Big Five Photography Safari (LUXURY, 5D4N)
-- Accommodation ~$1800, park fees ~$550, activities ~$850
(27, 'USD', 1800.00, 550.00, 850.00, 3200.00,
              1400.00, 460.00, 680.00, 2540.00, 0, @now, @start_date),

-- #28: Kilimanjaro Marangu Trek (MID_RANGE, 5D4N)
-- Accommodation ~$0 (huts included in fees), park fees ~$500 (permits+rescue), crew ~$700
(28, 'USD', 0.00, 500.00, 700.00, 1200.00,
              0.00, 420.00, 560.00, 980.00, 0, @now, @start_date),

-- #29: Northern Tanzania Budget Explorer (BUDGET, 5D4N)
-- Accommodation ~$320, park fees ~$520, activities ~$260
(29, 'USD', 320.00, 520.00, 260.00, 1100.00,
              250.00, 430.00, 200.00, 880.00, 0, @now, @start_date),

-- =============================================
-- 6-DAY SAFARIS (5 nights)
-- =============================================

-- #30: Kilimanjaro Machame Trek (MID_RANGE, 6D5N)
-- Park fees ~$600 (permits+rescue), camping/crew ~$900
(30, 'USD', 0.00, 600.00, 900.00, 1500.00,
              0.00, 500.00, 720.00, 1220.00, 0, @now, @start_date),

-- #31: Ultimate Northern Circuit Safari (LUXURY, 6D5N)
-- Accommodation ~$2250, park fees ~$650, activities ~$700
(31, 'USD', 2250.00, 650.00, 700.00, 3600.00,
              1750.00, 540.00, 560.00, 2850.00, 0, @now, @start_date),

-- #32: Family Northern Tanzania Safari Adventure (MID_RANGE, 6D5N)
-- Accommodation ~$900, park fees ~$600, activities ~$500
(32, 'USD', 900.00, 600.00, 500.00, 2000.00,
              700.00, 500.00, 400.00, 1600.00, 0, @now, @start_date),

-- =============================================
-- 7-DAY SAFARIS (6 nights)
-- =============================================

-- #33: Bush to Beach — Northern Circuit & Zanzibar (LUXURY, 7D6N)
-- Safari 4 nights ~$2000 + Zanzibar 2 nights ~$600, park fees ~$550, activities ~$550
(33, 'USD', 2600.00, 550.00, 550.00, 3700.00,
              2000.00, 460.00, 440.00, 2900.00, 0, @now, @start_date),

-- #34: Kilimanjaro Lemosho Trek (MID_RANGE, 7D6N)
-- Park fees ~$700 (permits+rescue), camping/crew ~$1100
(34, 'USD', 0.00, 700.00, 1100.00, 1800.00,
              0.00, 580.00, 880.00, 1460.00, 0, @now, @start_date),

-- #35: Great Migration Photography Expedition (ULTRA_LUXURY, 7D6N)
-- Accommodation ~$4200, park fees ~$600, activities ~$1200
(35, 'USD', 4200.00, 600.00, 1200.00, 6000.00,
              3300.00, 500.00, 960.00, 4760.00, 0, @now, @start_date),

-- #36: Tanzania Backpacker Grand Tour (BACKPACKER, 7D6N)
-- Accommodation ~$360, park fees ~$650, activities ~$290
(36, 'USD', 360.00, 650.00, 290.00, 1300.00,
              280.00, 540.00, 230.00, 1050.00, 0, @now, @start_date),

-- =============================================
-- 8-DAY SAFARIS (7 nights)
-- =============================================

-- #37: Safari & Zanzibar Beach Honeymoon (LUXURY, 8D7N)
-- Safari 4N ~$2000 + Zanzibar 3N ~$1200, park fees ~$550, activities ~$650
(37, 'USD', 3200.00, 550.00, 650.00, 4400.00,
              2500.00, 460.00, 520.00, 3480.00, 0, @now, @start_date),

-- #38: Kilimanjaro Summit & Crater Safari Combo (MID_RANGE, 8D7N)
-- Kili 6D ~$1500 + Safari 2D ~$700, park fees ~$800
(38, 'USD', 350.00, 800.00, 1050.00, 2200.00,
              270.00, 660.00, 850.00, 1780.00, 0, @now, @start_date),

-- =============================================
-- 9-DAY SAFARIS (8 nights)
-- =============================================

-- #39: Ultimate Tanzania — Safari & Spice Island (ULTRA_LUXURY, 9D8N)
-- Safari 5N ~$4000 + Zanzibar 3N ~$2400, park fees ~$600, activities ~$1000
(39, 'USD', 6400.00, 600.00, 1000.00, 8000.00,
              5000.00, 500.00, 800.00, 6300.00, 0, @now, @start_date),

-- =============================================
-- 10-DAY SAFARIS (9 nights)
-- =============================================

-- #40: Northern Tanzania & Zanzibar Grand Tour (LUXURY, 10D9N)
-- Safari 6N ~$2700 + Zanzibar 3N ~$900, park fees ~$700, activities ~$700
(40, 'USD', 3600.00, 700.00, 700.00, 5000.00,
              2800.00, 580.00, 560.00, 3940.00, 0, @now, @start_date),

-- #41: Family Safari & Zanzibar Beach Holiday (MID_RANGE, 10D9N)
-- Safari 6N ~$1500 + Zanzibar 3N ~$600, park fees ~$700, activities ~$500
(41, 'USD', 2100.00, 700.00, 500.00, 3300.00,
              1600.00, 580.00, 400.00, 2580.00, 0, @now, @start_date),

-- =============================================
-- 12-DAY SAFARIS (11 nights)
-- =============================================

-- #42: Kilimanjaro Summit & Northern Circuit Safari (LUXURY, 12D11N)
-- Kili 7D + Safari 5D: Accom ~$2500, park fees ~$1200, activities ~$1300
(42, 'USD', 2500.00, 1200.00, 1300.00, 5000.00,
              1900.00, 1000.00, 1040.00, 3940.00, 0, @now, @start_date),

-- #43: Ultimate Honeymoon — Safari, Crater & Zanzibar (ULTRA_LUXURY, 12D11N)
-- Safari 6N + Zanzibar 5N: Accom ~$6600, park fees ~$700, activities ~$1200
(43, 'USD', 6600.00, 700.00, 1200.00, 8500.00,
              5100.00, 580.00, 960.00, 6640.00, 0, @now, @start_date),

-- =============================================
-- 13-14 DAY SAFARIS (12-13 nights)
-- =============================================

-- #44: Complete Northern Tanzania & Zanzibar Experience (LUXURY, 14D13N)
-- Safari 9N + Zanzibar 4N: Accom ~$5200, park fees ~$1000, activities ~$1000
(44, 'USD', 5200.00, 1000.00, 1000.00, 7200.00,
              4000.00, 830.00, 800.00, 5630.00, 0, @now, @start_date),

-- #45: Tanzania Wildlife Photography Grand Tour (ULTRA_LUXURY, 14D13N)
-- Safari 9N + Zanzibar 4N: Accom ~$8400, park fees ~$1000, activities ~$1600
(45, 'USD', 8400.00, 1000.00, 1600.00, 11000.00,
              6500.00, 830.00, 1270.00, 8600.00, 0, @now, @start_date),

-- #46: Tanzania Discovery — Backpacker's Dream (BACKPACKER, 13D12N)
-- Accommodation ~$720, park fees ~$900, activities ~$380
(46, 'USD', 720.00, 900.00, 380.00, 2000.00,
              560.00, 750.00, 310.00, 1620.00, 0, @now, @start_date),

-- =============================================
-- ORIGINAL 16-DAY SAFARI
-- =============================================

-- #1: Tanzania Safari & Zanzibar Beach Holiday (MID_RANGE, 16D15N)
-- Safari 12N + Zanzibar 3N: Accom ~$2700, park fees ~$1100, activities ~$700
(1, 'USD', 2700.00, 1100.00, 700.00, 4500.00,
             2100.00, 920.00, 560.00, 3580.00, 0, @now, @start_date);

-- Verify
SELECT i.id, i.name, i.budget_category, i.total_days,
       cs.grand_total_rack, cs.currency
FROM itineraries i
LEFT JOIN itinerary_cost_summaries cs ON cs.itinerary_id = i.id
ORDER BY i.id;
