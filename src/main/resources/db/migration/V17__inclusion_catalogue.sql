-- What the price covers, as data rather than as a paragraph retyped 58 times.
--
-- "What's included" and "what's not included" is the promise attached to a price. Until now it was
-- two free-text TEXT columns on `itineraries`, one item per line, and a count against the live
-- database said what that costs: of 65 itineraries, 58 held the byte-identical eight-line included
-- list and the byte-identical six-line excluded list, 3 held a paragraph crammed into the field,
-- 1 held a genuine variant, and 3 held nothing at all. Correcting one word meant editing 58 rows,
-- and the three nobody had pasted into simply showed no section on the website.
--
-- Worse, it reached no document. FullItineraryDTO never carried the field, no PDF schema declared
-- it, no template rendered it, and quotes, safaris and invoices had no such column at all — so a
-- customer received a price with no statement anywhere of what it covered.
--
-- This migration creates the tables. It seeds nothing: the standard fourteen lines are written by
-- InclusionItemInitializer and the existing itineraries are linked by
-- ItineraryInclusionBackfillInitializer, so that both are readable Java that tops up idempotently
-- on every boot rather than a one-shot INSERT nobody can re-run.
--
-- The old itineraries.inclusions / .exclusions columns are deliberately NOT dropped here. Every
-- read goes through ItineraryInclusionReader, which falls back to them when an itinerary has no
-- rows yet; that makes the backfill safe by construction and the whole change reversible with a
-- DELETE. A later migration drops them once nothing is falling back.

