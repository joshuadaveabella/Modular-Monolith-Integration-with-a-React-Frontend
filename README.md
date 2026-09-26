# Modular Monolith — Order, Inventory & Notification (Lab 2)

A single Spring Boot deployable with three internally-integrated modules,
backed by Supabase (Postgres), with a React (Vite) frontend over REST.

Lab 2 adds: multi-item orders with all-or-nothing rollback, order
cancellation with restock, read endpoints for a live dashboard, in-process
domain events consumed by a new Notification module, and a low-stock
auto-reorder rule.

## Module Structure

```
edu.cit.abella                  - @SpringBootApplication (scans everything below)
edu.cit.abella.events           - shared domain event classes (neutral package)
edu.cit.abella.shop             - Order module
edu.cit.abella.inventory        - Inventory module (package-private impl)
edu.cit.abella.notification     - Notification module (event consumer only)
edu.cit.abella.config           - CORS
```

**Boundary rules enforced in code:**

- `InventoryServiceImpl`, `InventoryEntity` and `InventoryRepository` are package-private. Order can name `InventoryService`, `InventoryItem` and `ReservationResult` — nothing else.
- `OrderService` imports `ApplicationEventPublisher` and the event classes. It has no import from `edu.cit.abella.notification`.
- `NotificationListener` imports only the three event classes. It has no reference to `OrderService` or `InventoryService`.
- The event classes live in their own `events` package so the dependency arrow points from all three modules to a shared, behaviour-free package rather than between modules.

## Supabase Setup

