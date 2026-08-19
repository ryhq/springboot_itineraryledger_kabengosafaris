-- The brand's own three settings (2026-08-19).
--
-- On the profile rather than in properties: an accent is not a deployment detail, it is what the
-- company looks like, and a company that rebrands should not need a release to do it. Nullable
-- throughout — empty means "use the compiled default", so an installation that never touches this
-- looks exactly as it did.
--
alter table company_profile add column brand_accent varchar(32);
alter table company_profile add column brand_radius varchar(16);
alter table company_profile add column brand_font varchar(160);
