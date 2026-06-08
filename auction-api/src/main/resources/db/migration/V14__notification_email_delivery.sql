alter table notifications
    add column email_delivery_status varchar(30) not null default 'PENDING',
    add column email_sent_at timestamptz null,
    add column email_last_attempt_at timestamptz null,
    add column email_last_error varchar(1000) null;

create index idx_notifications_email_delivery_status on notifications (email_delivery_status, created_at);
