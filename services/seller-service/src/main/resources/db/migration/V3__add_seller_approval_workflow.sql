alter table seller_stores
  add column approval_status varchar(32) not null default 'PENDING_REVIEW',
  add column submitted_at timestamp with time zone,
  add column reviewed_at timestamp with time zone,
  add column reviewed_by_user_id uuid,
  add column review_notes varchar(1000);

update seller_stores
set submitted_at = created_at
where submitted_at is null;

alter table seller_stores
  alter column submitted_at set not null,
  alter column approval_status drop default,
  add constraint ck_seller_stores_approval_status
    check (approval_status in ('PENDING_REVIEW', 'APPROVED', 'REJECTED'));

create index idx_seller_stores_approval_status on seller_stores(approval_status);
