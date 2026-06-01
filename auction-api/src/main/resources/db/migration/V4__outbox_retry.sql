alter table outbox_events
add column retry_count int not null default 0;

alter table outbox_events
add column last_error text;
