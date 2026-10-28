-- A review is a conversation about day four, not a verdict on a document (2026-10-27).
--
-- Without an anchor, a client's only way to say "that lodge is too far from the gate" is to
-- decline the whole quotation or to write an email, and both of those throw away which part
-- they meant. The round trip that follows is what the portal exists to remove.
--
-- WHAT IS ANCHORED, AND WHY IT IS ANCHORED THREE WAYS:
--
--   day_number    the day, by its number rather than by its row. A quotation's day rows can be
--                 replaced wholesale when staff rebuild a stretch of the trip, and a comment
--                 about day four is about day four whatever row happens to carry it.
--
--   target_id     the real primary key of the line, for a comment about one lodge or one
--                 activity. The REAL key, never the obfuscated form: this installation mints a
--                 fresh Hashids salt on every restart unless one is configured, so an
--                 obfuscated id stored today addresses a different row next week.
--
--   target_label  what that line was called at the time. Denormalised on purpose, like
--                 author_name: a comment reading "too far from the gate" against a row somebody
--                 has since deleted is a sentence nobody can act on.
--
-- VISIBILITY IS NOT A DISPLAY DECISION. `visible_to_client` false means the office talking among
-- themselves on the same thread, and a query that forgets it publishes an internal discussion of
-- a customer's budget to the customer. The portal's read path filters in SQL rather than in a
-- template, and NoPortalResponseCarriesOurCostTest's sibling pins it.
--
-- Varchar rather than native enums, for the reason V27 through V30 all give.

create table quote_proposal_comments (
    id bigint not null auto_increment,
    quote_id bigint not null,
    journey_id bigint,
    -- Which round it was written against, so a comment answered two versions ago can be shown
    -- as historical rather than as an outstanding objection.
    revision_number integer,
    -- WHOLE, DAY or LINE.
    anchor varchar(20) not null,
    day_number integer,
    target_id bigint,
    target_label varchar(255),
    body text not null,
    -- CLIENT or STAFF.
    author_kind varchar(20) not null,
    author_name varchar(200),
    author_user_id bigint,
    -- False means the office talking among themselves. Never true for a client's own comment:
    -- there is nowhere for a client to keep a secret from the only other party.
    visible_to_client bit not null,
    resolved_at datetime(6),
    resolved_by varchar(200),
    created_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create index idx_proposal_comment_quote on quote_proposal_comments (quote_id, id);
create index idx_proposal_comment_anchor on quote_proposal_comments (quote_id, day_number);

alter table quote_proposal_comments
    add constraint fk_proposal_comment_quote
    foreign key (quote_id) references quotes (id) on delete cascade;

alter table quote_proposal_comments
    add constraint fk_proposal_comment_journey
    foreign key (journey_id) references portal_journeys (id) on delete set null;