-- ---------------------------------------------------------------------------------------------
-- The catalogue
-- ---------------------------------------------------------------------------------------------
CREATE TABLE inclusion_items (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    code              VARCHAR(60)  NULL,
    label             VARCHAR(300) NOT NULL,
    category          VARCHAR(120) NULL,
    display_order     INT          NOT NULL DEFAULT 0,
    is_active         BIT(1)       NOT NULL DEFAULT b'1',
    is_system         BIT(1)       NOT NULL DEFAULT b'0',
    is_standard       BIT(1)       NOT NULL DEFAULT b'1',
    default_included  BIT(1)       NOT NULL DEFAULT b'1',
    -- LineCategoryScope canonical text. NULL means "this sentence claims nothing a machine can
    -- check", which is the OPPOSITE of what the same column means on quotes.tax_applies_to, where
    -- NULL means every category. Anything reading this must test for null before calling
    -- LineCategoryScope.covers(), which answers true for null.
    claim_applies_to  VARCHAR(200) NULL,
    internal_notes    TEXT         NULL,
    created_by        BIGINT       NULL,
    updated_by        BIGINT       NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_inclusion_item_code UNIQUE (code),
    CONSTRAINT fk_inclusion_item_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_inclusion_item_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
    INDEX idx_inclusion_item_is_active (is_active),
    INDEX idx_inclusion_item_display_order (display_order),
    INDEX idx_inclusion_item_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- The itinerary's own list
--
-- is_included is stored, not derived. Reading "not included" as the complement of the ticked set
-- would mean adding one row to the catalogue silently rewrote the not-included paragraph of every
-- itinerary at once, and it would make it impossible to add a line at quote level — which is the
-- whole point of letting the promise be edited down the chain.
--
-- sort_order lives here rather than being read through the catalogue: otherwise dragging one item
-- in the catalogue would reorder the printed promise on all 65 itineraries, an edit nobody made.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE itinerary_inclusions (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    itinerary_id      BIGINT      NOT NULL,
    inclusion_item_id BIGINT      NOT NULL,
    is_included       BIT(1)      NOT NULL DEFAULT b'1',
    sort_order        INT         NOT NULL DEFAULT 0,
    created_at        DATETIME(6) NOT NULL,
    updated_at        DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_itinerary_inclusion UNIQUE (itinerary_id, inclusion_item_id),
    CONSTRAINT fk_itinerary_inclusion_itinerary FOREIGN KEY (itinerary_id)
        REFERENCES itineraries (id) ON DELETE CASCADE,
    -- RESTRICT, so a catalogue item an itinerary still lists cannot be deleted out from under it.
    -- This is what the delete endpoint's "disable it instead" refusal is backed by.
    CONSTRAINT fk_itinerary_inclusion_item FOREIGN KEY (inclusion_item_id)
        REFERENCES inclusion_items (id) ON DELETE RESTRICT,
    INDEX idx_itinerary_inclusion_itinerary (itinerary_id),
    INDEX idx_itinerary_inclusion_item (inclusion_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- The document snapshots
--
-- The wording is copied as TEXT, not joined through a foreign key. A sent quote's promise has to
-- be frozen: one UPDATE inclusion_items SET label = … would otherwise rewrite every quote ever
-- sent, including ones already rendered to PDF and emailed, so the customer's copy and ours would
-- disagree and ours would be the one that looked altered. inclusion_item_id survives only as a
-- breadcrumb for the panel, nullable, and is never dereferenced to render anything.
--
-- It is also what the house already does with money: quote_items.item_name and
-- invoice_line_items.item_name are denormalised text for exactly this reason.
-- ---------------------------------------------------------------------------------------------
CREATE TABLE quote_inclusions (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    quote_id          BIGINT       NOT NULL,
    inclusion_item_id BIGINT       NULL,
    label             VARCHAR(300) NOT NULL,
    category          VARCHAR(120) NULL,
    claim_applies_to  VARCHAR(200) NULL,
    is_included       BIT(1)       NOT NULL DEFAULT b'1',
    sort_order        INT          NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_quote_inclusion_quote FOREIGN KEY (quote_id)
        REFERENCES quotes (id) ON DELETE CASCADE,
    CONSTRAINT fk_quote_inclusion_item FOREIGN KEY (inclusion_item_id)
        REFERENCES inclusion_items (id) ON DELETE SET NULL,
    INDEX idx_quote_inclusion_quote (quote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE safari_inclusions (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    safari_id         BIGINT       NOT NULL,
    inclusion_item_id BIGINT       NULL,
    label             VARCHAR(300) NOT NULL,
    category          VARCHAR(120) NULL,
    claim_applies_to  VARCHAR(200) NULL,
    is_included       BIT(1)       NOT NULL DEFAULT b'1',
    sort_order        INT          NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_safari_inclusion_safari FOREIGN KEY (safari_id)
        REFERENCES safaris (id) ON DELETE CASCADE,
    CONSTRAINT fk_safari_inclusion_item FOREIGN KEY (inclusion_item_id)
        REFERENCES inclusion_items (id) ON DELETE SET NULL,
    INDEX idx_safari_inclusion_safari (safari_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_inclusions (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    invoice_id        BIGINT       NOT NULL,
    inclusion_item_id BIGINT       NULL,
    label             VARCHAR(300) NOT NULL,
    category          VARCHAR(120) NULL,
    claim_applies_to  VARCHAR(200) NULL,
    is_included       BIT(1)       NOT NULL DEFAULT b'1',
    sort_order        INT          NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_inclusion_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices (id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_inclusion_item FOREIGN KEY (inclusion_item_id)
        REFERENCES inclusion_items (id) ON DELETE SET NULL,
    INDEX idx_invoice_inclusion_invoice (invoice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------------------------
-- Provenance on each document
--
-- Without these, a quote, the safari it became and the invoice that bills it can each hold a
-- different promise with nothing on screen revealing that they diverged. INHERITED means "still
-- what the parent said"; EDITED means somebody changed it here, and the reset action offers to
-- pull the parent's version back.
-- ---------------------------------------------------------------------------------------------
ALTER TABLE quotes
    ADD COLUMN inclusions_source    VARCHAR(20) NULL,
    ADD COLUMN inclusions_synced_at DATETIME(6) NULL;

ALTER TABLE safaris
    ADD COLUMN inclusions_source    VARCHAR(20) NULL,
    ADD COLUMN inclusions_synced_at DATETIME(6) NULL;

ALTER TABLE invoices
    ADD COLUMN inclusions_source    VARCHAR(20) NULL,
    ADD COLUMN inclusions_synced_at DATETIME(6) NULL;
