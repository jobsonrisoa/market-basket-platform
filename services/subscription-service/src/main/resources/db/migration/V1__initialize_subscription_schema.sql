create table subscription_plans (
  id uuid primary key,
  seller_id uuid not null,
  product_id uuid not null,
  stock_id uuid not null,
  basket_size varchar(32) not null,
  cadence varchar(32) not null,
  quantity numeric(12, 3) not null,
  unit varchar(32) not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

create table customer_subscriptions (
  id uuid primary key,
  customer_id uuid not null,
  plan_id uuid not null references subscription_plans (id),
  seller_id uuid not null,
  product_id uuid not null,
  stock_id uuid not null,
  basket_size varchar(32) not null,
  cadence varchar(32) not null,
  quantity numeric(12, 3) not null,
  unit varchar(32) not null,
  status varchar(32) not null,
  next_renewal_date date not null,
  current_draft_order_id uuid not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null,
  cancelled_at timestamp with time zone
);

create index idx_customer_subscriptions_customer_created_at
  on customer_subscriptions (customer_id, created_at);

create index idx_customer_subscriptions_due
  on customer_subscriptions (status, next_renewal_date);
