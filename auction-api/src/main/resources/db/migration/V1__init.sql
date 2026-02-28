create table users (
  id uuid primary key,
  email varchar(255) not null unique,
  password_hash varchar(255) not null,
  role varchar(50) not null,
  created_at timestamptz not null default now()
);

create table auctions (
  id uuid primary key,
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
  created_by uuid not null references users(id),
  version bigint not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table bids (
  id uuid primary key,
  auction_id uuid not null references auctions(id),
  bidder_id uuid not null references users(id),
  amount numeric(12,2) not null,
  created_at timestamptz not null default now()
);

create index idx_bids_auction_created_at on bids (auction_id, created_at desc);

create table outbox_events (
  id uuid primary key,
  aggregate_type varchar(50) not null,
  aggregate_id uuid not null,
  event_type varchar(100) not null,
  payload jsonb not null,
  status varchar(20) not null,
  created_at timestamptz not null default now(),
  published_at timestamptz null
);

create index idx_outbox_status_created_at on outbox_events (status, created_at);
