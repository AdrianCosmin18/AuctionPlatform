create sequence notifications_seq start with 1 increment by 1;

create table notifications (
  id bigint primary key default nextval('notifications_seq'),
  user_id bigint not null references users(id),
  auction_id bigint null references auctions(id) on delete set null,
  type varchar(80) not null,
  title varchar(255) not null,
  message varchar(1000) not null,
  is_read boolean not null default false,
  created_at timestamptz not null default now(),
  read_at timestamptz null
);

create index idx_notifications_user_created_at on notifications (user_id, created_at desc);
create index idx_notifications_user_is_read on notifications (user_id, is_read);
