-- What the quotation said each time the pen changed hands (2026-10-27).
--
-- The requirement is that a client and the office read the identical list of changes. The only
-- way to guarantee that is for both to be derived from one stored thing, so this table stores
-- the quotation itself at each handover and the sentences are computed from two rows of it.
-- Storing the sentences instead would mean writing them at the moment of the change, which is
-- the moment we know least about what the other side will need to see.
--
-- `snapshot` is JSON built from QuoteCostEstimationService.buildSyntheticDTO, the same projection
-- the pricing engine reads, so a diff can never disagree with the price it is explaining.
--
-- It holds NO obfuscated ids, and that is not an oversight. This installation mints a fresh
-- Hashids salt on every restart unless one is configured, so an id written into a row today
-- decodes to a different record next week and to nothing at all after a schema change. A
-- snapshot is a historical record and has to outlive a deploy, so everything in it is named:
-- day number, lodge name, board type. Those are also what the sentences are made of.
--
-- Varchar rather than a native enum for `taken_by` and `reason`, for the reason V27 and V28 both
-- give: a native enum in this schema is frozen at the values it was created with, ddl-auto does
-- not widen one, and adding a Java constant to such a column has failed at commit five times.

create table quote_revisions (
    id bigint not null auto_increment,
    quote_id bigint not null,
    -- Null for a revision taken outside the portal, which is every revision of a quotation
    -- nobody has shared yet. The quote is what a revision belongs to; the journey is only how
    -- this one came about.
    journey_id bigint,
    -- 1, 2, 3 within one quotation. What a client is told ("version 3"), so it cannot be the
    -- primary key, which is global and would read as a number out of the thousands.
    revision_number integer not null,
    -- CLIENT, STAFF or SYSTEM: who held the pen for the state being recorded.
    taken_by varchar(30) not null,
    -- PUBLISHED, SUBMITTED, TAKEN_OVER, RETURNED, AGREED, DECLINED.
    reason varchar(40) not null,
    -- The name as it should be shown. Denormalised on purpose: a staff member who leaves must
    -- not turn last year's history into a row of nulls.
    author_name varchar(200),
    author_user_id bigint,
    snapshot longtext not null,
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

-- Two revisions numbered 3 on one quotation would make "version 3" ambiguous in an email that
-- has already been sent. The unique key is the only thing that can prevent it under a race.
create unique index uq_quote_revision_number on quote_revisions (quote_id, revision_number);
create index idx_quote_revision_quote on quote_revisions (quote_id, id);

-- Cascaded, unlike portal_journeys.quote_id. A journey survives its quotation because the client
-- is still going on the trip; a revision is a photograph of the quotation and is nothing without it.
alter table quote_revisions
    add constraint fk_quote_revision_quote
    foreign key (quote_id) references quotes (id) on delete cascade;

alter table quote_revisions
    add constraint fk_quote_revision_journey
    foreign key (journey_id) references portal_journeys (id) on delete set null;
