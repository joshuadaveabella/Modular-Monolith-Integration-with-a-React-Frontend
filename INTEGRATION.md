# INTEGRATION.md — LegacySupply Contract Discovery

## 1. Product mapping

**TODO — replace with your real GET /catalog response.** Use Postman
request "3. Get catalog" in `postman/LegacySupply_Discovery.postman_collection.json`,
then fill in the table below and update `sql/schema.sql`'s
`supplier_sku_mapping` insert with the same values (currently placeholders).

| Our Product ID | Our Name | LegacySupply SupplierSku | PackSize |
|---|---|---|---|
| P100 | Wireless Mouse | STF-8943 | 20 |
| P200 | Mechanical Keyboard | STF-4611 | 10 |
| P300 | USB-C Hub | STF-7961 | 20 |

If your catalog doesn't include equivalents for all three seeded products,
add rows to `inventory` for whatever products your catalog *does* cover,
and map those instead — the assignment allows this ("Add products to your
Inventory if you need to").

## 2. Sessions

**TODO — measure this yourself; the manual only says "short-lived."**

How to measure: authenticate once (Postman request 2), note the timestamp,
then call an authenticated endpoint (Postman request 11, "Measure session
length") repeatedly at increasing intervals using the *same* token, without
re-authenticating. Record the gap between login and the first `E-AUTH-03` /
`E-AUTH-07` rejection.

- Measured session length: **___ minutes/seconds** (fill in)
- What happened when the session was reused after expiry: *(paste the exact `LSError` body you received)*
- Design consequence: because this duration isn't documented and could
  change, `LegacySupplySessionManager` in the code does not rely on a
  fixed TTL at all — it caches a token until a real request is rejected
  with `E-AUTH-02/03/07`, then transparently re-authenticates and retries
  once. This works correctly regardless of what the actual measured value
  turns out to be.

## 3. Error codes actually received

**TODO — fill in with what you actually got back**, using Postman requests
7–10 (and anything else you tried) as a starting point. The table below
lists every code the manual documents; replace the "Cause" column with
what you personally observed, or note if you saw something the manual
doesn't mention.

| Code | HTTP | What triggered it (fill in from your own testing)                  |
|---|---|--------------------------------------------------------------------|
| E-AUTH-01 | 401 | *(e.g. sent a wrong ApiKey)*                                       |
| E-AUTH-02 | 401 | *<Code>E-AUTH-02</Code> <Message>Session header missing.</Message>* |
| E-AUTH-03 | 401 | *<Code>E-AUTH-07</Code> <Message>Session not valid.</Message>*     |
| E-AUTH-07 | 401 | *<Code>E-AUTH-07</Code> <Message>Session not valid.*               |
| E-FMT-01 | 415 | *(e.g. sent Content-Type: application/json)*                       |
| E-FMT-02 | 400 | *(e.g. malformed/unclosed XML tag)*                                |
| E-REF-05 | 400 | *(e.g. empty or over-length BuyerRef)*                             |
| E-SKU-02 | 422 | *<Code>E-SKU-02</Code> <Message>Item not recognized.</Message>*    |
| E-QTY-11 | 422 | *<Code>E-QTY-11</Code> <Message>Quantity invalid.</Message>*       |
| E-IDEM-04 | 409 | *<Code>E-IDEM-04</Code> <Message>Request id reused with different content.</Message>*                                          |
| E-PO-04 | 404 | *<Code>E-PO-04</Code> <Message>Order not found.</Message>*         |
| E-QRY-06 | 400 | *(e.g. GET /purchase-orders with no buyerRef param)*               |
| E-RATE-03 | 429 | *(only seen under load — note if you triggered this during Part D)* |
| E-SYS-50 | 503 | *<Code>E-SYS-50</Code> <Message>Processing error.</Message>*       |
| E-SYS-99 | 503 | *(only seen if the instructor induced this during Part D)*         |

## 4. Qty and Uom, in your own words

**Qty** is the order quantity in whatever unit LegacySupply itself uses to
sell that specific item — not necessarily the same unit our own Inventory
tracks stock in. **Uom** ("unit of measure") is what LegacySupply's
acknowledgement tells us that quantity was actually expressed in — in the
example shown in the manual, `Uom` comes back as `CS` (cases), meaning the
`Qty` sent was a number of *cases*, not individual pieces.

**Worked example:** say our Inventory needs 22 more units of a mouse to
restock, and this SKU's catalog `PackSize` is 12 (12 mice per case).
LegacySupply doesn't sell single mice through this interface — it sells by
the case — so we can't send `Qty=22`. We round up: `ceil(22 / 12) = 2`
cases. We send `Qty=2`. The response comes back with `Uom=CS`, confirming
those 2 cases translate to 2 × 12 = 24 physical units once delivered — 2
more than we strictly needed, because you can't order a partial case. This
is exactly what `SupplierOrderServiceImpl.placeReorder()` computes:
`cases = ceil(unitsNeeded / packSize)`, and `units = cases * packSize` is
what actually gets restocked into Inventory once the order is Delivered.

## 5. Unrecognized order status handling (Part E)

If `GET /purchase-orders/{PoNumber}` ever returns a `StatusCode` other than
10/20/30/40, `SupplierOrderServiceImpl.mapStatusCode()` maps it to our own
`UNKNOWN` enum value rather than guessing or crashing. `OrderStatusPoller`
keeps polling `UNKNOWN` orders on every subsequent run (it's included in
the "open" status list) rather than treating the order as finished or
failed, since an unrecognized code could simply mean LegacySupply added a
status after this manual revision (2.3.1) was written. This is logged at
WARN level each time it happens so it's visible without silently stalling.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<LSError>
    <Code>E-SKU-02</Code>
    <Message>Item not recognized.</Message>
</LSError>
```

## 6. Known ambiguity in the manual

`GET /purchase-orders?buyerRef=...` returns a `PurchaseOrderList` with a
`Count` and "every order you have placed under that reference," but the
manual doesn't show the exact XML tag name wrapping each repeated order.
`LegacySupplyHttpClient.findExistingByBuyerRef()` parses this defensively
with XPath (`//PoNumber[1]`, `//StatusCode[1]`) rather than a strict JAXB
class, so it works regardless of the wrapper tag's actual name.

**TODO:** run Postman request "6. Track orders by BuyerRef", paste the raw
response body below, and note whether the defensive XPath approach needs
any adjustment for your account's actual response shape.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<PurchaseOrderList>
  <Count>1</Count>
  <PurchaseOrder>
    <PoNumber>PO-100318</PoNumber>
    <StatusCode>20</StatusCode>
    <SupplierSku>STF-8943</SupplierSku>
    <Qty>1</Qty>
    <Uom>CS</Uom>
    <BuyerRef>RO-DISCOVERY-1</BuyerRef>
    <CreatedAt>2026-09-26T12:34:11.724Z</CreatedAt>
  </PurchaseOrder>
</PurchaseOrderList>
```
