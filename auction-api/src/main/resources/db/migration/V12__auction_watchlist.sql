create sequence auction_watchlist_seq start with 1 increment by 1;

create table auction_watchlist (
  id bigint primary key default nextval('auction_watchlist_seq'),
  user_id bigint not null references users(id),
  auction_id bigint not null references auctions(id) on delete cascade,
  created_at timestamptz not null default now(),
  constraint uq_auction_watchlist_user_auction unique (user_id, auction_id)
);

create index idx_auction_watchlist_user_created_at on auction_watchlist (user_id, created_at desc);
create index idx_auction_watchlist_auction_id on auction_watchlist (auction_id);
