alter table auctions
  add column category_code varchar(80) null,
  add column subcategory_code varchar(80) null,
  add column creator_author varchar(255) null,
  add column estimated_year int null,
  add column language_code varchar(32) null,
  add column item_condition varchar(50) null,
  add column authenticity_status varchar(50) null,
  add column provenance text null;

create index idx_auctions_category_code on auctions (category_code);

comment on column auctions.category_code is 'Controlled domain category for the auctioned cultural item.';
comment on column auctions.subcategory_code is 'Controlled subcategory tied to the main domain category.';
comment on column auctions.creator_author is 'Author, creator, or issuing person associated with the item.';
comment on column auctions.estimated_year is 'Approximate year associated with the item.';
comment on column auctions.language_code is 'Language code or short language descriptor for the item.';
comment on column auctions.item_condition is 'Conservation condition of the item.';
comment on column auctions.authenticity_status is 'Verification status for the item authenticity.';
comment on column auctions.provenance is 'Short provenance note for the item.';
