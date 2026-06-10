alter table auctions
  add column reserve_price numeric(12,2) null,
  add column reserve_met boolean null;

comment on column auctions.reserve_price is 'Optional hidden minimum acceptable sale price';
comment on column auctions.reserve_met is 'Whether the reserve price has been met; null when no reserve price exists';
