-- One invoice, several payers.
--
-- A group trip is organised by one person and paid for by six. Before this there was no way to
-- record that: `payments` has no payer, so every receipt came out in the invoice customer's name,
-- and the only alternative was for the other five to wire their money to the organiser and the
-- organiser to send one lump on, which makes a private individual carry several thousand dollars
-- of other people's money with no protection at all.
--
-- A share says who is paying which part of an invoice. The invoice itself is untouched: still one
-- per safari, still addressed to the lead traveller, still the sum of every payment against it.
-- That matters, because `invoices.safari_id` carries a unique key and the quote is recovered later
-- by (itinerary, customer) -- a second customer on the booking would break the lookup that builds
-- the invoice in the first place.
--
-- No amount_paid, balance or status column. The invoice has never stored its own and for the same
-- reason: a stored total is a second copy of the truth that drifts from the payments the moment
-- anything is corrected. They are summed on demand.

create table invoice_shares (
    id bigint not null auto_increment,
    invoice_id bigint not null,
    customer_id bigint,
    payer_name varchar(200) not null,
    payer_email varchar(255),
    amount decimal(15,2) not null,
    currency varchar(3) not null,
    share_reference varchar(100),
    notes TEXT,
    display_order integer,
    is_active bit not null,
    created_at datetime(6) not null,
    updated_at datetime(6),
    primary key (id)
) engine=InnoDB;

create index idx_invoice_share_invoice on invoice_shares (invoice_id);
create index idx_invoice_share_customer on invoice_shares (customer_id);

alter table invoice_shares
    add constraint fk_invoice_share_invoice
    foreign key (invoice_id) references invoices (id) on delete cascade;

-- The customer link is optional and must not take the share with it: a payer who is also a
-- customer record may be deleted long after the money arrived, and the receipt still happened.
alter table invoice_shares
    add constraint fk_invoice_share_customer
    foreign key (customer_id) references customers (id) on delete set null;

-- Nullable, and null keeps today's behaviour exactly: the payment belongs to the invoice as a
-- whole. Every existing row stays null, so nothing already recorded changes meaning.
alter table payments
    add column share_id bigint null;

create index idx_payment_share on payments (share_id);

-- Deliberately `set null` rather than `cascade`. Deleting a share must never delete the record of
-- money that was actually received; the payment falls back to belonging to the invoice at large.
alter table payments
    add constraint fk_payment_share
    foreign key (share_id) references invoice_shares (id) on delete set null;
