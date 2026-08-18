create table tasks (
    id bigserial primary key,
    title varchar(160) not null,
    description varchar(4000),
    category varchar(40) not null,
    status varchar(20) not null,
    scheduled_date date,
    scheduled_time time without time zone,
    due_date date,
    due_time time without time zone,
    reminder_date date,
    priority varchar(20) not null,
    responsible_user_id bigint not null,
    created_by bigint not null,
    completed_at timestamp without time zone,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    constraint fk_tasks_responsible_user foreign key (responsible_user_id) references users (id),
    constraint fk_tasks_created_by foreign key (created_by) references users (id),
    constraint ck_tasks_category check (category in (
        'DEADLINE', 'HEARING', 'PENDING_DOCUMENT', 'CLIENT_MEETING', 'INTERNAL_MEETING',
        'CASE_PENDING_ITEM', 'URGENT_PROTOCOL', 'DOCUMENT_COLLECTION', 'OTHER'
    )),
    constraint ck_tasks_status check (status in ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELED')),
    constraint ck_tasks_priority check (priority in ('NORMAL', 'HIGH', 'URGENT')),
    constraint ck_tasks_scheduled_time check (scheduled_time is null or scheduled_date is not null),
    constraint ck_tasks_due_time check (due_time is null or due_date is not null),
    constraint ck_tasks_reminder check (
        reminder_date is null or (due_date is not null and reminder_date <= due_date)
    ),
    constraint ck_tasks_completion check (
        (status = 'COMPLETED' and completed_at is not null)
        or (status <> 'COMPLETED' and completed_at is null)
    )
);
