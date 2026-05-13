create table orders (
  id uuid primary key,
  customer_id uuid not null,
  seller_id uuid not null,
  product_id uuid not null,
  stock_id uuid not null,
  quantity numeric(12, 3) not null,
  unit varchar(32) not null,
  source varchar(64) not null,
  source_reference_id varchar(128) not null,
  status varchar(32) not null,
  fulfillment_status varchar(32) not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  confirmed_at timestamp with time zone,
  cancelled_at timestamp with time zone,
  fulfilled_at timestamp with time zone
);

create index idx_orders_customer_created_at on orders (customer_id, created_at);
create index idx_orders_source_reference on orders (source, source_reference_id);
