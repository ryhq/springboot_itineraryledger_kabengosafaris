-- Flights as inventory, so a flying safari can be priced in one piece instead of two.
--
-- A flight is the one thing a northern-circuit trip needs that this system could not price. The
-- Arusha to Zanzibar sector was a sentence on the day and nothing on the cost sheet, and the
-- nine-day script says why in as many words: "The flight to Zanzibar is described on the day and
-- left off the cost sheet. Air fares are quoted separately." So every flying trip was quoted twice,
-- once by the system and once by hand, and the hand-written half reached the client unchecked.
--
-- The obvious cheap answer was to model a flight as an Activity. Measured against a real airline
-- price list (Air Excel, 01 June 2025 - 31 May 2027) that answer does not survive: 350 fare rows
-- across 243 route pairs and 21 airstrips, 65 routes with two or three departures a day, and a
-- taxes-and-fees amount that varies PER DEPARTURE rather than per airport - JRO to GRU is 25 on one
-- departure and 30 on another. An Activity has one name and no origin, destination, departure time
-- or tax component, so 243 activities called "Flight: X to Y" would be a catalogue nobody could
-- search and an invoice nobody could reconcile.
--
-- ---------------------------------------------------------------------------------------------
-- Four tables, not five
-- ---------------------------------------------------------------------------------------------
--
-- There is deliberately no flight_schedules table. A schedule would be one row per departure and a
-- fare would be one row per departure, because the source proves fare and tax both vary per
-- departure - so the two tables would be 1:1 and the join would buy nothing. One published line of
-- the price list is one row of flight_fares, times and price together.
--
-- ---------------------------------------------------------------------------------------------
-- Validity selects a price. Availability does not.
-- ---------------------------------------------------------------------------------------------
--
-- These are different things and only one of them picks a rate:
--
--   valid_from / valid_to   the contract window. THIS chooses the fare.
--   operating_months        which months the route actually flies.
--
-- The price list's "Season" column is the second of those, not the first: a route marked
-- "June - November" charges the same fare on every one of those days. Storing it as a month list
-- ("6,7,8,9,10,11") parsed all 350 rows with no failures, including the disjoint "Jan-Mar, June-Dec"
-- case, which is why it is one column here rather than a child table. It is ADVISORY: flying outside
-- it raises a warning, it never changes the price and never blocks a quote.
--
-- The existing `seasons` table was rejected for this. A season there is keyed to an accommodation or
-- flagged global, and an airline's operating window is neither.
--
-- ---------------------------------------------------------------------------------------------
-- Money
-- ---------------------------------------------------------------------------------------------
--
-- net_fare is what we pay and is what the quote is built from. gross_fare is the airline's published
-- price, kept for reference and for showing a client what they would pay booking direct. The Air
-- Excel sheet publishes GROSS ONLY, so an import of it leaves net_fare NULL on every row until the
-- office supplies the net fares; a fare with no net_fare raises a rate issue rather than quietly
-- pricing at the wrong number.
--
-- ⚠️ The house rack = 1.3 x STO rule MUST NOT reach this table. It is an accommodation rule. A 316
-- fare marked up 30% is 411 for a seat the airline publishes at 316, which nobody sells. Flights are
-- passed through at cost plus the markup below.
--
-- markup_type / markup_value default on the airline and may be overridden per fare and again per
-- flight line on a day. PERCENT or AMOUNT, applied per person, on the fare only - taxes are never
-- marked up.
--
-- ---------------------------------------------------------------------------------------------
-- A price list is reissued, so a fare is retired and never deleted
-- ---------------------------------------------------------------------------------------------
--
-- Next year's list creates new rows with a new valid_from; the old ones get retired_at set. They are
-- never removed, because an itinerary or a sent quote points at one and a quote must keep the price
-- it was sent with. departure_label gives a departure an identity that survives a reissue, so "the
-- 08:00, at this year's price" is answerable without a schedules table.

