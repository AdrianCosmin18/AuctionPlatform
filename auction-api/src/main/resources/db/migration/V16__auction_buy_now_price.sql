alter table auctions
  add column buy_now_price numeric(12,2) null;

comment on column auctions.buy_now_price is 'Optional instant purchase price that closes the auction immediately';
