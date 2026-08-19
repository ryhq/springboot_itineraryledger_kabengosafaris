-- Feature switches (2026-08-19).
--
-- One codebase serves several tour companies, and they do not all buy the same product. A row here
-- says whether this installation has a fleet, credit notes, availability requests, website content or
-- translation. Absent rows mean the default, which is ON — otherwise a release that adds a feature
-- would silently hide it from every existing company.
--
-- Shaped like the other settings tables so the panel's existing settings page can render it.
--
create table feature_settings (
    id bigint not null auto_increment,
    setting_key varchar(100) not null,
    setting_value text not null,
    data_type enum ('BOOLEAN','DOUBLE','INTEGER','LONG','STRING') not null,
    description text,
    category varchar(40) not null,
    active bit not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uq_feature_setting_key unique (setting_key)
) engine=InnoDB;
