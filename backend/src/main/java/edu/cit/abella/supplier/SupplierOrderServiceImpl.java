package edu.cit.abella.supplier;

import edu.cit.abella.supplier.LegacySupplyHttpClient.PoOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Package-private (no "public" modifier) - the same rule as Lab 1 and 2's
// InventoryServiceImpl. AutoReorderListener can only see this through the
// SupplierGateway interface.
@Service
class SupplierOrderServiceImpl implements SupplierGateway {

    private static final Logger log = LoggerFactory.getLogger(SupplierOrderServiceImpl.class);

    private final SupplierOrderRepository orderRepository;
    private final SupplierSkuMappingRepository skuMappingRepository;
    private final LegacySupplyHttpClient httpClient;
    private final LegacySupplyProperties properties;

    SupplierOrderServiceImpl(SupplierOrderRepository orderRepository,
                             SupplierSkuMappingRepository skuMappingRepository,
                             LegacySupplyHttpClient httpClient,
                             LegacySupplyProperties properties) {
        this.orderRepository = orderRepository;
        this.skuMappingRepository = skuMappingRepository;
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override
    @Transactional
    public SupplierOrderResult placeReorder(String productId, int unitsNeeded) {
        SupplierSkuMappingEntity mapping = skuMappingRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalStateException(
                        "No supplier SKU mapping for product " + productId
                                + " - add a row to supplier_sku_mapping (see INTEGRATION.md)"));

        // Round UP to whole cases - LegacySupply only sells by the case
        // (PackSize), and we must never order less than what's needed.
        int cases = (int) Math.ceil((double) unitsNeeded / mapping.getPackSize());
        int unitsOnOrder = cases * mapping.getPackSize();

        // Persist the PENDING row FIRST, before any network call. This is
        // what makes "never lose a reorder" true even if the JVM crashes
        // between here and the HTTP attempt below - the row already exists
        // and the scheduled queue processor will pick it up.
        SupplierOrderEntity order = new SupplierOrderEntity(productId, cases, unitsOnOrder);
        orderRepository.save(order); // assigns the id

        order.setBuyerRef("RO-" + order.getId());
        order.setRequestId("req-" + order.getId());
        orderRepository.save(order);

        // Try to send immediately. Deliberately swallow ANY exception here:
        // this method is called synchronously from AutoReorderListener,
        // which reacts to LowStockEvent inside the SAME transaction as the
        // stock reservation that triggered it. If a LegacySupply outage
        // threw all the way up, it would roll back the customer's order -
        // a supplier hiccup must never do that. On any failure the row
        // simply stays PENDING for ReorderQueueProcessor to retry later.
        try {
            attemptSubmission(order, mapping.getSupplierSku());
        } catch (Exception e) {
            log.warn("Immediate submission of reorder {} failed, left PENDING for retry: {}",
                    order.getBuyerRef(), e.getMessage());
        }

        return toResult(order);
    }

    /**
     * Shared by the immediate synchronous attempt above and
     * ReorderQueueProcessor's scheduled retries. Checks for an
     * already-placed order under this BuyerRef first (belt-and-braces
     * against a lost response), then submits with retry-with-backoff.
     */
    void attemptSubmission(SupplierOrderEntity order, String supplierSku) {
        Optional<PoOutcome> existing = safeCheckExisting(order.getBuyerRef());
        if (existing.isPresent()) {
            applyOutcome(order, existing.get());
            orderRepository.save(order);
            return;
        }

        try {
            PoOutcome outcome = RetryingCaller.call(properties.maxAttempts,
                    () -> httpClient.submitPurchaseOrder(
                            supplierSku, order.getCases(), order.getBuyerRef(), order.getRequestId()));
            applyOutcome(order, outcome);
        } catch (LsCallException e) {
            if (e.isRetryable()) {
                // Exhausted retries on a transient failure - stays PENDING,
                // the scheduled job will try again on its next run.
                log.warn("Reorder {} still PENDING after retries: {} {}",
                        order.getBuyerRef(), e.kind, e.getMessage());
            } else {
                // A non-transient failure (bad SKU, invalid qty, malformed
                // request) will fail identically every time - no point
                // queuing it forever.
                order.setStatus(SupplierOrderStatus.FAILED);
                order.setLastError(e.code + ": " + e.getMessage());
                log.error("Reorder {} marked FAILED ({}): {}",
                        order.getBuyerRef(), e.kind, e.getMessage());
            }
        }

        orderRepository.save(order);
    }

    private Optional<PoOutcome> safeCheckExisting(String buyerRef) {
        try {
            return httpClient.findExistingByBuyerRef(buyerRef);
        } catch (LsCallException e) {
            // Can't verify right now - proceed to a normal submission
            // attempt rather than blocking the reorder entirely.
            // X-Request-Id de-duplication is still the primary safeguard.
            log.warn("Could not check for an existing order under {}: {}", buyerRef, e.getMessage());
            return Optional.empty();
        }
    }

    private void applyOutcome(SupplierOrderEntity order, PoOutcome outcome) {
        order.setPoNumber(outcome.poNumber());
        order.setStatus(mapStatusCode(outcome.statusCode()));
    }

    // The one place LegacySupply's numeric codes (10/20/30/40) are
    // translated into our own enum. See INTEGRATION.md / Part E for how
    // an unrecognized code is handled.
    static SupplierOrderStatus mapStatusCode(int code) {
        return switch (code) {
            case 10 -> SupplierOrderStatus.SUBMITTED;
            case 20 -> SupplierOrderStatus.IN_PROGRESS;
            case 30 -> SupplierOrderStatus.SHIPPED;
            case 40 -> SupplierOrderStatus.DELIVERED;
            default -> {
                log.warn("Unrecognized LegacySupply StatusCode {} - mapping to UNKNOWN", code);
                yield SupplierOrderStatus.UNKNOWN;
            }
        };
    }

    private SupplierOrderResult toResult(SupplierOrderEntity order) {
        return new SupplierOrderResult(
                order.getId(), order.getBuyerRef(), order.getPoNumber(),
                order.getStatus(), order.getCases(), order.getUnits());
    }
}
