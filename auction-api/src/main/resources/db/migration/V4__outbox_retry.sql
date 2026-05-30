alter table outbox_events
add column retry_count int not null default 0;

alter table outbox_events
add column last_error text;

comment on column outbox_events.retry_count is 'Number of publish attempts performed for the outbox event.';
comment on column outbox_events.last_error is 'Last publishing error captured for troubleshooting and retries.';
