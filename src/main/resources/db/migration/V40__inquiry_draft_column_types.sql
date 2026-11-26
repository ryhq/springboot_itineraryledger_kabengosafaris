-- The columns V39 added are the right columns in the wrong types.
--
-- `ddl-auto=validate` compares every column against what Hibernate derives from the Java field,
-- and refuses to build the SessionFactory on a mismatch. It is not fussiness: a CHAR(36) pads
-- with spaces where a VARCHAR does not, so a token written by one and read by the other stops
-- matching. The application would not start:
--
--   Schema-validation: wrong column type encountered in column [draft_token] in table
--   [booking_inquiries]; found [char (Types#CHAR)], but expecting [varchar(36) (Types#VARCHAR)]
--
-- Two were wrong, both for the same reason - the migration was written in the types a person
-- would pick by hand rather than the types the mapping actually produces:
--
--   draft_token  CHAR(36)  -> a String with length = 36 is varchar(36), never char
--   last_step    SMALLINT  -> an Integer is int, whatever its range in practice
--
-- V39 is left exactly as it is rather than corrected in place. It has already run on one of the
-- two databases, and Flyway checksums what it has applied: editing it would stop that
-- installation deploying at all, which is a worse failure than the one being fixed.

ALTER TABLE booking_inquiries
  MODIFY COLUMN draft_token VARCHAR(36) NULL,
  MODIFY COLUMN last_step   INT         NULL;
