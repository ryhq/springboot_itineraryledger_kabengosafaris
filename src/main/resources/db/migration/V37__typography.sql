-- Text people actually type, in the data as well as the code (2026-11-16).
--
-- V35 cleaned the repositories: the shipped templates, the Java strings, the panel and both
-- websites. None of that reaches a row that is already stored. An accommodation whose
-- short_description was written with an em dash keeps it, a seeded blog post keeps the forty-six
-- it was seeded with, and a park still measures "14,763 km2" in a character nobody has on a
-- keyboard. This is the other half.
--
-- The column list is not a guess. It was read out of a backup of each company taken on
-- 2026-11-16 at 02:00, parsed row by row, and narrowed to the columns that actually hold one of
-- these characters: 106 on Kabengo, 52 on Jatelo, 109 in the union that one migration has to
-- cover. The result was simulated against those same dumps before this was written: 2,914 values
-- change, every JSON column still parses, and nothing outside the keep list survives.
--
-- WHAT IS DELIBERATELY NOT HERE
--
-- Anything somebody else wrote. contact_messages, booking_inquiries, email_messages and their
-- snippets, the Resend webhook payloads, a guest's own testimony text and review title, the
-- display names on received mail, and both halves of the audit log. Those are a record of what
-- was said and what changed. Tidying a record is falsifying it.
--
-- Anything that is data rather than prose: file names, URLs, enum values, class names, cron
-- expressions, the email headers an availability letter went out with. A fold through
-- `setting_value` would have reached a configured path, and nobody would have found out until
-- something stopped starting.
--
-- Two variable catalogues are missing from this list on purpose. email_events.variables_json and
-- pdf_documents.variables_json are reconciled from the schema files at boot, so cleaning those
-- files in V35 already cleaned the rows; both read clean on 1.0.58 before this was written.
--
-- ORDER MATTERS, AND IT IS THE WHOLE TRICK
--
--     ' km2'  before  '2'       a rewording, not a substitution: "14,763 km2" -> "14,763 sq km"
--     'C'     before  'degree'  so "-25 degrees C" stays a temperature and not an essay
--     warning before  the bare sign, because the emoji carries a variation selector after it
--     ' -> '  before  '->'      a spaced arrow becomes " to ", and "Arusha->Serengeti" still does
--
-- Every statement is guarded by a REGEXP, so each database rewrites only the rows it has, and a
-- second run changes nothing: no replacement produces a character that folds again.
--
-- A native UPDATE does not fire JPA auditing, so updated_at does not move. That is correct.
-- Nobody edited these records; their spelling was corrected underneath them.

UPDATE `accommodation_board_types` SET
    `description` = REPLACE(`description`, '—', '-')
WHERE `description` REGEXP '[—]';

UPDATE `accommodation_rates` SET
    `notes` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`notes`, '⚠️ ', ''), '⚠ ', ''), '⚠️', ''), '⚠', ''), '—', '-'), '×', 'x')
WHERE `notes` REGEXP '[×—⚠]';

UPDATE `accommodation_room_standards` SET
    `amenities` = REPLACE(REPLACE(REPLACE(`amenities`, ' km²', ' sq km'), ' m²', ' sq m'), '²', '2'),
    `description` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`description`, ' km²', ' sq km'), ' m²', ' sq m'), '²', '2'), '—', '-'), '×', 'x'),
    `name` = REPLACE(`name`, '—', '-')
WHERE `amenities` REGEXP '[²]'
   OR `description` REGEXP '[²×—]'
   OR `name` REGEXP '[—]';

UPDATE `accommodation_room_types` SET
    `bed_configuration` = REPLACE(`bed_configuration`, '—', '-'),
    `description` = REPLACE(REPLACE(`description`, '—', '-'), '–', '-'),
    `name` = REPLACE(`name`, '–', '-')
WHERE `bed_configuration` REGEXP '[—]'
   OR `description` REGEXP '[–—]'
   OR `name` REGEXP '[–]';