-- ---------------------------------------------------------------------------------------------
-- Where aircraft land
-- ---------------------------------------------------------------------------------------------
CREATE TABLE airstrips (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(10)  NOT NULL,          -- ARS, ZNZ, SER. The airline's own code, and stable.
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(160) NOT NULL,
    region      VARCHAR(100) NULL,
    district    VARCHAR(100) NULL,
    country     VARCHAR(100) NOT NULL DEFAULT 'Tanzania',
    latitude    DECIMAL(10, 7) NULL,
    longitude   DECIMAL(10, 7) NULL,
    notes       TEXT         NULL,
    is_active   BIT(1)       NOT NULL DEFAULT b'1',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_airstrip_code UNIQUE (code),
    CONSTRAINT uk_airstrip_slug UNIQUE (slug),
    INDEX idx_airstrip_is_active (is_active),
    INDEX idx_airstrip_name (name),
    INDEX idx_airstrip_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- Who flies them
-- ---------------------------------------------------------------------------------------------
CREATE TABLE airlines (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    code                 VARCHAR(10)  NULL,
    name                 VARCHAR(150) NOT NULL,
    slug                 VARCHAR(160) NOT NULL,
    website              VARCHAR(255) NULL,
    -- 20 kg on Air Excel's scheduled services, 23 on a charter. Printed for the client, because a
    -- guest who packs a hard case discovers this at the airstrip otherwise.
    baggage_kg           INT          NULL,
    baggage_notes        TEXT         NULL,
    -- The default markup for everything this airline flies. PERCENT or AMOUNT, per person, on the
    -- fare before taxes. A fare or an individual flight line may override it; neither has to.
    markup_type          VARCHAR(20)  NULL,
    markup_value         DECIMAL(10, 2) NULL,
    booking_terms        TEXT         NULL,
    cancellation_policy  TEXT         NULL,
    internal_notes       TEXT         NULL,
    is_active            BIT(1)       NOT NULL DEFAULT b'1',
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_airline_slug UNIQUE (slug),
    CONSTRAINT uk_airline_name UNIQUE (name),
    INDEX idx_airline_is_active (is_active),
    INDEX idx_airline_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- A sector one airline flies. 243 of these from one price list.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE flight_routes (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    code                    VARCHAR(60)  NULL,
    airline_id              BIGINT       NOT NULL,
    origin_airstrip_id      BIGINT       NOT NULL,
    destination_airstrip_id BIGINT       NOT NULL,
    -- 109 of the 350 rows are "N/A": the route is not scheduled and is flown on request only. This
    -- warns when a trip uses one; it does not block the quote.
    is_on_request           BIT(1)       NOT NULL DEFAULT b'0',
    minimum_seats           INT          NULL,
    remarks                 TEXT         NULL,
    is_active               BIT(1)       NOT NULL DEFAULT b'1',
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_flight_route_code UNIQUE (code),
    -- One airline flies a given sector once. Two rows for ARS->ZNZ on the same airline is a data
    -- entry mistake, not two products; the departures that differ live in flight_fares.
    CONSTRAINT uk_flight_route_sector UNIQUE (airline_id, origin_airstrip_id, destination_airstrip_id),
    -- RESTRICT on all three: an airstrip or an airline that still has routes must not vanish and
    -- leave a route pointing at nothing.
    CONSTRAINT fk_flight_route_airline FOREIGN KEY (airline_id) REFERENCES airlines (id) ON DELETE RESTRICT,
    CONSTRAINT fk_flight_route_origin FOREIGN KEY (origin_airstrip_id) REFERENCES airstrips (id) ON DELETE RESTRICT,
    CONSTRAINT fk_flight_route_destination FOREIGN KEY (destination_airstrip_id) REFERENCES airstrips (id) ON DELETE RESTRICT,
    INDEX idx_flight_route_airline (airline_id),
    INDEX idx_flight_route_origin (origin_airstrip_id),
    INDEX idx_flight_route_destination (destination_airstrip_id),
    INDEX idx_flight_route_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- One published line of a price list: a departure, its window, and what it costs.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE flight_fares (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    code              VARCHAR(60)  NULL,
    flight_route_id   BIGINT       NOT NULL,
    -- NULL on 65 of 350 rows, where the sheet says TBC. A time nobody has fixed yet is not a time,
    -- so it is not stored as one.
    etd               TIME         NULL,
    eta               TIME         NULL,
    -- "08:00" or "AM" - what makes this departure recognisable as the same one next year, when the
    -- price has changed and these rows have been replaced.
    departure_label   VARCHAR(40)  NULL,
    -- The contract window. This is what selects a fare for a travel date.
    valid_from        DATE         NOT NULL,
    valid_to          DATE         NOT NULL,
    -- Which months this departure flies: "6,7,8,9,10,11". A year-round row stores all twelve
    -- EXPLICITLY rather than NULL, so NULL keeps one meaning only — no window stated, which is what
    -- an on-request sector has. It also keeps the unique constraint above honest, since MySQL treats
    -- NULLs as distinct and two NULL-month rows would both slip through.
    -- Advisory for pricing: see the header. It governs availability, never the fare.
    operating_months  VARCHAR(40)  NULL,
    -- What we pay. NULL until the airline's net fares are loaded; a NULL here raises a rate issue
    -- rather than pricing a trip at a number nobody agreed.
    net_fare          DECIMAL(12, 2) NULL,
    -- What the airline publishes. Reference only, never the basis of a quote.
    gross_fare        DECIMAL(12, 2) NULL,
    -- Per person, and NEVER marked up. Separate from the fare because the airline itemises it and
    -- an invoice has to be reconcilable against theirs.
    taxes_and_fees    DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency          VARCHAR(3)   NOT NULL DEFAULT 'USD',
    -- Air Excel carries children under 14 at exactly 70% of the adult fare on all 350 rows, so it is
    -- a rule with an override rather than a second fare column to keep in step.
    child_percent     DECIMAL(5, 2) NULL DEFAULT 70.00,
    minimum_seats     INT          NULL,
    -- Overrides the airline default. NULL means "whatever the airline says".
    markup_type       VARCHAR(20)  NULL,
    markup_value      DECIMAL(10, 2) NULL,
    remarks           TEXT         NULL,
    -- Set when a new price list supersedes this row. Retired, never deleted: a sent quote points
    -- here and must keep the price it was sent with.
    retired_at        DATETIME(6)  NULL,
    is_active         BIT(1)       NOT NULL DEFAULT b'1',
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_flight_fare_code UNIQUE (code),
    -- A route may not publish the same departure, in the same months, twice in one contract window.
    --
    -- The operating window is part of the identity and not decoration: Air Excel's Arusha to Seronera
    -- leaves at 15:30 all year at one fare, but arrives 17:30 in Dec-Mar, 16:55 in Apr-May and 18:00
    -- in Jun-Nov, because the aircraft routes differently by season. Three rows, one departure, and a
    -- driver meeting the flight needs the right one. Keying on the time alone would have rejected two
    -- of the three as duplicates and lost the arrival times.
    CONSTRAINT uk_flight_fare_departure UNIQUE (flight_route_id, valid_from, etd, operating_months),
    CONSTRAINT fk_flight_fare_route FOREIGN KEY (flight_route_id) REFERENCES flight_routes (id) ON DELETE CASCADE,
    INDEX idx_flight_fare_route (flight_route_id),
    INDEX idx_flight_fare_validity (valid_from, valid_to),
    INDEX idx_flight_fare_is_active (is_active),
    INDEX idx_flight_fare_retired (retired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- A flight on a day of a trip
-- ---------------------------------------------------------------------------------------------
--
-- Shaped like itinerary_day_accommodations, because it is the same idea: a choice attached to a day,
-- with alternatives sitting beside the primary and excluded from the total.
--
-- flight_fare_id is NULLABLE on purpose. An itinerary is a product, not a booking: the planner
-- chooses the sector now and the office picks the departure when the dates are real. A line with a
-- route but no fare prices at the cheapest current fare for that route and says so.
CREATE TABLE itinerary_day_flights (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    itinerary_day_id    BIGINT       NOT NULL,
    flight_route_id     BIGINT       NOT NULL,
    flight_fare_id      BIGINT       NULL,
    passenger_count     INT          NULL,      -- NULL means "everybody on the trip"
    is_alternative      BIT(1)       NOT NULL DEFAULT b'0',
    is_included_in_price BIT(1)      NOT NULL DEFAULT b'1',
    -- Overrides the fare, which overrides the airline. NULL all the way up means no markup.
    markup_type         VARCHAR(20)  NULL,
    markup_value        DECIMAL(10, 2) NULL,
    sort_order          INT          NOT NULL DEFAULT 0,
    notes               TEXT         NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_iday_flight_day FOREIGN KEY (itinerary_day_id) REFERENCES itinerary_days (id) ON DELETE CASCADE,
    CONSTRAINT fk_iday_flight_route FOREIGN KEY (flight_route_id) REFERENCES flight_routes (id) ON DELETE RESTRICT,
    -- SET NULL, not RESTRICT: if a fare is ever genuinely removed the day keeps its route and the
    -- office re-picks a departure, rather than the delete failing somewhere far away.
    CONSTRAINT fk_iday_flight_fare FOREIGN KEY (flight_fare_id) REFERENCES flight_fares (id) ON DELETE SET NULL,
    INDEX idx_iday_flight_day (itinerary_day_id),
    INDEX idx_iday_flight_route (flight_route_id),
    INDEX idx_iday_flight_fare (flight_fare_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
