# Modular Monolith — Order, Inventory, Notification, Supplier ACL & Tiangge Channel (Lab 4)

Adds a fifth module, `edu.cit.abella.channel`, that runs the shop live and
unattended against Tiangge (an external marketplace) while reusing every
module built in Labs 1–3 unchanged in its core logic.

## ⚠️ Before you do anything else

**Verify `tiangge.base-url` and the exact field/endpoint names in Stage 0.**
This code was written from the Tiangge manual without the ability to test
against your live account. I'm fairly confident in the overall shape, but
not every exact string. Every endpoint path and JSON field name lives in
exactly one file — **`TiangeHttpClient.java`** — and the DTO classes next
to it (`FeedEvent`, `DecisionRequest`, etc.) — so if Stage 0 probing
(`postman/Tiangge_Discovery.postman_collection.json`) shows a different
exact name, that's the only place you need to change it.

**Remember the One Rule:** once your app has sent a heartbeat and published
a listing, it is "live," and every further Postman/curl call against
Tiangge counts against your grade. Do all your exploration *before* Task 1
is complete.

## Module Structure

```
edu.cit.abella                  - @SpringBootApplication (+ @EnableScheduling)
edu.cit.abella.config           - CorsConfig, AppInstance (shared instance identity)
edu.cit.abella.events           - shared domain events (now 6, see below)
edu.cit.abella.shop             - Order module (+ BACKORDERED status, backorder resolution)
edu.cit.abella.inventory        - Inventory module (+ InventoryStockChangedEvent)
edu.cit.abella.notification     - Notification module (+ 1 new listener method)
edu.cit.abella.supplier         - Supplier ACL (+ hasOpenOrder, getSupplierSkuFor)
edu.cit.abella.channel          - NEW: Tiangge integration
```

**Why `AppInstance` lives in `config`, not `channel`:** the manual requires
the *same* `X-Client-Instance` header on LegacySupply calls too. Putting it
in `channel` would force `supplier` to depend on `channel`, which nothing
in the assignment calls for. A small shared class avoids that entirely.

**What's public in `channel`:** nothing needed to be. Every other module in
this app only ever *calls into* `channel` indirectly via events it publishes
(`InventoryStockChangedEvent`, `BackorderResolvedEvent`) — nothing calls a
channel method directly. So there's no interface for another module to
depend on, and everything in the package is package-private. This is worth
being able to explain if asked: the "public interface" rule exists to
bound what *could* leak outward; here, nothing needs to flow outward at
all, which is itself evidence the boundary is clean.

## How Each Task Is Satisfied

| Task | Where |
|---|---|
| 1. Instance ID + heartbeat | `AppInstance`, `TiangeStartup` (first heartbeat), `HeartbeatScheduler` (every 30s after) |
| 2. Publish listings | `TiangeStartup.publishListings()`, using `SupplierGateway.getSupplierSkuFor()` |
| 3. Stock sync, event-driven | `InventoryStockChangedEvent` (published from `InventoryServiceImpl.reserve()`/`restock()`) → `StockPublishListener` |
| 4. Decide orders within 60s, redelivery-safe | `FeedPoller` (polls every 5s by default), `OrderService.placeOrder(items, true)` |
| 5. Cancellations | `FeedPoller.handleOrderCancelled()` → `OrderService.cancelOrder()` (Lab 2 logic, unchanged) |
| 6. Backorder + resolution via LegacySupply delivery | `OrderService` (decision + `resolveBackordersForProduct()`), `BackorderResolvedEvent` → `BackorderNotifier` |
| Restart test | `ChannelFeedCursorEntity` — a DB row, not an in-memory field |

## Idempotency Design (Task 4's hardest requirement)

Two independent, overlapping layers:

1. **`channel_processed_events`** — one row per feed `eventId` that was
   **fully** handled (our side effect done *and* Tiangge successfully
   notified). A re-delivered event with the same `eventId` is recognized
   and skipped.
2. **`channel_order_mapping`** — maps Tiangge's `orderId` to our own order.
   Before creating a new order for an `ORDER_PLACED` event, `FeedPoller`
   checks this table first. If a mapping already exists (meaning a
   previous attempt created the order but failed to tell Tiangge about it),
   it re-sends the *existing* order's decision instead of creating a
   second order.

Layer 2 is what actually guarantees "exactly one order per Tiangge order,"
not layer 1 — if layer 1 were ever bypassed by a timing edge case, layer 2
still holds.

**The feed cursor only advances past an event once it is fully handled.**
If creating the order succeeds but notifying Tiangge of the decision fails,
the cursor does not move, so the next poll re-fetches that same event —
and layer 2 above prevents it from becoming a duplicate order on that retry.

## A Bug I Caught and Fixed During Design

`InventoryReplenishmentListener` (restocks on delivery) and
`BackorderResolutionListener` (checks current stock to resolve backorders)
both react to the **same** `SupplierOrderDeliveredEvent`. Spring doesn't
guarantee listener execution order by default. If the backorder check ran
*before* the restock, it would see stale, pre-delivery stock and wrongly
cancel orders that should have been confirmed. Both listeners now carry
explicit `@Order` annotations (`@Order(1)` on the restock, `@Order(2)` on
the resolution check) to pin this.

## Known Limitation

`BackorderNotifier` (tells Tiangge a backorder resolved) retries with
backoff on failure but, unlike reorders in Lab 3, has no durable
PENDING-style queue behind it — if all retries fail, the notification is
only logged, not retried on a later schedule. Given the lab's time limit I
judged "at least one backorder resolved" (the stated proof) more important
to get working end-to-end than building a second durable queue identical
to Lab 3's. A fuller version would add a `resolution_confirmed` flag (same
pattern as `decision_confirmed`) plus a small scheduled sweep.

## Setup

1. Run `sql/schema.sql` in Supabase (adds `channel_feed_cursor`,
   `channel_processed_events`, `channel_order_mapping`; `orders.status`
   now allows `BACKORDERED`).
2. Same `LS_API_KEY`/`LS_CLIENT_ID` environment variables as Lab 3 — Tiangge
   reuses them (`tiangge.client-id`/`tiangge.api-key` point at the same
   variables in `application.properties`).
3. **Stage 0:** import `postman/Tiangge_Discovery.postman_collection.json`,
   verify the base URL and payload shapes, fix `TiangeHttpClient.java` if
   anything differs — **before** running the app for real.
4. Run the backend. Watch the startup log for the instance ID line and
   confirm "First heartbeat sent."
5. Check `https://legacysupply.onrender.com/verify` for live status.

## REFLECTION.md

See `REFLECTION_Lab4_Addendum.md` — append its content to your existing
`REFLECTION.md` from Lab 3, under a new "Marketplace" heading.