UPDATE `accommodations` SET
    `address` = REPLACE(`address`, '—', '-'),
    `amenities` = REPLACE(`amenities`, '×', 'x'),
    `best_season` = REPLACE(REPLACE(`best_season`, '—', '-'), '–', '-'),
    `cancellation_policy` = REPLACE(`cancellation_policy`, '–', '-'),
    `check_in_policy` = REPLACE(`check_in_policy`, '—', '-'),
    `check_out_policy` = REPLACE(`check_out_policy`, '—', '-'),
    `child_policy` = REPLACE(REPLACE(`child_policy`, '—', '-'), '–', '-'),
    `details` = REPLACE(REPLACE(REPLACE(`details`, '—', '-'), '–', '-'), '×', 'x'),
    `internal_notes` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`internal_notes`, '⚠️ ', ''), '⚠ ', ''), '⚠️', ''), '⚠', ''), ' → ', ' to '), '→', ' to '), '≥', '>='), '—', '-'), '–', '-'), '·', '|'), '•', '-'), '×', 'x'), '≥', '>='),
    `location` = REPLACE(REPLACE(`location`, '—', '-'), '–', '-'),
    `nearby_attractions` = REPLACE(REPLACE(`nearby_attractions`, '—', '-'), '–', '-'),
    `operating_season` = REPLACE(REPLACE(`operating_season`, '—', '-'), '–', '-'),
    `services` = REPLACE(`services`, '—', '-'),
    `short_description` = REPLACE(REPLACE(`short_description`, '—', '-'), '–', '-'),
    `terms_and_conditions` = REPLACE(REPLACE(`terms_and_conditions`, '—', '-'), '–', '-')
WHERE `address` REGEXP '[—]'
   OR `amenities` REGEXP '[×]'
   OR `best_season` REGEXP '[–—]'
   OR `cancellation_policy` REGEXP '[–]'
   OR `check_in_policy` REGEXP '[—]'
   OR `check_out_policy` REGEXP '[—]'
   OR `child_policy` REGEXP '[–—]'
   OR `details` REGEXP '[×–—]'
   OR `internal_notes` REGEXP '[·×–—•→≥⚠]'
   OR `location` REGEXP '[–—]'
   OR `nearby_attractions` REGEXP '[–—]'
   OR `operating_season` REGEXP '[–—]'
   OR `services` REGEXP '[—]'
   OR `short_description` REGEXP '[–—]'
   OR `terms_and_conditions` REGEXP '[–—]';

UPDATE `activities` SET
    `description` = REPLACE(`description`, '—', '-'),
    `detailed_description` = REPLACE(`detailed_description`, '—', '-'),
    `safety_information` = REPLACE(`safety_information`, '—', '-'),
    `season_availability` = REPLACE(`season_availability`, '–', '-')
WHERE `description` REGEXP '[—]'
   OR `detailed_description` REGEXP '[—]'
   OR `safety_information` REGEXP '[—]'
   OR `season_availability` REGEXP '[–]';

UPDATE `airlines` SET
    `baggage_notes` = REPLACE(`baggage_notes`, '×', 'x'),
    `internal_notes` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`internal_notes`, '⚠️ ', ''), '⚠ ', ''), '⚠️', ''), '⚠', ''), '—', '-')
WHERE `baggage_notes` REGEXP '[×]'
   OR `internal_notes` REGEXP '[—⚠]';

UPDATE `availability_requests` SET
    `subject` = REPLACE(REPLACE(`subject`, '–', '-'), '·', '|')
WHERE `subject` REGEXP '[·–]';

UPDATE `blogs` SET
    `body_json` = REPLACE(REPLACE(`body_json`, '—', '-'), '–', '-'),
    `excerpt` = REPLACE(`excerpt`, '—', '-'),
    `faqs_json` = REPLACE(REPLACE(`faqs_json`, '—', '-'), '–', '-')
WHERE `body_json` REGEXP '[–—]'
   OR `excerpt` REGEXP '[—]'
   OR `faqs_json` REGEXP '[–—]';

UPDATE `company_profile` SET
    `tagline` = REPLACE(`tagline`, '•', '-')
