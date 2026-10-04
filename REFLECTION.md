# REFLECTION.md

The self-check page (`https://legacysupply.onrender.com/verify`) generates
its three reflection questions from **your own** traffic against
LegacySupply, so they will not match a classmate's. Open that page, sign in
with your Client ID, and copy the exact three questions it shows you below,
each followed by your own 3–6 sentence answer referring to your actual
logs and code.

---

## Question 1

**LegacySupply holds more than one order for BuyerRef "B-TEST-1": PO-100075 (20:07:48) and PO-100077 (20:10:10). Reconstruct the sequence of events that produced the duplicate, and describe the change you made (or would make) so it cannot happen again.**

*At 20:07:48 I submitted a purchase order for BuyerRef B-TEST-1 (SupplierSku STF-8943) from Postman during Part B testing, before my adapter existed. LegacySupply created the order server-side as PO-100075, but the client-facing response came back as an error, so from my side the request looked like it had failed. Because I was testing manually at that stage, I had no stored, reusable identifier for that logical reorder, and I hadn't yet checked for an existing order before resending. About two and a half minutes later, at 20:10:10, I resent what I believed was the same order, and since it carried no matching X-Request-Id, LegacySupply treated it as new and created PO-100077. The fix in my adapter is that each reorder generates exactly one request_id (a UUID) at row creation, persists it in supplier_orders, and reuses that same value on every retry, scheduled resend, and even after an app restart, so LegacySupply's deduplication can recognize repeats. I also added a GET /purchase-orders?buyerRef= lookup before resending any PENDING row, so even if the ID reuse ever failed, the adapter checks for an existing order under that BuyerRef first.*

## Question 2

**At 20:07:48 your request for BuyerRef "B-TEST-1" received a 503, but LegacySupply had already created PO-100075. Walk through exactly what your adapter did next, and explain why that did or did not result in a second order.**

*At the time of the 20:07:48 failure, there was no adapter yet, only a manual Postman request, so there was no automatic "next step" at all: I simply saw an error and re-sent the order by hand, which is what produced the duplicate. My adapter now behaves differently, and I have log evidence of the correct pattern from later in this same run: at 21:04:43 a POST /purchase-orders failed with 401 E-AUTH-07 (expired session), the client signed in again at 21:04:50, and the retry at 21:05:04 came back 200 idempotent replay, followed by another retry at 21:05:43 also returning 200 idempotent replay rather than a new PO. That happens because every retry reuses the row's stored request_id, so LegacySupply recognizes it as the same request and returns the original result instead of creating a second order. In other words, the 20:07:48 duplicate could not happen with the current code, because the adapter never regenerates an ID between attempts, and I have the 21:05 log lines as direct evidence that repeated sends under one ID are being deduplicated correctly.*

## Question 3

**PO-100138 (BuyerRef "B-TEST-2") ended with StatusCode 90, which is not in the documentation. How did you work out what it means, and what does your system now do with the stock that will never arrive?**

