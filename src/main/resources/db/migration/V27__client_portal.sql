-- One link per client per itinerary (2026-10-22).
--
-- Every step of a client's journey is currently a PDF we email: itinerary, quotation, invoice.
-- A document is a dead end, so every reaction to it comes back as a message and becomes staff
-- work, even when it is something the client could have done themselves. A journey is the live
-- record behind one link, and the client can act on it.
--
-- Nothing here grants access on its own. The journey token is an ADDRESS: it identifies which
-- booking a page is about and opens only the parts of it that are not sensitive. The key is a
-- short code sent to an address already on the customer record, typed into the page the client
-- already has open, because a link that logs you in travels through email and email is full of
-- machines that click things.
--
-- Secrets are stored hashed. The device token and the verification code are both bearer secrets,
-- so a leaked backup of these tables must not be a leaked set of sessions. The journey token is
-- not hashed, because it is a lookup key with no authority and the row has to be findable by it.
--
-- Enum columns here are varchar, not MySQL's native enum. Every native enum in this schema is
-- frozen at the values that existed when the baseline was taken, ddl-auto does not widen one, and
-- adding a Java constant to such a column has failed at commit five times in this codebase. A
-- varchar column accepts tomorrow's constant without a migration.

-- ---------------------------------------------------------------------------------------------
-- The journey
-- ---------------------------------------------------------------------------------------------

create table portal_journeys (
    id bigint not null auto_increment,
    -- the address. url-safe Base64 of 32 random bytes, as the newsletter confirmation does it
    token varchar(64) not null,
    customer_id bigint not null,
    itinerary_id bigint,
    quote_id bigint,
    invoice_id bigint,
    safari_id bigint,
    -- OPEN, CLOSED, REVOKED
    status varchar(30) not null,
    -- RESPOND, CHOOSE_OFFERED, CHOOSE_CATALOGUE; null means inherit the itinerary or the company
    editing_level varchar(30),
    -- the operational override: a request that smells wrong is frozen here alone
    locked bit not null default 0,
    -- where the review loop stands. OPEN_TO_CLIENT, SUBMITTED, WITH_OPERATOR, RETURNED, AGREED, DECLINED
    proposal_state varchar(30) not null,
    proposal_state_at datetime(6),
    -- the staff member currently holding the pen, and from which revision they took it
    held_by_user_id bigint,
    held_from_revision bigint,
    -- when behavioural data for this journey may be purged: safari end, or closure, plus three months
    purge_after date,
    created_at datetime(6) not null,
    updated_at datetime(6),
    expires_at datetime(6),
    revoked_at datetime(6),
    revoked_reason varchar(500),
    primary key (id)
) engine=InnoDB;

create unique index uk_portal_journey_token on portal_journeys (token);
create index idx_portal_journey_customer on portal_journeys (customer_id);
create index idx_portal_journey_quote on portal_journeys (quote_id);
create index idx_portal_journey_purge on portal_journeys (purge_after);

-- A journey belongs to a customer and dies with them.
alter table portal_journeys
    add constraint fk_portal_journey_customer
    foreign key (customer_id) references customers (id) on delete cascade;

-- The documents it points at are set null rather than cascaded: deleting a quote must not silently
-- delete the record that a client was given access and what they did with it.
alter table portal_journeys
    add constraint fk_portal_journey_itinerary
    foreign key (itinerary_id) references itineraries (id) on delete set null;

alter table portal_journeys
    add constraint fk_portal_journey_quote
    foreign key (quote_id) references quotes (id) on delete set null;

alter table portal_journeys
    add constraint fk_portal_journey_invoice
    foreign key (invoice_id) references invoices (id) on delete set null;

alter table portal_journeys
    add constraint fk_portal_journey_safari
    foreign key (safari_id) references safaris (id) on delete set null;

-- ---------------------------------------------------------------------------------------------
-- Who may open it
-- ---------------------------------------------------------------------------------------------

create table portal_participants (
    id bigint not null auto_increment,
    journey_id bigint not null,
    -- stored lowercase; the address the code is sent to
    email varchar(255) not null,
    display_name varchar(200),
    -- LEAD or READER. Only the lead may propose, accept or decline
    participant_role varchar(30) not null,
    verified_at datetime(6),
    invited_by_participant_id bigint,
    invited_at datetime(6),
    -- which privacy policy they were shown when they first verified
    policy_version varchar(30),
    policy_accepted_at datetime(6),
    is_active bit not null default 1,
    created_at datetime(6) not null,
    updated_at datetime(6),
    primary key (id)
) engine=InnoDB;