WHERE `tagline` REGEXP '[•]';

UPDATE `customer_notes` SET
    `content` = REPLACE(`content`, '—', '-'),
    `subject` = REPLACE(`subject`, '—', '-')
WHERE `content` REGEXP '[—]'
   OR `subject` REGEXP '[—]';

UPDATE `customers` SET
    `internal_notes` = REPLACE(REPLACE(REPLACE(`internal_notes`, ' → ', ' to '), '→', ' to '), '—', '-')
WHERE `internal_notes` REGEXP '[—→]';

UPDATE `docx_settings` SET
    `description` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`description`, '⚠️ ', ''), '⚠ ', ''), '⚠️', ''), '⚠', ''), '—', '-')
WHERE `description` REGEXP '[—⚠]';

UPDATE `email_account_signatures` SET
    `name` = REPLACE(`name`, '—', '-')
WHERE `name` REGEXP '[—]';

UPDATE `email_events` SET
    `description` = REPLACE(`description`, '—', '-')
WHERE `description` REGEXP '[—]';

UPDATE `expense_documents` SET
    `title` = REPLACE(`title`, '—', '-')
WHERE `title` REGEXP '[—]';

UPDATE `expense_line_items` SET
    `item_name` = REPLACE(`item_name`, '—', '-')
WHERE `item_name` REGEXP '[—]';

UPDATE `expenses` SET
    `description` = REPLACE(`description`, '·', '|'),
    `title` = REPLACE(`title`, '—', '-')
WHERE `description` REGEXP '[·]'
   OR `title` REGEXP '[—]';

UPDATE `feature_settings` SET
    `description` = REPLACE(`description`, '—', '-')
WHERE `description` REGEXP '[—]';

UPDATE `heroes` SET
    `description` = REPLACE(`description`, '—', '-'),
    `subtitle` = REPLACE(`subtitle`, '—', '-'),
    `title` = REPLACE(`title`, '—', '-')
WHERE `description` REGEXP '[—]'
   OR `subtitle` REGEXP '[—]'
   OR `title` REGEXP '[—]';

UPDATE `inclusion_items` SET
    `label` = REPLACE(`label`, '×', 'x')
WHERE `label` REGEXP '[×]';

UPDATE `invoice_documents` SET
    `title` = REPLACE(`title`, '—', '-')
WHERE `title` REGEXP '[—]';

UPDATE `invoice_inclusions` SET
    `label` = REPLACE(`label`, '×', 'x')
WHERE `label` REGEXP '[×]';

UPDATE `invoice_line_item_prices` SET
    `breakdown` = REPLACE(`breakdown`, '×', 'x')
WHERE `breakdown` REGEXP '[×]';

UPDATE `invoice_line_items` SET
    `description` = REPLACE(REPLACE(`description`, ' → ', ' to '), '→', ' to '),
    `item_name` = REPLACE(`item_name`, '—', '-')
WHERE `description` REGEXP '[→]'
   OR `item_name` REGEXP '[—]';

UPDATE `invoices` SET
    `description` = REPLACE(REPLACE(`description`, '—', '-'), '’', ''''),
    `title` = REPLACE(`title`, '—', '-')
WHERE `description` REGEXP '[—’]'
   OR `title` REGEXP '[—]';

UPDATE `itineraries` SET
    `description` = REPLACE(REPLACE(REPLACE(`description`, '—', '-'), '–', '-'), '’', ''''),
    `end_location` = REPLACE(`end_location`, '—', '-'),
    `highlights` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`highlights`, ' → ', ' to '), '→', ' to '), '—', '-'), '–', '-'), '​', ''),
    `name` = REPLACE(REPLACE(REPLACE(`name`, ' → ', ' to '), '→', ' to '), '—', '-'),
    `start_location` = REPLACE(`start_location`, '—', '-')
WHERE `description` REGEXP '[–—’]'
   OR `end_location` REGEXP '[—]'
   OR `highlights` REGEXP '[​–—→]'
   OR `name` REGEXP '[—→]'
   OR `start_location` REGEXP '[—]';

