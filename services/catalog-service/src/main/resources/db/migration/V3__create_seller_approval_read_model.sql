create table catalog_seller_approvals (
  seller_id uuid primary key,
  seller_name varchar(255) not null,
  approval_status varchar(32) not null,
  reviewed_by_user_id uuid not null,
  reviewed_at timestamptz not null,
  updated_at timestamptz not null,
  constraint catalog_seller_approvals_status_valid check (approval_status in ('PENDING_REVIEW', 'APPROVED', 'REJECTED'))
);

create index idx_catalog_seller_approvals_status on catalog_seller_approvals (approval_status);
