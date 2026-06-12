create sequence user_profiles_seq start with 1 increment by 1;

create table user_profiles (
  id bigint primary key default nextval('user_profiles_seq'),
  user_id bigint not null unique references users(id) on delete cascade,
  first_name varchar(100) null,
  last_name varchar(100) null,
  phone varchar(50) null,
  country varchar(100) null,
  city varchar(100) null,
  address_line_1 varchar(255) null,
  address_line_2 varchar(255) null,
  postal_code varchar(50) null,
  updated_at timestamptz not null default now()
);

create index idx_user_profiles_user_id on user_profiles (user_id);
