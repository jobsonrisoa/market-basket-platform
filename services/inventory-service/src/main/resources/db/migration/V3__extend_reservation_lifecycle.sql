alter table inventory_reservations
  add column expires_at timestamp with time zone,
  add column expired_at timestamp with time zone,
  add column committed_at timestamp with time zone;

alter table inventory_reservations
  drop constraint ck_inventory_reservations_status;

alter table inventory_reservations
  add constraint ck_inventory_reservations_status
    check (status in ('ACTIVE', 'RELEASED', 'EXPIRED', 'COMMITTED'));

alter table inventory_reservations
  add constraint uk_inventory_reservations_reference
    unique (stock_id, requested_by, reference_id);

create index idx_inventory_reservations_expiry
  on inventory_reservations(status, expires_at);
