create table users (
  id uuid not null,
  email varchar(255) not null,
  email_verified boolean not null,
  status varchar(255) not null,
  account_profile varchar(255) not null,
  customer_profile_type varchar(255),
  created_at timestamp(6) with time zone not null,
  updated_at timestamp(6) with time zone not null,
  primary key (id)
);

alter table users
  add constraint uk_users_email unique (email);

create table user_roles (
  user_id uuid not null,
  role varchar(255) not null,
  primary key (user_id, role)
);

alter table user_roles
  add constraint fk_user_roles_user
  foreign key (user_id) references users;

create table password_credentials (
  user_id uuid not null,
  password_hash varchar(255) not null,
  changed_at timestamp(6) with time zone not null,
  primary key (user_id)
);

create table oauth_accounts (
  id uuid not null,
  user_id uuid not null,
  provider varchar(255) not null,
  provider_subject varchar(255) not null,
  email varchar(255) not null,
  created_at timestamp(6) with time zone not null,
  primary key (id)
);

alter table oauth_accounts
  add constraint uk_oauth_accounts_provider_subject unique (provider, provider_subject);

create table refresh_token_families (
  id uuid not null,
  user_id uuid not null,
  status varchar(255) not null,
  revoked_at timestamp(6) with time zone,
  revoke_reason varchar(255),
  created_at timestamp(6) with time zone not null,
  primary key (id)
);

create table refresh_tokens (
  id uuid not null,
  family_id uuid not null,
  user_id uuid not null,
  token_hash varchar(255) not null,
  previous_token_id uuid,
  expires_at timestamp(6) with time zone not null,
  created_at timestamp(6) with time zone not null,
  used_at timestamp(6) with time zone,
  revoked_at timestamp(6) with time zone,
  primary key (id)
);

alter table refresh_tokens
  add constraint uk_refresh_tokens_token_hash unique (token_hash);

create table outbox_events (
  event_id uuid not null,
  aggregate_id varchar(255) not null,
  event_type varchar(255) not null,
  version integer not null,
  occurred_at timestamp(6) with time zone not null,
  correlation_id varchar(255) not null,
  payload text not null,
  status varchar(255) not null,
  published_at timestamp(6) with time zone,
  primary key (event_id)
);
