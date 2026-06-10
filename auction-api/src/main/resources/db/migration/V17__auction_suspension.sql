alter table auctions
    add column suspended_at timestamptz,
    add column suspended_by bigint,
    add column suspension_reason text;
