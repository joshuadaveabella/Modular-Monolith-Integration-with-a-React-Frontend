package edu.cit.abella.supplier;

import edu.cit.abella.supplier.LegacySupplyHttpClient.PoOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
class SupplierOrderServiceImpl implements SupplierGateway {

    private static final Logger log = LoggerFactory.getLogger(SupplierOrderServiceImpl.class);

    // "Open" = still active, not yet delivered and not dead. UNKNOWN is
    // included deliberately: an unrecognized status code doesn't mean the
    // order stopped existing, just that we don't yet know what it means -
    // see OrderStatusPoller.
    private static final List<SupplierOrderStatus> OPEN_STATUSES = List.of(
            SupplierOrderStatus.PENDING,
            SupplierOrderStatus.SUBMITTED,
            SupplierOrderStatus.IN_PROGRESS,
            SupplierOrderStatus.SHIPPED,
            SupplierOrderStatus.UNKNOWN
    );

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

        int cases = (int) Math.ceil((double) unitsNeeded / mapping.getPackSize());
        int unitsOnOrder = cases * mapping.getPackSize();

        SupplierOrderEntity order = new SupplierOrderEntity(productId, cases, unitsOnOrder);
        orderRepository.save(order);

        try {
            attemptSubmission(order, mapping.getSupplierSku());
        } catch (Exception e) {
            log.warn("Immediate submission of reorder {} failed, left PENDING for retry: {}",
                    order.getBuyerRef(), e.getMessage());
        }

        return toResult(order);
    }

    @Override
    public boolean hasOpenOrder(String productId) {
        return !orderRepository.findAllByProductIdAndStatusIn(productId, OPEN_STATUSES).isEmpty();
    }
    @Override
    public int getOpenOrderUnits(String productId) {
        return orderRepository.findAllByProductIdAndStatusIn(productId, OPEN_STATUSES).stream()
                .mapToInt(SupplierOrderEntity::getUnits)
                .sum();
    }

    @Override
    public Optional<String> getSupplierSkuFor(String productId) {
        return skuMappingRepository.findByProductId(productId).map(SupplierSkuMappingEntity::getSupplierSku);
    }

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
                log.warn("Reorder {} still PENDING after retries: {} {}",
                        order.getBuyerRef(), e.kind, e.getMessage());
            } else {
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
            log.warn("Could not check for an existing order under {}: {}", buyerRef, e.getMessage());
            return Optional.empty();
        }
    }

    private void applyOutcome(SupplierOrderEntity order, PoOutcome outcome) {
        order.setPoNumber(outcome.poNumber());
        order.setStatus(mapStatusCode(outcome.statusCode()));
    }

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
