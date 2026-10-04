-- Lab 4 schema. Recreates everything from scratch, including seed data.
-- Run in the Supabase SQL Editor. Safe to re-run - it drops existing
-- tables first.

drop table if exists channel_processed_events cascade;
drop table if exists channel_order_mapping cascade;
drop table if exists channel_feed_cursor cascade;
drop table if exists notifications cascade;
drop table if exists supplier_orders cascade;
drop table if exists supplier_sku_mapping cascade;
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
-- orders / order_items (Lab 2/3, status now includes BACKORDERED)
-- ---------------------------------------------------------------
create table orders (
    order_id   bigserial primary key,
    status     varchar(20) not null,
    reason     varchar(500),
    created_at timestamp not null default now(),
    constraint orders_status_check
        check (status in ('CONFIRMED', 'REJECTED', 'CANCELLED', 'BACKORDERED'))
);

create table order_items (
    order_item_id bigserial primary key,
    order_id      bigint not null references orders(order_id) on delete cascade,
    product_id    varchar(50) not null references inventory(product_id),
    quantity      integer not null
);

create index idx_order_items_order_id on order_items(order_id);

-- ---------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------
create table notifications (
    notification_id bigserial primary key,
    type            varchar(30) not null,
    message         varchar(500) not null,
    created_at      timestamp not null default now()
);

-- ---------------------------------------------------------------
-- supplier_sku_mapping / supplier_orders (Lab 3)
-- ---------------------------------------------------------------
create table supplier_sku_mapping (
    product_id   varchar(50) primary key references inventory(product_id),
    supplier_sku varchar(50) not null,
    pack_size    integer not null
);

create table supplier_orders (
    id          bigserial primary key,
    product_id  varchar(50) not null references inventory(product_id),
    buyer_ref   varchar(40) not null unique,
    request_id  varchar(80) not null unique,
    po_number   varchar(50),
    cases       integer not null,
    units       integer not null,
    status      varchar(20) not null,
    last_error  varchar(300),
    created_at  timestamp not null default now(),
    updated_at  timestamp not null default now(),
    constraint supplier_orders_status_check
        check (status in ('PENDING', 'SUBMITTED', 'IN_PROGRESS', 'SHIPPED',
                           'DELIVERED', 'FAILED', 'UNKNOWN'))
);

create index idx_supplier_orders_status on supplier_orders(status);

-- ---------------------------------------------------------------
-- channel_feed_cursor (Lab 4, new) - single row, restart-safe feed position
-- ---------------------------------------------------------------
create table channel_feed_cursor (
    id       bigint primary key,
    last_seq bigint
);

insert into channel_feed_cursor (id, last_seq) values (1, null);

-- ---------------------------------------------------------------
-- channel_processed_events (Lab 4, new) - per-eventId idempotency
-- ---------------------------------------------------------------
create table channel_processed_events (
    event_id     varchar(80) primary key,
    seq          bigint not null,
    type         varchar(30) not null,
    processed_at timestamp not null default now()
);

-- ---------------------------------------------------------------
-- channel_order_mapping (Lab 4, new) - Tiangge orderId <-> our order
-- ---------------------------------------------------------------
create table channel_order_mapping (
    tiangge_order_id      varchar(80) primary key,
    shop_order_id         bigint not null references orders(order_id),
    decision_confirmed    boolean not null default false,
    cancellation_confirmed boolean not null default false,
    created_at            timestamp not null default now(),
    updated_at            timestamp not null default now()
);

create index idx_channel_order_mapping_shop_order_id on channel_order_mapping(shop_order_id);

-- ---------------------------------------------------------------
-- Seed data
-- ---------------------------------------------------------------
insert into inventory (product_id, name, stock) values
    ('P100', 'Wireless Mouse', 25),
    ('P200', 'Mechanical Keyboard', 10),
    ('P300', 'USB-C Hub', 0);

-- Replace with your real values from Lab 3's GET /catalog probe if you
-- haven't already (see INTEGRATION.md from Lab 3/4).
insert into supplier_sku_mapping (product_id, supplier_sku, pack_size) values
    ('P100', 'STF-8943', 20),
    ('P200', 'STF-4611', 10),
    ('P300', 'STF-7961', 20);