*(PO-100138 (BuyerRef B-TEST-2) came back with StatusCode 90, which isn't in the manual's table of 10/20/30/40, so I couldn't look up what it means, and support doesn't answer questions the manual already claims to cover. Rather than guess at its meaning, my XmlTranslator.mapStatus maps any code outside the documented set to my own UNKNOWN enum value by default, and I log the raw code (90) alongside the PO number and BuyerRef so there's a record of it even though my system doesn't interpret it. Because my restock logic only fires on the transition into DELIVERED, an order stuck at UNKNOWN is never restocked, so no phantom stock is added for units that may never arrive. The gap I noticed while reviewing this is that UNKNOWN is treated as still "open" by my reorder guard, so the system won't place a fresh automatic reorder for that product either, meaning if code 90 really means the order was rejected or cancelled, the product could stay under-stocked indefinitely with no automatic order and no delivered stock. My documented policy is that persistently UNKNOWN orders need a human to review them and manually mark the row FAILED so the reorder guard clears and a real replacement order can be placed; I don't currently have an automatic time-based escalation for this, which I'd call out as a known limitation.*

## Marketplace (Lab 4)

Open `https://legacysupply.onrender.com/verify`, find the "Marketplace" section,
and copy its exact three questions here, each with your own 3–6 sentence
answer referring to your actual logs, `supplier_orders`/`orders`/
`channel_order_mapping` rows, and specific classes.

### Question 1

**Tiangge order TG-PFVVP5 (7 x P100) was accepted at 16:22:18. At that moment your last published stock for P100 was 1, and the stock Tiangge worked out from your own decisions, cancellations and deliveries was 1. Where did your application's stock figure come from, and why did it disagree?**

*My application's ACCEPT decision never consults the number it last told Tiangge — it's based entirely on the live inventory table, checked fresh at decision time inside InventoryService.reserve(), which runs inside the same @Transactional call as OrderService.placeOrder(). The "1" that both I and Tiangge were showing was the last successfully published figure via StockPublishListener, but that publish path is asynchronous and retried with backoff (ChannelRetrying) whenever Tiangge is slow or unreachable — my logs show repeated NETWORK_OR_TIMEOUT failures around this window. So the disagreement wasn't a bug in the decision itself: the real inventory count had already risen above 7 by the time the order was decided (most likely from a supplier delivery restock via InventoryReplenishmentListener), but the communication of that new number to Tiangge hadn't landed yet, leaving Tiangge's own derived figure stuck on the stale "1" from before the restock. This is exactly the gap I patched afterward with the sequence-tracking fix in StockPublishListener, which prevents a late, stale retry from overwriting a newer value once it exists — but it doesn't eliminate the underlying lag between "truth" and "last successfully told," which is why the two numbers can briefly disagree even when both sides are behaving correctly.*

### Question 2

**Event evt_3a17a001c24b50da (order TG-VM6G3Z) reached your application twice, as seq 5 and seq 12, and you processed it once. Show the code and the stored data that made the second delivery harmless, and explain what would happen if your application restarted between the two.**

*FeedPoller.handleEventSafely() checks ChannelProcessedEventRepository.existsByEventId(event.eventId) before doing anything else; since seq 5 was fully handled (order created, decision sent, and ChannelProcessedEventEntity saved), the seq 12 redelivery of the same eventId hit that check, returned true immediately, and was skipped with no second order created. There's a second, independent safeguard behind that: handleOrderPlaced() also checks ChannelOrderMappingRepository.findById(event.orderId) by Tiangge's own order ID before creating anything, so even if the eventId check were somehow bypassed, the Tiangge-order-to-shop-order mapping table would still stop a duplicate order from being created. If my application had restarted between seq 5 and seq 12, nothing changes: both the processed-event row and the order mapping row are persisted via plain repository.save() calls, which commit immediately rather than waiting in memory, so a fresh JVM instance would see the exact same "already handled" state on disk and skip the redelivery identically — restart-safety here isn't a special case, it's the same check running against durable data either way.*

### Question 3

**During your restart test your application was down for about 61 seconds while 1 orders arrived. How did the restarted application find those orders, and how did it avoid handling earlier ones again?**

*ChannelFeedCursorEntity is a single database row (channel_feed_cursor, id=1) holding last_seq, read and written by FeedPoller.currentCursor()/advanceCursor() on every poll — it is never held only in memory. On restart, the very first scheduled poll() call reads that row, gets the same last_seq the previous instance left behind, and calls GET /feed?after={that value}, which naturally returns the one order that arrived while the app was down plus nothing from before it. Avoiding re-handling earlier orders isn't really a separate mechanism from this — it's the same cursor value doing double duty: anything at or before last_seq is never even requested from Tiangge again, so there's no redelivery of old events to defend against in the first place, and the ChannelProcessedEventEntity/ChannelOrderMappingEntity checks from Question 2 exist specifically to catch the narrower case where Tiangge redelivers something at or after the cursor despite us having already seen it.*

---
