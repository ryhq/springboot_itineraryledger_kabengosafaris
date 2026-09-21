-- A flight follows the trip down the chain: itinerary, quote, safari.
--
-- V18 gave an itinerary day its flights. This gives the same to a quote and to a safari, because
-- the three carry their own copy of the day tree rather than pointing back at the itinerary — a
-- quote that has been sent must keep the trip it was sent with, and a safari that is running must
-- keep the trip that was sold. The same reason quote_days and safari_days exist at all.
--
-- Both tables are created together in one migration on purpose: they are the same shape, they are
-- needed by the same feature, and splitting them over two deploys would leave a release where a
-- quote could carry a flight and the safari built from it silently could not.
--
-- ---------------------------------------------------------------------------------------------
-- What is copied, and what is not
-- ---------------------------------------------------------------------------------------------
--
-- The chosen fare travels as a reference, not as a copy of its numbers. That is deliberate and it
-- is the opposite of how the inclusion lines were done: an inclusion is a SENTENCE, and a sentence
-- printed on a sent quote must never change afterwards, so it was snapshotted as text. A fare is a
-- PRICE, and the price on a quote is already frozen where it belongs — in the quote's own line
-- items, written when the quote was generated. Copying the fare's figures here as well would give
-- the same number two homes and no rule for which wins.
--
-- flight_fare_id is therefore ON DELETE SET NULL, and flight_fares are retired rather than deleted
-- (see V18), so the reference survives a price-list reissue and the quote keeps its own price.
--
-- ---------------------------------------------------------------------------------------------
-- The markup override travels
-- ---------------------------------------------------------------------------------------------
--
-- markup_type / markup_value are copied down the chain because they are a DECISION somebody made
-- about this trip — "flat 50 on this sector for this client" — not a rate. Losing it on the way to
-- the quote would silently re-price the flight at the airline's default, which is exactly the kind
-- of quiet change this module has had to be careful about throughout.

-- ---------------------------------------------------------------------------------------------
-- A flight on a day of a quote
-- ---------------------------------------------------------------------------------------------
CREATE TABLE quote_day_flights (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    quote_day_id         BIGINT       NOT NULL,
    flight_route_id      BIGINT       NOT NULL,
    flight_fare_id       BIGINT       NULL,
    passenger_count      INT          NULL,      -- NULL means everybody on the trip
    is_alternative       BIT(1)       NOT NULL DEFAULT b'0',
    is_included_in_price BIT(1)       NOT NULL DEFAULT b'1',
    markup_type          VARCHAR(20)  NULL,
    markup_value         DECIMAL(10, 2) NULL,
    sort_order           INT          NOT NULL DEFAULT 0,
    notes                TEXT         NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_qday_flight_day FOREIGN KEY (quote_day_id) REFERENCES quote_days (id) ON DELETE CASCADE,
    -- RESTRICT: a sector a quote is using must not vanish underneath it.
    CONSTRAINT fk_qday_flight_route FOREIGN KEY (flight_route_id) REFERENCES flight_routes (id) ON DELETE RESTRICT,
    -- SET NULL: the quote keeps its own price, so a removed departure costs it the reference and
    -- nothing else. The alternative — RESTRICT — would fail a delete somewhere far from the cause.
    CONSTRAINT fk_qday_flight_fare FOREIGN KEY (flight_fare_id) REFERENCES flight_fares (id) ON DELETE SET NULL,
    INDEX idx_qday_flight_day (quote_day_id),
    INDEX idx_qday_flight_route (flight_route_id),
    INDEX idx_qday_flight_fare (flight_fare_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- A flight on a day of a safari
-- ---------------------------------------------------------------------------------------------
--
-- Same shape, plus booking_status. A safari is a trip that is actually running, so its day rows
-- carry operational state the quote's do not: whether the seat has been asked for, held, or
-- ticketed. The accommodation rows on a safari already work this way.
CREATE TABLE safari_day_flights (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    safari_day_id        BIGINT       NOT NULL,
    flight_route_id      BIGINT       NOT NULL,
    flight_fare_id       BIGINT       NULL,
    passenger_count      INT          NULL,
    is_alternative       BIT(1)       NOT NULL DEFAULT b'0',
    is_included_in_price BIT(1)       NOT NULL DEFAULT b'1',
    markup_type          VARCHAR(20)  NULL,
    markup_value         DECIMAL(10, 2) NULL,
    -- PENDING on arrival, like every other safari day row: a trip that has been sold has not yet
    -- had its seats held, and pretending otherwise is how a party reaches an airstrip without one.
    booking_status       VARCHAR(30)  NULL,
    -- What the airline actually gave us, once somebody has booked it.
    ticket_reference     VARCHAR(100) NULL,
    sort_order           INT          NOT NULL DEFAULT 0,
    notes                TEXT         NULL,
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sday_flight_day FOREIGN KEY (safari_day_id) REFERENCES safari_days (id) ON DELETE CASCADE,
    CONSTRAINT fk_sday_flight_route FOREIGN KEY (flight_route_id) REFERENCES flight_routes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_sday_flight_fare FOREIGN KEY (flight_fare_id) REFERENCES flight_fares (id) ON DELETE SET NULL,
    INDEX idx_sday_flight_day (safari_day_id),
    INDEX idx_sday_flight_route (flight_route_id),
    INDEX idx_sday_flight_fare (flight_fare_id),
    INDEX idx_sday_flight_status (booking_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
