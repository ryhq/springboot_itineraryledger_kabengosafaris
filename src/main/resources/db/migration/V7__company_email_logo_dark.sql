-- A raster logo for a DARK email header (2026-08-21).
--
-- The shipped emails paint their header band in the brand's dark accent, and the one email logo was
-- whatever the company uploaded — for Kabengo, the green mark. Green ink on a dark green band is a
-- logo nobody can see: the welcome email opened with a faint outline where the mark should be.
--
-- A light/dark pair is the same answer already used for the icon logo and the favicon; email simply
-- needs its own because it must be a raster. LOGO_EMAIL stays the light-background copy, so nothing
-- already uploaded changes meaning.
--
alter table company_assets
    modify column asset_kind enum (
        'FAVICON_DARK',
        'FAVICON_LIGHT',
        'LOGO_DARK',
        'LOGO_EMAIL',
        'LOGO_EMAIL_DARK',
        'LOGO_FULL',
        'LOGO_FULL_TAGLINE',
        'LOGO_LIGHT'
    ) not null;
