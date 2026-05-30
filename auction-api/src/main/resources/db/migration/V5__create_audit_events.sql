create table audit_events (
  id bigserial primary key,
  event_type varchar(100) not null,
  aggregate_id bigint not null,
  payload jsonb not null,
  processed_at timestamptz not null default now(),
  source varchar(50) not null
);

create index idx_audit_events_event_type on audit_events(event_type);
create index idx_audit_events_aggregate_id on audit_events(aggregate_id);

comment on table audit_events is 'Worker-side audit log of processed domain events.';
comment on column audit_events.id is 'Primary key of the audit event.';
comment on column audit_events.event_type is 'Domain event type consumed by the worker.';
comment on column audit_events.aggregate_id is 'Identifier of the aggregate referenced by the event payload.';
comment on column audit_events.payload is 'Original event payload persisted for auditing.';
comment on column audit_events.processed_at is 'Timestamp when the worker processed the event.';
comment on column audit_events.source is 'Logical source component that processed the event.';
