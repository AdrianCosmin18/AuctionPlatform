alter table auctions
  add column winner_id bigint null references users(id),
  add column winning_bid_id bigint null references bids(id),
  add column final_price numeric(12,2) null,
  add column closed_at timestamptz null,
  add column closed_reason varchar(30) null;

comment on column auctions.winner_id is 'User id of the winner after auction close';
comment on column auctions.winning_bid_id is 'Winning bid id after auction close';
comment on column auctions.final_price is 'Final price captured when the auction ends';
comment on column auctions.closed_at is 'Timestamp when the auction was closed';
comment on column auctions.closed_reason is 'Reason for closure: MANUAL, EXPIRED, BUY_NOW, CANCELLED';
