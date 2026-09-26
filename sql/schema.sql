-- Lab 3 schema. Recreates everything from scratch, including seed data.
-- Run in the Supabase SQL Editor. Safe to re-run - it drops existing
-- tables first, so it wipes orders, order items, notifications and
-- supplier order history each time.

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
-- orders / order_items (Lab 2 - multi-item, CANCELLED-capable)
-- ---------------------------------------------------------------
create table orders (
    order_id   bigserial primary key,
    status     varchar(20) not null,
    reason     varchar(500),
    created_at timestamp not null default now(),
    constraint orders_status_check
        check (status in ('CONFIRMED', 'REJECTED', 'CANCELLED'))
);

create table order_items (
    order_item_id bigserial primary key,
    order_id      bigint not null references orders(order_id) on delete cascade,
    product_id    varchar(50) not null references inventory(product_id),
    quantity      integer not null
);

create index idx_order_items_order_id on order_items(order_id);

-- ---------------------------------------------------------------
-- notifications (Lab 2, plus SUPPLIER_ORDER_DELIVERED in Lab 3)
-- type: ORDER_CONFIRMED | ORDER_REJECTED | LOW_STOCK | SUPPLIER_ORDER_DELIVERED
-- ---------------------------------------------------------------
create table notifications (
    notification_id bigserial primary key,
    type            varchar(30) not null,
    message         varchar(500) not null,
    created_at      timestamp not null default now()
);

-- ---------------------------------------------------------------
-- supplier_sku_mapping (Lab 3, new)
-- Maps YOUR inventory products to LegacySupply's SupplierSku + PackSize.
-- THE VALUES BELOW ARE PLACEHOLDERS. Replace them with real values from
-- your own GET /catalog probe (Part B) before running the app for real -
-- see INTEGRATION.md. Wrong SupplierSku values will fail with E-SKU-02.
-- ---------------------------------------------------------------
create table supplier_sku_mapping (
    product_id   varchar(50) primary key references inventory(product_id),
    supplier_sku varchar(50) not null,
    pack_size    integer not null
);

-- ---------------------------------------------------------------
-- supplier_orders (Lab 3, new)
-- status uses OUR enum (PENDING/SUBMITTED/IN_PROGRESS/SHIPPED/DELIVERED/
-- FAILED/UNKNOWN), never LegacySupply's numeric StatusCode.
-- ---------------------------------------------------------------
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
-- Seed data
-- ---------------------------------------------------------------
insert into inventory (product_id, name, stock) values
    ('P100', 'Wireless Mouse', 25),
    ('P200', 'Mechanical Keyboard', 10),
    ('P300', 'USB-C Hub', 0);

-- PLACEHOLDER mapping - replace SupplierSku/PackSize with what your own
-- GET /catalog call actually returns (see Part B / INTEGRATION.md).
insert into supplier_sku_mapping (product_id, supplier_sku, pack_size) values
    ('P100', 'REPLACE-ME-P100', 1),
    ('P200', 'REPLACE-ME-P200', 1),
    ('P300', 'REPLACE-ME-P300', 1);
