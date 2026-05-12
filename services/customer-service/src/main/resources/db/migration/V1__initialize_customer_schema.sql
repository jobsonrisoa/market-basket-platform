create table customer_profiles (
  id uuid primary key,
  auth_user_id uuid not null unique,
  display_name varchar(160),
  phone varchar(40),
  default_locale varchar(16) not null,
  status varchar(32) not null,
  address_preferences jsonb not null default '{}'::jsonb,
  communication_preferences jsonb not null default '{}'::jsonb,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create index idx_customer_profiles_status on customer_profiles(status);