UPDATE `itinerary_day_accommodations` SET
    `notes` = REPLACE(REPLACE(`notes`, '—', '-'), '×', 'x')
WHERE `notes` REGEXP '[×—]';

UPDATE `itinerary_day_park_activities` SET
    `notes` = REPLACE(`notes`, '—', '-')
WHERE `notes` REGEXP '[—]';

UPDATE `itinerary_day_parks` SET
    `notes` = REPLACE(REPLACE(`notes`, '—', '-'), '’', '''')
WHERE `notes` REGEXP '[—’]';

UPDATE `itinerary_days` SET
    `afternoon_activities` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`afternoon_activities`, ' → ', ' to '), '→', ' to '), '—', '-'), '–', '-'), '’', ''''), '​', ''),
    `description` = REPLACE(REPLACE(REPLACE(REPLACE(`description`, '—', '-'), '–', '-'), '’', ''''), '​', ''),
    `end_location` = REPLACE(REPLACE(`end_location`, ' → ', ' to '), '→', ' to '),
    `evening_activities` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`evening_activities`, ' → ', ' to '), '→', ' to '), '—', '-'), '–', '-'), '​', ''),
    `morning_activities` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`morning_activities`, '°C', 'C'), '°F', 'F'), ' → ', ' to '), '→', ' to '), '°', ' degrees'), '—', '-'), '–', '-'), '’', ''''),
    `scenic_highlights` = REPLACE(REPLACE(REPLACE(REPLACE(`scenic_highlights`, '—', '-'), '“', '"'), '”', '"'), '​', ''),
    `special_notes` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`special_notes`, ' km²', ' sq km'), ' m²', ' sq m'), '°C', 'C'), '°F', 'F'), '²', '2'), '°', ' degrees'), '—', '-'), '–', '-'), '​', ''),
    `title` = REPLACE(REPLACE(REPLACE(REPLACE(`title`, ' → ', ' to '), '→', ' to '), '—', '-'), '’', ''''),
    `wildlife_highlights` = REPLACE(REPLACE(REPLACE(`wildlife_highlights`, '—', '-'), '–', '-'), '​', '')
WHERE `afternoon_activities` REGEXP '[​–—’→]'
   OR `description` REGEXP '[​–—’]'
   OR `end_location` REGEXP '[→]'
   OR `evening_activities` REGEXP '[​–—→]'
   OR `morning_activities` REGEXP '[°–—’→]'
   OR `scenic_highlights` REGEXP '[​—“”]'
   OR `special_notes` REGEXP '[°²​–—]'
   OR `title` REGEXP '[—’→]'
   OR `wildlife_highlights` REGEXP '[​–—]';

UPDATE `itinerary_pax` SET
    `notes` = REPLACE(`notes`, '—', '-')
WHERE `notes` REGEXP '[—]';

UPDATE `parks` SET
    `size` = REPLACE(REPLACE(REPLACE(`size`, ' km²', ' sq km'), ' m²', ' sq m'), '²', '2')
WHERE `size` REGEXP '[²]';

UPDATE `pdf_templates` SET
    `description` = REPLACE(`description`, '—', '-')
WHERE `description` REGEXP '[—]';

UPDATE `quote_day_accommodations` SET
    `notes` = REPLACE(REPLACE(`notes`, '—', '-'), '×', 'x')
WHERE `notes` REGEXP '[×—]';

UPDATE `quote_day_parks` SET
    `notes` = REPLACE(`notes`, '—', '-')
WHERE `notes` REGEXP '[—]';

UPDATE `quote_days` SET
    `afternoon_activities` = REPLACE(REPLACE(REPLACE(`afternoon_activities`, ' → ', ' to '), '→', ' to '), '—', '-'),
    `description` = REPLACE(REPLACE(REPLACE(`description`, '—', '-'), '–', '-'), '’', ''''),
    `end_location` = REPLACE(REPLACE(`end_location`, ' → ', ' to '), '→', ' to '),
    `evening_activities` = REPLACE(REPLACE(REPLACE(`evening_activities`, ' → ', ' to '), '→', ' to '), '—', '-'),
    `morning_activities` = REPLACE(REPLACE(REPLACE(`morning_activities`, ' → ', ' to '), '→', ' to '), '—', '-'),
    `scenic_highlights` = REPLACE(`scenic_highlights`, '—', '-'),
    `special_notes` = REPLACE(`special_notes`, '—', '-'),
    `title` = REPLACE(REPLACE(REPLACE(`title`, ' → ', ' to '), '→', ' to '), '—', '-'),
    `wildlife_highlights` = REPLACE(REPLACE(`wildlife_highlights`, '—', '-'), '–', '-')
WHERE `afternoon_activities` REGEXP '[—→]'
   OR `description` REGEXP '[–—’]'
   OR `end_location` REGEXP '[→]'
   OR `evening_activities` REGEXP '[—→]'
   OR `morning_activities` REGEXP '[—→]'
   OR `scenic_highlights` REGEXP '[—]'
   OR `special_notes` REGEXP '[—]'
   OR `title` REGEXP '[—→]'
   OR `wildlife_highlights` REGEXP '[–—]';

UPDATE `quote_documents` SET
    `title` = REPLACE(`title`, '—', '-')
WHERE `title` REGEXP '[—]';

UPDATE `quote_inclusions` SET
    `label` = REPLACE(`label`, '×', 'x')
WHERE `label` REGEXP '[×]';

UPDATE `quote_item_prices` SET
    `breakdown` = REPLACE(`breakdown`, '×', 'x')
WHERE `breakdown` REGEXP '[×]';

UPDATE `quote_items` SET
    `description` = REPLACE(REPLACE(REPLACE(`description`, ' → ', ' to '), '→', ' to '), '•', '-'),
    `item_name` = REPLACE(REPLACE(REPLACE(`item_name`, ' → ', ' to '), '→', ' to '), '—', '-')
WHERE `description` REGEXP '[•→]'
   OR `item_name` REGEXP '[—→]';

UPDATE `quote_revisions` SET
    `snapshot` = REPLACE(REPLACE(REPLACE(`snapshot`, ' → ', ' to '), '→', ' to '), '—', '-')
WHERE `snapshot` REGEXP '[—→]';

UPDATE `quotes` SET
    `description` = REPLACE(REPLACE(REPLACE(`description`, '—', '-'), '–', '-'), '’', ''''),
    `title` = REPLACE(REPLACE(REPLACE(`title`, ' → ', ' to '), '→', ' to '), '—', '-')
WHERE `description` REGEXP '[–—’]'
   OR `title` REGEXP '[—→]';

UPDATE `safari_day_accommodations` SET
    `notes` = REPLACE(REPLACE(`notes`, '—', '-'), '×', 'x')
WHERE `notes` REGEXP '[×—]';

UPDATE `safari_days` SET
    `afternoon_activities` = REPLACE(REPLACE(REPLACE(`afternoon_activities`, ' → ', ' to '), '→', ' to '), '—', '-'),
    `description` = REPLACE(`description`, '—', '-'),
    `morning_activities` = REPLACE(REPLACE(`morning_activities`, ' → ', ' to '), '→', ' to '),
    `special_notes` = REPLACE(REPLACE(`special_notes`, '—', '-'), '–', '-'),
    `title` = REPLACE(REPLACE(REPLACE(`title`, ' → ', ' to '), '→', ' to '), '—', '-')
WHERE `afternoon_activities` REGEXP '[—→]'
   OR `description` REGEXP '[—]'
   OR `morning_activities` REGEXP '[→]'
   OR `special_notes` REGEXP '[–—]'
   OR `title` REGEXP '[—→]';

UPDATE `safari_documents` SET
    `description` = REPLACE(REPLACE(`description`, ' → ', ' to '), '→', ' to '),
    `title` = REPLACE(`title`, '—', '-')
WHERE `description` REGEXP '[→]'
   OR `title` REGEXP '[—]';

UPDATE `safari_inclusions` SET
    `label` = REPLACE(`label`, '×', 'x')
WHERE `label` REGEXP '[×]';

UPDATE `safaris` SET
    `description` = REPLACE(`description`, '’', ''''),
    `highlights` = REPLACE(`highlights`, '–', '-'),
    `state_reason` = REPLACE(`state_reason`, '—', '-')
WHERE `description` REGEXP '[’]'
   OR `highlights` REGEXP '[–]'
   OR `state_reason` REGEXP '[—]';

UPDATE `season_periods` SET
    `notes` = REPLACE(`notes`, '–', '-')
WHERE `notes` REGEXP '[–]';

UPDATE `seasons` SET
    `description` = REPLACE(REPLACE(REPLACE(REPLACE(`description`, ' → ', ' to '), '→', ' to '), '—', '-'), '–', '-')
WHERE `description` REGEXP '[–—→]';

UPDATE `vendors` SET
    `name` = REPLACE(`name`, '–', '-')
WHERE `name` REGEXP '[–]';

-- ---------------------------------------------------------------------------------------
-- The translation cache, folded and then re-keyed.
--
-- It is keyed on SHA-256(source_language | target_language | original_content), so changing
-- one character of a source string orphans every language variant of it: the old row keeps
-- the old hash, is never matched again, and a row with verified = 1 is never deleted
-- either, so it becomes permanent dead weight. The next public request is a cache miss
-- that goes to the provider and silently replaces whatever a person had corrected.
--
-- The formula was checked against 400 real rows before this was written: all 400 reproduce
-- their stored hash exactly, so re-keying here keeps roughly 29,000 cached translations
-- and every human correction in them.
--
-- A translation into German, French, Spanish, Italian, Portuguese, Ukrainian or Russian
-- keeps that language's own punctuation, for the same reason the message catalogues do.
-- Only the dashes, bullets and ellipses go.
-- ---------------------------------------------------------------------------------------

UPDATE `translation_cache` SET
    `original_content` = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`original_content`, ' km²', ' sq km'), ' m²', ' sq m'), '°C', 'C'), '°F', 'F'), '⚠️ ', ''), '⚠ ', ''), '⚠️', ''), '⚠', ''), ' → ', ' to '), '→', ' to '), '≥', '>='), '≤', '<='), '²', '2'), '°', ' degrees'), '®', ''), '™', ''), '—', '-'), '–', '-'), '‒', '-'), '―', '-'), '−', '-'), '·', '|'), '•', '-'), '…', '...'), '⋯', '...'), '’', ''''), '‘', ''''), '‚', ''''), '“', '"'), '”', '"'), '„', '"'), '«', '"'), '»', '"'), '×', 'x'), '✕', 'x'), '≈', '~'), '≥', '>='), '≤', '<='), '‹', '<'), '›', '>'), '⌘', 'Cmd'), ' ', ' '), '​', ''), '­', ''), '─', '-'), '━', '-'), '│', '|'), '┃', '|'), '┌', '+'), '┐', '+'), '└', '+'), '┘', '+'), '├', '+'), '┤', '+'), '┬', '+'), '┴', '+'), '┼', '+'), '═', '='), '║', '|'), '╔', '+'), '╗', '+'), '╚', '+'), '╝', '+'), '╠', '+'), '╣', '+'), '╦', '+'), '╩', '+'), '╬', '+'), '█', '#'), '░', '#'), '▒', '#'), '▓', '#'), '▲', '^'), '▼', 'v'), '⊘', 'x'), '◍', 'o'), '◌', 'o'), '⤢', '[]'),
    `translated_content` = CASE
        WHEN `target_language` IN ('en', 'sw') THEN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`translated_content`, ' km²', ' sq km'), ' m²', ' sq m'), '°C', 'C'), '°F', 'F'), '⚠️ ', ''), '⚠ ', ''), '⚠️', ''), '⚠', ''), ' → ', ' to '), '→', ' to '), '≥', '>='), '≤', '<='), '²', '2'), '°', ' degrees'), '®', ''), '™', ''), '—', '-'), '–', '-'), '‒', '-'), '―', '-'), '−', '-'), '·', '|'), '•', '-'), '…', '...'), '⋯', '...'), '’', ''''), '‘', ''''), '‚', ''''), '“', '"'), '”', '"'), '„', '"'), '«', '"'), '»', '"'), '×', 'x'), '✕', 'x'), '≈', '~'), '≥', '>='), '≤', '<='), '‹', '<'), '›', '>'), '⌘', 'Cmd'), ' ', ' '), '​', ''), '­', ''), '─', '-'), '━', '-'), '│', '|'), '┃', '|'), '┌', '+'), '┐', '+'), '└', '+'), '┘', '+'), '├', '+'), '┤', '+'), '┬', '+'), '┴', '+'), '┼', '+'), '═', '='), '║', '|'), '╔', '+'), '╗', '+'), '╚', '+'), '╝', '+'), '╠', '+'), '╣', '+'), '╦', '+'), '╩', '+'), '╬', '+'), '█', '#'), '░', '#'), '▒', '#'), '▓', '#'), '▲', '^'), '▼', 'v'), '⊘', 'x'), '◍', 'o'), '◌', 'o'), '⤢', '[]')
        ELSE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(`translated_content`, ' km²', ' sq km'), ' m²', ' sq m'), '°C', 'C'), '°F', 'F'), '⚠️ ', ''), '⚠ ', ''), '⚠️', ''), '⚠', ''), ' → ', ' to '), '→', ' to '), '≥', '>='), '≤', '<='), '²', '2'), '°', ' degrees'), '®', ''), '™', ''), '—', '-'), '–', '-'), '‒', '-'), '―', '-'), '−', '-'), '·', '|'), '•', '-'), '…', '...'), '⋯', '...'), '‚', ''''), '×', 'x'), '✕', 'x'), '≈', '~'), '≥', '>='), '≤', '<='), '‹', '<'), '›', '>'), '⌘', 'Cmd'), '​', ''), '­', ''), '─', '-'), '━', '-'), '│', '|'), '┃', '|'), '┌', '+'), '┐', '+'), '└', '+'), '┘', '+'), '├', '+'), '┤', '+'), '┬', '+'), '┴', '+'), '┼', '+'), '═', '='), '║', '|'), '╔', '+'), '╗', '+'), '╚', '+'), '╝', '+'), '╠', '+'), '╣', '+'), '╦', '+'), '╩', '+'), '╬', '+'), '█', '#'), '░', '#'), '▒', '#'), '▓', '#'), '▲', '^'), '▼', 'v'), '⊘', 'x'), '◍', 'o'), '◌', 'o'), '⤢', '[]')
    END
WHERE (`original_content` REGEXP '[ «­®°²·»×​‒–—―‘’‚“”„•…‹›™→−≈≤≥⊘⋯⌘─━│┃┌┐└┘├┤┬┴┼═║╔╗╚╝╠╣╦╩╬█░▒▓▲▼◌◍⚠✕⤢]'
    OR `translated_content` REGEXP '[ «­®°²·»×​‒–—―‘’‚“”„•…‹›™→−≈≤≥⊘⋯⌘─━│┃┌┐└┘├┤┬┴┼═║╔╗╚╝╠╣╦╩╬█░▒▓▲▼◌◍⚠✕⤢]')
  -- A guest's own words are not folded in `testimonies`, so they must not be folded here
  -- either: the cache is keyed on its source text, and folding one side of that pair is
  -- exactly how a translation gets orphaned. 56 rows on Kabengo, none on Jatelo.
  AND `original_content` NOT IN (
        SELECT `message` FROM `testimonies` WHERE `message` IS NOT NULL)
  AND `original_content` NOT IN (
        SELECT `review_title` FROM `testimonies` WHERE `review_title` IS NOT NULL);

UPDATE `translation_cache`
SET `content_hash` = SHA2(
        CONCAT(`source_language`, '|', `target_language`, '|', `original_content`), 256);
