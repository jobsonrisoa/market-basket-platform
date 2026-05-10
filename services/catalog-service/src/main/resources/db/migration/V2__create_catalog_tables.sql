create table catalog_categories (
  id uuid primary key,
  name varchar(255) not null unique,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table catalog_products (
  id uuid primary key,
  seller_id uuid not null,
  category_id uuid not null references catalog_categories (id),
  name varchar(255) not null,
  description text,
  unit varchar(64) not null,
  package_size varchar(128) not null,
  price_amount numeric(12, 2) not null,
  currency varchar(3) not null,
  status varchar(32) not null,
  created_at timestamptz not null,
  updated_at timestamptz not null,
  constraint catalog_products_price_non_negative check (price_amount >= 0),
  constraint catalog_products_status_valid check (status in ('DRAFT', 'PUBLISHED', 'UNPUBLISHED')),
  constraint catalog_products_currency_length check (char_length(currency) = 3)
);

create index idx_catalog_products_seller_id on catalog_products (seller_id);
create index idx_catalog_products_category_id on catalog_products (category_id);
create index idx_catalog_products_status on catalog_products (status);
