-- Two more asset slots: the full logo, with and without its tagline (2026-08-19).
--
-- The existing LOGO_LIGHT / LOGO_DARK are the ICONIC mark — the shape alone, which is what fits a
-- 28px topbar, a favicon-sized space and a footer line. A letterhead, a wide email header or a
-- website header has room for the whole lockup: mark plus wordmark, sometimes plus the tagline.
-- Squeezing one into the other's place is why logos end up illegible.
--
-- MySQL stores the kind as an enum column, so the column itself has to learn the new values.
--
alter table company_assets
    modify column asset_kind enum (
        'FAVICON_DARK',
        'FAVICON_LIGHT',
        'LOGO_DARK',
        'LOGO_EMAIL',
        'LOGO_FULL',
        'LOGO_FULL_TAGLINE',
        'LOGO_LIGHT'
    ) not null;
