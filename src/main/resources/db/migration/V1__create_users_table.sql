create table users (
    id bigserial primary key,
    name varchar(120) not null,
    email varchar(254) not null,
    password_hash varchar(255) not null,
    role varchar(20) not null,
    active boolean not null default true,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    constraint uk_users_email unique (email),
    constraint ck_users_role check (role in ('ADMIN', 'USER')),
    constraint ck_users_email_lowercase check (email = lower(trim(email)))
);
