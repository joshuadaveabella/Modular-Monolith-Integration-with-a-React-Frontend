-- Run this in the Supabase SQL Editor (or via psql) before starting the
-- Spring Boot app. The app uses ddl-auto=validate, so it expects these
-- tables to already exist with these exact columns.

create table if not exists inventory (
    product_id varchar(50) primary key,
    name       varchar(255) not null,
    stock      integer not null default 0
);

create table if not exists orders (
    order_id   bigserial primary key,
    product_id varchar(50) not null references inventory(product_id),
    quantity   integer not null,
    status     varchar(20) not null,
    reason     varchar(255),
    created_at timestamp not null default now()
);

insert into inventory (product_id, name, stock) values
    ('P100', 'Wireless Mouse', 25),
    ('P200', 'Mechanical Keyboard', 10),
    ('P300', 'USB-C Hub', 0)
on conflict (product_id) do nothing;
