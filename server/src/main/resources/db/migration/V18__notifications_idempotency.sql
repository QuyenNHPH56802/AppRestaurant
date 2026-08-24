-- V2.3 / V18: notifications.idempotency_key
--
-- Purpose: deduplicate notification creation when the same business event
-- triggers multiple attempts (e.g. manager UI retries, double-clicks,
-- network blip on confirmation).
--
-- The natural key for a notification is roughly:
--   (recipient_user_id, type, ref_entity_type, ref_entity_id)
--   e.g. (employee=42, type='SHIFT_ASSIGNED', ref_entity_type='shift_assignment', ref_entity_id=123)
--
-- We don't want to enforce uniqueness on the natural key directly because
-- notifications of the SAME type can legitimately be sent multiple times
-- (e.g. SHIFT_REMINDER at T-24h AND at T-2h for the same shift). Instead,
-- callers compute a stable idempotency_key for *this specific event firing*
-- and we de-duplicate on that.
--
-- Convention for key:
--   "<type>:<refEntityType>:<refEntityId>:<action>"
--   e.g. "SHIFT_ASSIGNED:shift_assignment:123:create"
--   e.g. "SHIFT_CANCELLED:shift_assignment:123:cancel"
--
-- `idempotency_key` is NULLABLE because:
--   - V1/V2.2 notifications have no such concept and were inserted without
--     a key.
--   - System-generated notifications (e.g. test pings) may also leave it null.
--
-- The unique index is PARTIAL (`WHERE idempotency_key IS NOT NULL`) so the
-- many legacy NULL rows coexist without conflict.
--
-- This is a pure additive ALTER + CREATE INDEX — no data is rewritten.

ALTER TABLE notifications ADD COLUMN idempotency_key TEXT;

CREATE UNIQUE INDEX ux_notifications_idempotency
    ON notifications(idempotency_key)
    WHERE idempotency_key IS NOT NULL;
