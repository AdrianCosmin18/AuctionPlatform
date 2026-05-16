create sequence users_seq start with 1 increment by 1;
create sequence auctions_seq start with 1 increment by 1;
create sequence bids_seq start with 1 increment by 1;
create sequence outbox_events_seq start with 1 increment by 1;

create table users (
  id bigint primary key default nextval('users_seq'),
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  role varchar(50) not null,
  created_at timestamptz not null default now()
);

create table auctions (
  id bigint primary key default nextval('auctions_seq'),
  title varchar(255) not null,
  description text,
  start_price numeric(12,2) not null,
  current_price numeric(12,2) not null,
  min_increment numeric(12,2) not null,
  status varchar(30) not null,
  start_time timestamptz null,
  end_time timestamptz null,
  anti_sniping_window_sec int not null default 30,
  anti_sniping_extend_sec int not null default 30,
  created_by bigint not null references users(id),
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table bids (
  id bigint primary key default nextval('bids_seq'),
  auction_id bigint not null references auctions(id),
  bidder_id bigint not null references users(id),
  amount numeric(12,2) not null,
  created_at timestamptz not null default now()
);

create index idx_bids_auction_created_at on bids (auction_id, created_at desc);

create table outbox_events (
  id bigint primary key default nextval('outbox_events_seq'),
  aggregate_type varchar(50) not null,
  aggregate_id bigint not null,
  event_type varchar(100) not null,
  payload jsonb not null,
  status varchar(20) not null,
  created_at timestamptz not null default now(),
  published_at timestamptz null
);

create index idx_outbox_status_created_at on outbox_events (status, created_at);
