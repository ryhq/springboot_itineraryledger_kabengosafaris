--
-- Company profile: who this installation is (2026-08-19).
--
-- Six tables rather than columns on one, because a company has several emails, several numbers,
-- more than one address and a handful of links — each retireable without being forgotten — and a
-- logo is a set of marks, not a file. Bank details are deliberately absent: they live in the
-- Bank accounts module, which the invoice PDF already reads.
--
-- Generated from the JPA metadata with the MySQL8 dialect, like the baseline.
--
create table company_addresses (display_order integer not null, is_active bit not null, is_primary bit not null, company_profile_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6) not null, postal_code varchar(40), city varchar(100), country varchar(100), label varchar(100), region varchar(100), line_one varchar(200), line_two varchar(200), address_type enum ('BILLING','OFFICE','OTHER','POSTAL','WAREHOUSE') not null, primary key (id)) engine=InnoDB;
create table company_assets (is_active bit not null, company_profile_id bigint not null, created_at datetime(6) not null, file_size bigint, id bigint not null auto_increment, updated_at datetime(6) not null, mime_type varchar(100), file_name varchar(255) not null, original_file_name varchar(255), asset_kind enum ('FAVICON_DARK','FAVICON_LIGHT','LOGO_DARK','LOGO_EMAIL','LOGO_LIGHT') not null, primary key (id)) engine=InnoDB;
create table company_emails (display_order integer not null, is_active bit not null, is_primary bit not null, company_profile_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6) not null, label varchar(100), email varchar(255) not null, email_type enum ('BILLING','GENERAL','MANAGEMENT','MARKETING','OTHER','RESERVATIONS','SUPPORT') not null, primary key (id)) engine=InnoDB;
create table company_links (display_order integer not null, is_active bit not null, is_primary bit not null, company_profile_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6) not null, label varchar(100), url varchar(500) not null, link_type enum ('BOOKING','FACEBOOK','INSTAGRAM','LINKEDIN','OTHER','TIKTOK','TRIPADVISOR','WEBSITE','X','YOUTUBE') not null, primary key (id)) engine=InnoDB;
create table company_phones (display_order integer not null, is_active bit not null, is_primary bit not null, is_whatsapp bit not null, company_profile_id bigint not null, created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6) not null, country_code varchar(10), phone_number varchar(50) not null, label varchar(100), operating_hours varchar(200), phone_type enum ('EMERGENCY','FAX','LANDLINE','MOBILE','OTHER','RECEPTION','RESERVATIONS','TOLL_FREE','WHATSAPP') not null, primary key (id)) engine=InnoDB;
create table company_profile (default_currency varchar(3), created_at datetime(6) not null, id bigint not null auto_increment, updated_at datetime(6) not null, locale varchar(16), tin varchar(50), vrn varchar(50), timezone varchar(64), licence_number varchar(100), registration_number varchar(100), legal_name varchar(200), trading_name varchar(200) not null, tagline varchar(300), primary key (id)) engine=InnoDB;

-- foreign keys and the one-asset-per-kind guard
alter table company_assets add constraint uq_company_asset_kind unique (company_profile_id, asset_kind);
alter table company_addresses add constraint FKjs6ka5ygw2kbyha2e5jr4xnlj foreign key (company_profile_id) references company_profile (id);
alter table company_assets add constraint FKldlbcsvkbqytieq6w9yfqyyq4 foreign key (company_profile_id) references company_profile (id);
alter table company_emails add constraint FK2w3cnslou8bpd4sx4k6qkw9xp foreign key (company_profile_id) references company_profile (id);
alter table company_links add constraint FKj28h1inrw5fe0mrkeiyrith5f foreign key (company_profile_id) references company_profile (id);
alter table company_phones add constraint FKi6ngq5lhnwyttklpn8wdlp6d0 foreign key (company_profile_id) references company_profile (id);

-- the lookups every child collection is read by
create index idx_company_address_profile on company_addresses (company_profile_id);
create index idx_company_asset_profile on company_assets (company_profile_id);
create index idx_company_email_profile on company_emails (company_profile_id);
create index idx_company_link_profile on company_links (company_profile_id);
create index idx_company_phone_profile on company_phones (company_profile_id);
