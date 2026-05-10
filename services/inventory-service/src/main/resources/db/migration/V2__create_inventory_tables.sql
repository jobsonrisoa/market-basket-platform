create table inventory_stocks (
  id uuid primary key,
  seller_id uuid not null,
  product_id uuid not null,
  on_hand_quantity numeric(19, 3) not null,
  reserved_quantity numeric(19, 3) not null,
  unit varchar(32) not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  constraint uk_inventory_stocks_seller_product unique (seller_id, product_id),
  constraint ck_inventory_stocks_on_hand_non_negative check (on_hand_quantity >= 0),
  constraint ck_inventory_stocks_reserved_non_negative check (reserved_quantity >= 0),
  constraint ck_inventory_stocks_reserved_not_above_on_hand check (reserved_quantity <= on_hand_quantity)
);

create table inventory_reservations (
  id uuid primary key,
  stock_id uuid not null references inventory_stocks(id),
  seller_id uuid not null,
  product_id uuid not null,
  quantity numeric(19, 3) not null,
  unit varchar(32) not null,
  requested_by varchar(128) not null,
  reference_id varchar(128) not null,
  status varchar(32) not null,
  created_at timestamp with time zone not null,
  released_at timestamp with time zone,
  constraint ck_inventory_reservations_quantity_positive check (quantity > 0),
  constraint ck_inventory_reservations_status check (status in ('ACTIVE', 'RELEASED'))
);

create index idx_inventory_stocks_seller_id on inventory_stocks(seller_id);
create index idx_inventory_stocks_product_id on inventory_stocks(product_id);
create index idx_inventory_reservations_stock_id on inventory_reservations(stock_id);
create index idx_inventory_reservations_status on inventory_reservations(status);