-- One row per address per journey: a second invitation to the same person is the same participant.
create unique index uk_portal_participant on portal_participants (journey_id, email);
create index idx_portal_participant_email on portal_participants (email);

alter table portal_participants
    add constraint fk_portal_participant_journey
    foreign key (journey_id) references portal_journeys (id) on delete cascade;

alter table portal_participants
    add constraint fk_portal_participant_inviter
    foreign key (invited_by_participant_id) references portal_participants (id) on delete set null;

-- ---------------------------------------------------------------------------------------------
-- Trusted devices
-- ---------------------------------------------------------------------------------------------

create table portal_devices (
    id bigint not null auto_increment,
    participant_id bigint not null,
    -- SHA-256 of the cookie value. The cookie itself is never stored
    token_hash varchar(64) not null,
    label varchar(200),
    ip_address varchar(64),
    user_agent varchar(500),
    created_at datetime(6) not null,
    last_seen_at datetime(6),
    expires_at datetime(6) not null,
    revoked_at datetime(6),
    primary key (id)
) engine=InnoDB;

create unique index uk_portal_device_token on portal_devices (token_hash);
create index idx_portal_device_participant on portal_devices (participant_id);

alter table portal_devices
    add constraint fk_portal_device_participant
    foreign key (participant_id) references portal_participants (id) on delete cascade;

-- ---------------------------------------------------------------------------------------------
-- Verification codes
-- ---------------------------------------------------------------------------------------------

create table portal_codes (
    id bigint not null auto_increment,
    participant_id bigint not null,
    code_hash varchar(64) not null,
    expires_at datetime(6) not null,
    attempts integer not null default 0,
    used_at datetime(6),
    created_at datetime(6) not null,
    ip_address varchar(64),
    primary key (id)
) engine=InnoDB;

create index idx_portal_code_participant on portal_codes (participant_id);
create index idx_portal_code_expiry on portal_codes (expires_at);

alter table portal_codes
    add constraint fk_portal_code_participant
    foreign key (participant_id) references portal_participants (id) on delete cascade;

-- ---------------------------------------------------------------------------------------------
-- What was opened
-- ---------------------------------------------------------------------------------------------
--
-- Support ("she says the invoice is not there") and insight (where clients stall). This is the
-- table the retention job empties: behavioural data about an identified person, kept for three
-- months after the trip ends or the booking falls through, and no longer.

create table portal_access_events (
    id bigint not null auto_increment,
    journey_id bigint not null,
    participant_id bigint,
    device_id bigint,
    -- TRIP, QUOTE, INVOICE, DOCUMENT, DEVICES, CHANGES, and the actions
    section varchar(60) not null,
    detail varchar(200),
    ip_address varchar(64),
    user_agent varchar(500),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create index idx_portal_event_journey on portal_access_events (journey_id, created_at);

alter table portal_access_events
    add constraint fk_portal_event_journey
    foreign key (journey_id) references portal_journeys (id) on delete cascade;

alter table portal_access_events
    add constraint fk_portal_event_participant
    foreign key (participant_id) references portal_participants (id) on delete set null;

alter table portal_access_events
    add constraint fk_portal_event_device
    foreign key (device_id) references portal_devices (id) on delete set null;

-- ---------------------------------------------------------------------------------------------
-- Legal documents
-- ---------------------------------------------------------------------------------------------
--
-- The privacy notice has to be readable from the portal and from both public websites, and it has
-- to be versioned: a client consented to a particular text, and "the policy as it is today" is not
-- an answer to which one. Managed here rather than hand-written into each website, because there
-- are two of those and they are copies of one another.

create table legal_documents (
    id bigint not null auto_increment,
    -- PRIVACY_POLICY or TERMS
    document_type varchar(40) not null,
    version varchar(30) not null,
    title varchar(200) not null,
    -- The source text, not markup. There is no HTML sanitiser on this side, and a notice edited
    -- in the panel and rendered on two public websites would be a stored-script hole waiting for
    -- one careless paste. It is written in a small plain-text markup (a blank line is a
    -- paragraph, `## ` a heading, `- ` a bullet) and rendered with everything else escaped.
    body longtext not null,
    effective_from date not null,
    published bit not null default 0,
    created_at datetime(6) not null,
    updated_at datetime(6),
    primary key (id)
) engine=InnoDB;

create unique index uk_legal_document_version on legal_documents (document_type, version);
create index idx_legal_document_published on legal_documents (document_type, published);