1. Create a free project at [supabase.com](https://supabase.com).
2. Click **Connect** on the project home page → **Direct connection** or **Session pooler** → set **Type** to **JDBC** → copy the connection string. (Session pooler works over IPv4 and doesn't need the paid IPv4 add-on; note it uses port `6543` and a username in the form `postgres.<project-ref>`.)
3. Open the **SQL Editor**, paste the contents of `sql/schema.sql`, and run it. Choose **"Run without RLS"** when prompted — the backend connects as the Postgres user over JDBC, not via Supabase's anon-key REST API, so RLS policies aren't what protects these tables here.
4. Set the credentials as environment variables (never commit them):

   ```bash
   export SUPABASE_DB_URL="jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres"
   export SUPABASE_DB_USERNAME="postgres.<project-ref>"
   export SUPABASE_DB_PASSWORD="your-db-password"
   ```

   PowerShell:
   ```powershell
   $env:SUPABASE_DB_URL="jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres"
   $env:SUPABASE_DB_USERNAME="postgres.<project-ref>"
   $env:SUPABASE_DB_PASSWORD="your-db-password"
   ```

   In IntelliJ, set these under **Run → Edit Configurations → Environment variables** instead.

## Running

```bash
cd backend && mvn spring-boot:run     # http://localhost:8080
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## API

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/orders` | POST | Place a multi-item order |
| `/api/orders` | GET | Order history with line items |
| `/api/orders/{orderId}/cancel` | POST | Cancel an order and restock every line |
| `/api/inventory` | GET | All products with current stock + threshold |
| `/api/notifications` | GET | Activity log (confirmations, rejections, reorder alerts) |

**POST /api/orders**

```json
{ "items": [{ "productId": "P100", "quantity": 2 }, { "productId": "P200", "quantity": 3 }] }
```

```json
{
  "orderId": 7,
  "status": "CONFIRMED",
  "reason": null,
  "items": [
    { "productId": "P100", "quantity": 2, "outcome": "RESERVED" },
    { "productId": "P200", "quantity": 3, "outcome": "RESERVED" }
  ],
  "inventory": [ /* full screenshot below after the order settled */ ]
}
```
![img_9.png](img_9.png)

On rejection, `outcome` is `INSUFFICIENT_STOCK` / `PRODUCT_NOT_FOUND` / `INVALID_QUANTITY` for the offending line and `NOT_ATTEMPTED` for lines that passed validation but were never reserved — which is the point: nothing was reserved.

**Status codes:** `200` for both CONFIRMED and REJECTED (both are valid business outcomes); `400` only for a malformed request body; `404` cancelling an unknown order; `409` cancelling an order that is already CANCELLED or that is REJECTED (a rejected order never reserved stock, so restocking it would create inventory from nothing).

## Design Notes

**Duplicate line items are collapsed before validation.** A cart holding P200 ×6 twice is merged into P200 ×12 before anything is checked. Without this, each line would validate independently (6 ≤ 10, twice) and then reserve 12 units from a stock of 10 — the validate-then-reserve split would be defeated by the cart itself.

**Event listeners are synchronous — deliberately not `@Async`.** Spring's `@EventListener` runs on the publishing thread by default, inside the caller's transaction. That is what I want here: the notification write and the order write commit or roll back together, so the activity feed can never show a confirmation for an order that failed to persist. Making them `@Async` would move the listener to a separate thread with its own transaction, which buys throughput (the HTTP response wouldn't wait on the notification insert) at the cost of that guarantee — the order could commit while the notification silently fails, and I'd need a retry or outbox mechanism to close the gap. At this scale the insert is trivially fast, so the consistency is worth more than the microseconds. It is worth noting that this synchronous coupling is also the thing that stops being free the moment Notification moves out of process.

## Network Tab Evidence

_Insert DevTools → Network screenshots (Payload + Response for each):_

1. **Multi-item order, all items succeed** — e.g. P100 ×2 + P200 ×3 → `CONFIRMED`, both lines `RESERVED`.
![img_4.png](img_4.png)
2. **Multi-item order, one item fails** — e.g. P100 ×2 + P300 ×1 (P300 seeded at 0) → `REJECTED`, and `GET /api/inventory` afterwards shows P100 **unchanged at 25**, proving no partial reservation.
![img_5.png](img_5.png)
3. **Cancel with restock** — cancel a confirmed order, then `GET /api/inventory` showing the quantities returned.
![img_6.png](img_6.png)
4. **Notification feed** — `GET /api/notifications` showing an `ORDER_CONFIRMED`, an `ORDER_REJECTED`, and a `LOW_STOCK` entry. (To trigger low stock: order P200 ×6, leaving 4, below the threshold of 5.)
![img_8.png](img_8.png)

## Reflection

### 1. What keeps multi-item orders atomic in-process, and what would a network split require?

`placeOrder` is annotated `@Transactional`, so every `InventoryService.reserve()` call it makes joins the same Spring-managed transaction and the same JDBC connection. Reserving three products isn't three independent operations that might each half-succeed — it is one unit of work that either commits whole or rolls back whole. If anything throws partway through the reserve loop, every stock decrement already applied in that loop is rolled back by the database itself, along with the order and order_items rows. On top of that I validate the entire cart against current stock *before* calling `reserve()` even once, so the common rejection case (one item short) never touches inventory at all and the rollback path is a fallback rather than the primary mechanism.

Splitting Order and Inventory across a network removes both of those guarantees simultaneously. There is no shared transaction across services, so I'd need to reimplement atomicity at the application level. The realistic approach is a saga with compensating transactions: Order calls Inventory to reserve item 1, then item 2, and if item 3 fails, it must explicitly call a compensating `release()` for items 1 and 2 to undo what already succeeded. That compensation is itself a network call that can fail, so it needs retries with idempotency keys — otherwise a retried release could double-refund stock. I'd also need the reserve call itself to be idempotent, because a timeout tells me nothing about whether the reservation actually happened on the far side. Beyond that: timeouts and circuit breakers so a slow Inventory doesn't hang every order thread, a versioned API contract between the two services, and a reconciliation job to catch the cases where compensation never landed at all. The validate-then-reserve trick still helps — it makes the expensive rollback path rare — but it stops being sufficient, because between validation and reservation another service instance can drain the stock, and across a network that window is much wider.

### 2. How does publishing an event change the coupling with Notification?

Direct invocation would mean `OrderService` holds a reference to something in the notification package — it would have to know that notifications exist, know the method to call, and be recompiled whenever that method's signature changed. Publishing `OrderPlacedEvent` inverts that: `OrderService` announces that something happened and has no idea who, if anyone, is listening. I can add a second listener (an email sender, an audit log) without touching `OrderService` at all, and I can delete the Notification module entirely and the Order module still compiles and runs. The dependency now points from Notification to a shared event class rather than from Order to Notification — Notification depends on Order's *vocabulary*, not on its code.

What the in-process version still shares is a thread and a transaction, and that's exactly what breaks if Notification becomes its own service. I'd need a message broker (RabbitMQ, Kafka) in place of `ApplicationEventPublisher`, and then I have to decide what delivery guarantee I actually need. The dangerous failure is a dual-write: the order commits to Postgres but the broker publish fails, so the event is lost forever and no notification is ever sent. The standard fix is the transactional outbox — write the event to an `outbox` table in the same transaction as the order, then have a separate relay publish it to the broker. That gives at-least-once delivery, which in turn means the consumer must be idempotent, since it will occasionally see the same event twice. I'd also need to handle ordering (does a LowStock event have to arrive after the OrderPlaced that caused it?), a dead-letter queue for events the consumer can never process, and event schema versioning so an older consumer doesn't break when I add a field.

### 3. Which module would I extract first?

Notification, clearly. It's already the loosest-coupled of the three: it consumes events, never calls back into anything, and owns a table nobody else reads. It is also the one whose failure matters least — if a notification is delayed or lost, no stock is wrong and no customer is charged twice, which makes it a safe place to accept the eventual-consistency tradeoff that leaving the monolith forces on you.

The changes are correspondingly small. I'd replace `ApplicationEventPublisher.publishEvent(...)` with a broker publish, ideally behind an interface so the calling code barely changes; add an outbox table and relay so a committed order can't lose its event; move the `notifications` table into the new service's own database rather than sharing Postgres; swap `@EventListener` for a broker listener annotation; and make the handler idempotent by deduplicating on an event ID, since at-least-once delivery will replay events. The event classes themselves would move into a shared contract library or be redefined as a published JSON schema.

Extracting Inventory first would be the worse choice: it's the module Order calls synchronously and transactionally, multiple times per request, and it's the one whose correctness actually matters — pulling it out means immediately taking on the entire saga/compensation problem from question 1 just to keep stock counts honest.

# Modular Monolith — Order, Inventory, Notification & Supplier ACL (Lab 3)

Builds on Lab 2 by adding a fourth module, `edu.cit.abella.supplier`, that
wraps LegacySupply — an external, XML-only, unreliable supplier system —
behind an Anti-Corruption Layer, so none of its quirks leak into Order or
Inventory.

## Module Structure

```
edu.cit.abella                  - @SpringBootApplication (+ @EnableScheduling)
edu.cit.abella.events           - shared domain event classes
edu.cit.abella.shop             - Order module (unchanged from Lab 2)
edu.cit.abella.inventory        - Inventory module + 2 new listeners
edu.cit.abella.notification     - Notification module (+ 1 new listener method)
edu.cit.abella.supplier         - NEW: Anti-Corruption Layer for LegacySupply
```

**What's public in `supplier`, and nothing else:** `SupplierGateway`,
`SupplierOrderResult`, `SupplierOrderStatus`. Everything else — the XML
classes (`Ls*`), `LegacySupplyHttpClient`, `LegacySupplySessionManager`,
`SupplierOrderServiceImpl`, the JPA entities and repositories, the two
scheduled jobs — is package-private. Order and Inventory cannot import a
`SupplierSku`, a `PackSize`, an XML class, or a LegacySupply status code,
because none of those types are visible outside this package.

## New Data Flow (Lab 3)

```
Inventory.reserve() drops stock below threshold
    -> publishes LowStockEvent (unchanged from Lab 2)
    -> NotificationListener logs it (unchanged)
    -> AutoReorderListener (NEW, in inventory package) calls
       SupplierGateway.placeReorder(productId, unitsNeeded)
           -> SupplierOrderServiceImpl converts units to cases (round up),
              persists a PENDING supplier_orders row, attempts an
              idempotent, retried, timed-out call to LegacySupply

OrderStatusPoller (NEW, @Scheduled, in supplier package) polls open orders
    -> on Delivered, publishes SupplierOrderDeliveredEvent
    -> InventoryReplenishmentListener (NEW, in inventory package) calls
       InventoryService.restock() - Inventory never calls the supplier
       module directly for this
    -> NotificationListener logs "Restocked ..."
```

## Setup

### 1. Supabase (carried over from Lab 2)

Run `sql/schema.sql` in the Supabase SQL Editor — it now also creates
`supplier_sku_mapping` and `supplier_orders`, with **placeholder** SKU
mapping rows you must replace (see step 3 below and `INTEGRATION.md`).

### 2. LegacySupply credentials

```bash
export LS_CLIENT_ID="23-4152-359"
export LS_API_KEY="LSK-AC636F11075367E213D9"
```
PowerShell:
```powershell
$env:LS_CLIENT_ID="23-4152-359"
$env:LS_API_KEY="LSK-AC636F11075367E213D9"
```
Never commit these. `.gitignore` already excludes `.env` files, but double
check you haven't pasted the key into `application.properties` directly.

### 3. Discover your real catalog (Part B) — do this before running the app for real

Import `postman/LegacySupply_Discovery.postman_collection.json`, set
`clientId`/`apiKey` in the collection variables, and run requests 1–3 to
get your real `SupplierSku`/`PackSize` values. Update:
- `sql/schema.sql`'s `supplier_sku_mapping` insert (or run an `UPDATE`
  directly in Supabase if you've already executed the script once)
- `INTEGRATION.md`'s mapping table

Without this step, every reorder will fail with `E-SKU-02` (unknown item),
be marked `FAILED`, and nothing further will happen for that product.

### 4. Run

```bash
cd backend && mvn spring-boot:run     # http://localhost:8080
cd frontend && npm install && npm run dev   # http://localhost:5173
```

## Resilience Design (Part D)

- **Timeout:** `RestTemplateConfig` sets both connect and read timeout to
  `legacysupply.timeout-ms` (default 3000ms).
- **Retry with backoff, max 3 attempts:** `RetryingCaller`, used by both
  the immediate synchronous send (`SupplierOrderServiceImpl.attemptSubmission`)
  and the scheduled resend (`ReorderQueueProcessor`). Only retries
  transient failures (`NETWORK_OR_TIMEOUT`, `RATE_LIMIT`, `SERVER_ERROR`) —
  validation errors like a bad SKU are marked `FAILED` immediately, since
  retrying an inherently wrong request would just waste quota.
- **No duplicate purchase orders:** `SupplierOrderEntity.requestId` is
  derived from the row's own database id (`"req-" + id`) and persisted
  before the first network attempt — so it is identical across every
  retry AND survives an app restart. On top of that,
  `LegacySupplyHttpClient.findExistingByBuyerRef()` checks whether an
  order already exists under this `BuyerRef` before every submission
  attempt (including scheduled retries), as a second safeguard against a
  lost response causing a resend.
- **No lost reorders:** the `supplier_orders` row is persisted as
  `PENDING` *before* any network call is made. If that call fails for any
  reason — including the JVM crashing — the row is already durable, and
  `ReorderQueueProcessor` (`@Scheduled`) resends anything still `PENDING`
  on its next run.

## Event Listeners: Synchronous, Same As Lab 2

`@EventListener` methods here still run synchronously, on the same thread
and transaction as whatever published the event — same choice as Lab 2, for
the same consistency reasons. This has one important consequence I had to
guard against explicitly: `AutoReorderListener.onLowStock()` fires
*inside* the same transaction as the stock reservation that triggered
`LowStockEvent`. If placing a reorder threw an exception up through that
listener, it would roll back the customer's order — a LegacySupply outage
should never do that. `SupplierOrderServiceImpl.placeReorder()` therefore
persists its `PENDING` row and then wraps the actual network attempt in a
try/catch that swallows any exception, logging it instead. The reorder
simply stays `PENDING` for the scheduled job to pick up; it never
propagates back into the order transaction.

## Checkpoints

- Part C: at least 3 purchase orders visible on the self-check page.
- Part D: zero duplicates, no lost reorders, visible on the self-check page.

These are verified server-side from your actual traffic — running the app
against a few real low-stock events (or calling `SupplierGateway` a few
times via a temporary test endpoint, if you need to force it faster) is
how you actually clear them.