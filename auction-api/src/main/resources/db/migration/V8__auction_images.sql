create sequence auction_images_seq start with 1 increment by 1;

create table auction_images (
  id bigint primary key default nextval('auction_images_seq'),
  auction_id bigint not null references auctions(id) on delete cascade,
  image_url varchar(1000) not null,
  display_order int not null
);

create index idx_auction_images_auction_order on auction_images (auction_id, display_order);

comment on table auction_images is 'Gallery images linked to an auction';
comment on column auction_images.image_url is 'Remote image URL used by the UI gallery';
comment on column auction_images.display_order is 'Display order inside the gallery';
