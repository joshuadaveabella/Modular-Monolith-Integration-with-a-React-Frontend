package edu.cit.abella.supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// "If LegacySupply is unavailable, keep the reorder as PENDING in
// supplier_orders and have a scheduled job send it later." This is that
// job. It is restart-safe: buyerRef and requestId are stored in the row
// (derived from the row's own id), not held in memory, so a PENDING order
// left behind by a JVM restart is picked up exactly the same way as one
// left behind by a network failure a second ago.
@Component
class ReorderQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReorderQueueProcessor.class);

    private final SupplierOrderRepository orderRepository;
    private final SupplierSkuMappingRepository skuMappingRepository;
    private final SupplierOrderServiceImpl supplierOrderService;

    ReorderQueueProcessor(SupplierOrderRepository orderRepository,
                          SupplierSkuMappingRepository skuMappingRepository,
                          SupplierOrderServiceImpl supplierOrderService) {
        this.orderRepository = orderRepository;
        this.skuMappingRepository = skuMappingRepository;
        this.supplierOrderService = supplierOrderService;
    }

    // Tune this to stay within LegacySupply's request quota - each run
    // costs at most (1 buyerRef check + up to 3 submit attempts) calls
    // PER pending row.
    @Scheduled(fixedDelayString = "${legacysupply.reorder-queue-interval-ms:30000}")
    void processPendingReorders() {
        List<SupplierOrderEntity> pending = orderRepository.findAllByStatus(SupplierOrderStatus.PENDING);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Reorder queue: {} order(s) still PENDING, attempting resend", pending.size());

        for (SupplierOrderEntity order : pending) {
            skuMappingRepository.findByProductId(order.getProductId()).ifPresentOrElse(
                    mapping -> supplierOrderService.attemptSubmission(order, mapping.getSupplierSku()),
                    () -> log.error("Reorder {} has no SKU mapping for product {} - cannot send",
                            order.getBuyerRef(), order.getProductId())
            );
        }
    }
}
