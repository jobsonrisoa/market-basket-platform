create table seller_stores (
  id uuid primary key,
  name varchar(255) not null,
  owner_user_id uuid not null,
  created_at timestamp with time zone not null
);

create table seller_memberships (
  id uuid primary key,
  seller_id uuid not null references seller_stores(id),
  user_id uuid not null,
  role varchar(32) not null,
  status varchar(32) not null,
  created_at timestamp with time zone not null,
  removed_at timestamp with time zone,
  constraint uk_seller_memberships_seller_user unique (seller_id, user_id),
  constraint ck_seller_memberships_role check (role in ('OWNER', 'STAFF')),
  constraint ck_seller_memberships_status check (status in ('ACTIVE', 'REMOVED'))
);

create index idx_seller_memberships_seller_id on seller_memberships(seller_id);
create index idx_seller_memberships_user_id on seller_memberships(user_id);
