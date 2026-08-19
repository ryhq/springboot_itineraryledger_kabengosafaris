-- The letter mark (2026-08-19).
--
-- What the panel draws where a logo would go, before anybody has uploaded one — and next to the
-- company name in the sidebar. Derived from the trading name when blank ("Kabengo Safaris" -> "K"),
-- but a company whose initial is not the right mark ("JAT" for Jatelo African Travels) can say so.
--
alter table company_profile add column brand_mark varchar(8);
