-- Lab 2 schema. This script recreates the entire schema from scratch,
-- including seed data - run it in the Supabase SQL Editor. Safe to re-run:
-- it drops existing tables first, so it will wipe any existing orders,
-- order items and notifications.

drop table if exists notifications cascade;
drop table if exists order_items cascade;
drop table if exists orders cascade;
drop table if exists inventory cascade;

-- ---------------------------------------------------------------
-- inventory
-- ---------------------------------------------------------------
create table inventory (
    product_id varchar(50) primary key,
    name       varchar(255) not null,
    stock      integer not null default 0
);

-- ---------------------------------------------------------------
-- orders (Lab 2: no product_id/quantity here anymore - an order can
-- now hold several line items, so those moved to order_items.
-- status supports CONFIRMED | REJECTED | CANCELLED)
-- ---------------------------------------------------------------
create table orders (
    order_id   bigserial primary key,
    status     varchar(20) not null,
    reason     varchar(500),
    created_at timestamp not null default now(),
    constraint orders_status_check
        check (status in ('CONFIRMED', 'REJECTED', 'CANCELLED'))
);

-- ---------------------------------------------------------------
-- order_items (new in Lab 2)
-- ---------------------------------------------------------------
create table order_items (
    order_item_id bigserial primary key,
    order_id      bigint not null references orders(order_id) on delete cascade,
    product_id    varchar(50) not null references inventory(product_id),
    quantity      integer not null
);

create index idx_order_items_order_id on order_items(order_id);

-- ---------------------------------------------------------------
-- notifications (new in Lab 2)
-- type: ORDER_CONFIRMED | ORDER_REJECTED | LOW_STOCK
-- ---------------------------------------------------------------
create table notifications (
    notification_id bigserial primary key,
    type            varchar(30) not null,
    message         varchar(500) not null,
    created_at      timestamp not null default now()
);

-- ---------------------------------------------------------------
-- Seed data
-- ---------------------------------------------------------------
insert into inventory (product_id, name, stock) values
    ('P100', 'Wireless Mouse', 25),
    ('P200', 'Mechanical Keyboard', 10),
    ('P300', 'USB-C Hub', 0);
